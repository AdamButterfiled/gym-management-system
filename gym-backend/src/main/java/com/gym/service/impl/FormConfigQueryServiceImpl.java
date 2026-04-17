package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.dto.FilterRuleRequest;
import com.gym.dto.FormPageConfig;
import com.gym.dto.RemoteOptionsRequest;
import com.gym.dto.TableQueryRequest;
import com.gym.entity.*;
import com.gym.mapper.*;
import com.gym.service.FormConfigPageService;
import com.gym.service.FormConfigQueryService;
import com.gym.service.GymV2Service;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

@Service
public class FormConfigQueryServiceImpl implements FormConfigQueryService {

    private final FormConfigPageService formConfigPageService;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final VenueMapper venueMapper;
    private final EquipmentMapper equipmentMapper;
    private final CoachMapper coachMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final PrivateScheduleMapper privateScheduleMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final ScheduleConflictMapper scheduleConflictMapper;
    private final SysDictMapper sysDictMapper;
    private final MenuMapper menuMapper;
    private final MemberMembershipMapper memberMembershipMapper;
    private final MemberPrivatePackageMapper memberPrivatePackageMapper;
    private final GymV2Service gymV2Service;

    public FormConfigQueryServiceImpl(FormConfigPageService formConfigPageService,
                                      ObjectMapper objectMapper,
                                      UserMapper userMapper,
                                      VenueMapper venueMapper,
                                      EquipmentMapper equipmentMapper,
                                      CoachMapper coachMapper,
                                      CourseScheduleMapper courseScheduleMapper,
                                      PrivateScheduleMapper privateScheduleMapper,
                                      BookingOrderMapper bookingOrderMapper,
                                      PaymentOrderMapper paymentOrderMapper,
                                      CheckinRecordMapper checkinRecordMapper,
                                      ScheduleConflictMapper scheduleConflictMapper,
                                      SysDictMapper sysDictMapper,
                                      MenuMapper menuMapper,
                                      MemberMembershipMapper memberMembershipMapper,
                                      MemberPrivatePackageMapper memberPrivatePackageMapper,
                                      GymV2Service gymV2Service) {
        this.formConfigPageService = formConfigPageService;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.venueMapper = venueMapper;
        this.equipmentMapper = equipmentMapper;
        this.coachMapper = coachMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.privateScheduleMapper = privateScheduleMapper;
        this.bookingOrderMapper = bookingOrderMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.checkinRecordMapper = checkinRecordMapper;
        this.scheduleConflictMapper = scheduleConflictMapper;
        this.sysDictMapper = sysDictMapper;
        this.menuMapper = menuMapper;
        this.memberMembershipMapper = memberMembershipMapper;
        this.memberPrivatePackageMapper = memberPrivatePackageMapper;
        this.gymV2Service = gymV2Service;
    }

    @Override
    public Page<?> queryPage(TableQueryRequest request) {
        FormPageConfig config = resolveConfig(request.getPageKey(), request.getRoutePath());
        return queryPageInternal(request, config);
    }

    @Override
    public List<Map<String, Object>> loadRemoteApiOptions(RemoteOptionsRequest request) {
        String endpoint = stringValue(request.getOptionSourceConfig().get("endpoint"));
        if (endpoint == null) {
            return List.of();
        }

        String pageKey = endpointToPageKey(endpoint);
        if (pageKey == null) {
            return List.of();
        }

        Map<String, Object> optionSourceConfig = new LinkedHashMap<>(request.getOptionSourceConfig());
        optionSourceConfig.put("sourcePageKey", pageKey);

        RemoteOptionsRequest delegate = new RemoteOptionsRequest();
        delegate.setKeyword(request.getKeyword());
        delegate.setLimit(request.getLimit());
        delegate.setPageKey(pageKey);
        delegate.setOptionSourceType("remote-form");
        delegate.setOptionSourceConfig(optionSourceConfig);
        return loadRemoteFormOptions(delegate);
    }

    @Override
    public List<Map<String, Object>> loadRemoteFormOptions(RemoteOptionsRequest request) {
        Map<String, Object> config = request.getOptionSourceConfig() == null
            ? Map.of()
            : request.getOptionSourceConfig();
        String sourcePageKey = firstNonBlank(
            stringValue(config.get("sourcePageKey")),
            request.getPageKey()
        );
        if (sourcePageKey == null) {
            return List.of();
        }

        FormPageConfig sourceConfig = formConfigPageService.getConfigByPageKey(sourcePageKey);
        if (sourceConfig == null) {
            return List.of();
        }

        FormPageConfig runtimeConfig = objectMapper.convertValue(sourceConfig, FormPageConfig.class);
        List<String> searchFields = toStringList(config.get("searchFields"));
        if (!searchFields.isEmpty()) {
            runtimeConfig.setQuickSearchFields(searchFields);
        }

        TableQueryRequest queryRequest = new TableQueryRequest();
        queryRequest.setPageKey(sourcePageKey);
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(request.getLimit() == null ? 20 : request.getLimit());
        queryRequest.setKeyword(request.getKeyword());

        Page<?> page = queryPageInternal(queryRequest, runtimeConfig);
        String labelField = firstNonBlank(
            stringValue(config.get("labelField")),
            guessLabelField(runtimeConfig)
        );
        String valueField = firstNonBlank(
            stringValue(config.get("valueField")),
            guessValueField(runtimeConfig)
        );

        return page.getRecords().stream()
            .map(record -> toOptionItem(record, labelField, valueField))
            .filter(item -> item.get("label") != null && item.get("value") != null)
            .toList();
    }

    private Page<?> queryPageInternal(TableQueryRequest request, FormPageConfig config) {
        return switch (config.getPageKey()) {
            case "user" -> queryUsers(request, config);
            case "venue" -> queryVenues(request, config);
            case "equipment" -> queryEquipments(request, config);
            case "coach" -> queryCoaches(request, config);
            case "course" -> queryCourseSchedules(request, config);
            case "private-schedule" -> queryPrivateSchedules(request, config);
            case "reservation" -> queryBookings(request, config);
            case "payment-order" -> queryPayments(request, config);
            case "member-assets" -> queryMemberAssets(request, config);
            case "checkin-record" -> queryCheckins(request, config);
            case "schedule-conflict" -> queryConflicts(request, config);
            case "dict" -> queryDicts(request, config);
            case "menu" -> queryMenus(request, config);
            default -> new Page<>(safePageNum(request), safePageSize(request));
        };
    }

    private Page<User> queryUsers(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            userMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "username", "username",
                "realName", "real_name",
                "nickname", "nickname",
                "email", "email",
                "role", "role",
                "phone", "phone",
                "balance", "balance",
                "type", "type",
                "status", "status",
                "createdAt", "created_at"
            ),
            wrapper -> wrapper.orderByDesc("id")
        );
    }

    private Page<Venue> queryVenues(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            venueMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "name", "name",
                "location", "location",
                "capacity", "capacity",
                "pricePerHour", "price_per_hour",
                "layoutJson", "layout_json",
                "status", "status",
                "openTime", "open_time",
                "closeTime", "close_time",
                "description", "description"
            ),
            wrapper -> wrapper.orderByDesc("id")
        );
    }

    private Page<Equipment> queryEquipments(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            equipmentMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "name", "name",
                "description", "description",
                "status", "status",
                "venueId", "venue_id",
                "quantity", "quantity"
            ),
            wrapper -> wrapper.orderByDesc("id")
        );
    }

    private Page<Coach> queryCoaches(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            coachMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "name", "name",
                "phone", "phone",
                "specialization", "specialization",
                "hourlyPrice", "hourly_price",
                "rating", "rating",
                "status", "status",
                "gender", "gender",
                "age", "age",
                "entryDate", "entry_date",
                "intro", "intro"
            ),
            wrapper -> wrapper.orderByDesc("id")
        );
    }

    private Page<CourseSchedule> queryCourseSchedules(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            courseScheduleMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "name", "name",
                "coachId", "coach_id",
                "venueId", "venue_id",
                "period", "start_time",
                "startTime", "start_time",
                "endTime", "end_time",
                "capacity", "capacity",
                "bookedCount", "booked_count",
                "normalPrice", "normal_price",
                "price", "normal_price",
                "flashSale", "flash_sale",
                "flashSalePrice", "flash_sale_price",
                "status", "status",
                "createdAt", "created_at"
            ),
            wrapper -> wrapper.orderByDesc("start_time")
        );
    }

    private Page<PrivateSchedule> queryPrivateSchedules(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            privateScheduleMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "coachId", "coach_id",
                "venueId", "venue_id",
                "startTime", "start_time",
                "endTime", "end_time",
                "capacity", "capacity",
                "bookedCount", "booked_count",
                "status", "status",
                "description", "description",
                "createdAt", "created_at"
            ),
            wrapper -> wrapper.orderByDesc("start_time")
        );
    }

    private Page<BookingOrder> queryBookings(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            bookingOrderMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "orderNo", "order_no",
                "userId", "user_id",
                "resourceType", "resource_type",
                "resourceName", "resource_name",
                "venueId", "venue_id",
                "coachId", "coach_id",
                "bookingDate", "booking_date",
                "period", "start_time",
                "startTime", "start_time",
                "endTime", "end_time",
                "amount", "amount",
                "paymentStatus", "payment_status",
                "status", "status",
                "remark", "remark",
                "createdAt", "created_at"
            ),
            wrapper -> wrapper.orderByDesc("created_at")
        );
    }

    private Page<PaymentOrder> queryPayments(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            paymentOrderMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "paymentNo", "payment_no",
                "userId", "user_id",
                "paymentType", "payment_type",
                "targetType", "target_type",
                "targetId", "target_id",
                "amount", "amount",
                "status", "status",
                "paidAt", "paid_at",
                "createdAt", "created_at"
            ),
            wrapper -> wrapper.orderByDesc("created_at")
        );
    }

    private Page<CheckinRecord> queryCheckins(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            checkinRecordMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "bookingOrderId", "booking_order_id",
                "userId", "user_id",
                "checkinCode", "checkin_code",
                "checkinTime", "checkin_time",
                "status", "status",
                "operatorName", "operator_name"
            ),
            wrapper -> wrapper.orderByDesc("checkin_time")
        );
    }

    private Page<ScheduleConflict> queryConflicts(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            scheduleConflictMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "resourceType", "resource_type",
                "resourceId", "resource_id",
                "conflictType", "conflict_type",
                "referenceId", "reference_id",
                "message", "message",
                "startTime", "start_time",
                "endTime", "end_time",
                "createdAt", "created_at"
            ),
            wrapper -> wrapper.orderByDesc("created_at")
        );
    }

    private Page<SysDict> queryDicts(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            sysDictMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "dictName", "dict_name",
                "dictType", "dict_type",
                "dictLabel", "dict_label",
                "dictValue", "dict_value",
                "sort", "sort",
                "status", "status",
                "remark", "remark"
            ),
            wrapper -> wrapper.orderByAsc("dict_type").orderByAsc("sort")
        );
    }

    private Page<Menu> queryMenus(TableQueryRequest request, FormPageConfig config) {
        return querySimplePage(
            menuMapper,
            request,
            config,
            mapOf(
                "id", "id",
                "title", "title",
                "name", "name",
                "path", "path",
                "component", "component",
                "icon", "icon",
                "parentId", "parent_id",
                "sort", "sort",
                "roles", "roles",
                "hidden", "hidden",
                "componentStyle", "component_style"
            ),
            wrapper -> wrapper.orderByAsc("sort")
        );
    }

    private Page<Map<String, Object>> queryMemberAssets(TableQueryRequest request, FormPageConfig config) {
        List<User> members = userMapper.selectList(new LambdaQueryWrapper<User>()
            .eq(User::getRole, "MEMBER")
            .orderByDesc(User::getId));

        List<Map<String, Object>> items = members.stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("realName", user.getRealName());
            item.put("balance", user.getBalance());

            MemberMembership membership = memberMembershipMapper.selectOne(new LambdaQueryWrapper<MemberMembership>()
                .eq(MemberMembership::getUserId, user.getId())
                .orderByDesc(MemberMembership::getEndDate)
                .last("limit 1"));
            MemberPrivatePackage privatePackage = memberPrivatePackageMapper.selectOne(new LambdaQueryWrapper<MemberPrivatePackage>()
                .eq(MemberPrivatePackage::getUserId, user.getId())
                .orderByDesc(MemberPrivatePackage::getCreatedAt)
                .last("limit 1"));

            item.put("membership", membership == null ? null : membership.getMembershipName());
            item.put("membershipEndDate", membership == null ? null : membership.getEndDate());
            item.put("remainingPrivateSessions", privatePackage == null ? 0 : privatePackage.getRemainingSessions());
            return item;
        }).toList();

        List<Map<String, Object>> filtered = items.stream()
            .filter(item -> matchKeyword(item, request.getKeyword(), effectiveQuickSearchFields(config, List.of("username", "realName"))))
            .filter(item -> matchRules(item, request.getFilterLogic(), request.getFilterRules()))
            .toList();

        return sliceMapPage(filtered, request);
    }

    private <T> Page<T> querySimplePage(BaseMapper<T> mapper,
                                        TableQueryRequest request,
                                        FormPageConfig config,
                                        Map<String, String> fieldColumnMap,
                                        Consumer<QueryWrapper<T>> orderConsumer) {
        Page<T> page = new Page<>(safePageNum(request), safePageSize(request));
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        applyKeyword(wrapper, request, config, fieldColumnMap);
        applyRules(wrapper, request, fieldColumnMap);
        if (orderConsumer != null) {
            orderConsumer.accept(wrapper);
        }
        return mapper.selectPage(page, wrapper);
    }

    private <T> void applyKeyword(QueryWrapper<T> wrapper,
                                  TableQueryRequest request,
                                  FormPageConfig config,
                                  Map<String, String> fieldColumnMap) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            return;
        }
        List<String> quickFields = effectiveQuickSearchFields(config, List.of());
        if (quickFields.isEmpty()) {
            return;
        }
        wrapper.and(group -> {
            boolean first = true;
            for (String fieldKey : quickFields) {
                String column = fieldColumnMap.get(fieldKey);
                if (column == null) {
                    continue;
                }
                if (!first) {
                    group.or();
                }
                group.like(column, request.getKeyword().trim());
                first = false;
            }
        });
    }

    private <T> void applyRules(QueryWrapper<T> wrapper,
                                TableQueryRequest request,
                                Map<String, String> fieldColumnMap) {
        if (request.getFilterRules() == null || request.getFilterRules().isEmpty()) {
            return;
        }
        if ("OR".equalsIgnoreCase(request.getFilterLogic())) {
            wrapper.and(group -> {
                boolean first = true;
                for (FilterRuleRequest rule : request.getFilterRules()) {
                    if (isRuleEmpty(rule)) {
                        continue;
                    }
                    if (!first) {
                        group.or();
                    }
                    group.and(single -> appendRule(single, rule, fieldColumnMap));
                    first = false;
                }
            });
            return;
        }
        request.getFilterRules().forEach(rule -> appendRule(wrapper, rule, fieldColumnMap));
    }

    private <T> void appendRule(QueryWrapper<T> wrapper,
                                FilterRuleRequest rule,
                                Map<String, String> fieldColumnMap) {
        if (isRuleEmpty(rule)) {
            return;
        }
        String queryKey = firstNonBlank(rule.getQueryKey(), rule.getFieldKey());
        String column = fieldColumnMap.get(queryKey);
        if (column == null) {
            return;
        }

        String operator = firstNonBlank(rule.getOperator(), defaultOperator(rule.getControlType(), rule.getMatchMode()));
        String controlType = rule.getControlType();
        Object value = rule.getValue();
        Object valueTo = rule.getValueTo();

        switch (operator) {
            case "contains" -> wrapper.like(column, stringValue(value));
            case "equals" -> wrapper.eq(column, normalizeScalar(value));
            case "gt" -> wrapper.gt(column, normalizeScalar(value));
            case "lt" -> wrapper.lt(column, normalizeScalar(value));
            case "in" -> {
                List<Object> values = normalizeList(value);
                if (!values.isEmpty()) {
                    wrapper.in(column, values);
                }
            }
            case "on" -> appendDateOn(wrapper, column, value);
            case "between" -> appendBetween(wrapper, column, controlType, value, valueTo);
            default -> wrapper.like(column, stringValue(value));
        }
    }

    private <T> void appendDateOn(QueryWrapper<T> wrapper, String column, Object value) {
        String date = stringValue(value);
        if (date == null || date.isBlank()) {
            return;
        }
        wrapper.apply("DATE(" + column + ") = {0}", date);
    }

    private <T> void appendBetween(QueryWrapper<T> wrapper,
                                   String column,
                                   String controlType,
                                   Object value,
                                   Object valueTo) {
        if ("date-range".equals(controlType)) {
            String start = stringValue(value);
            String end = stringValue(valueTo);
            if (start != null && !start.isBlank()) {
                wrapper.apply("DATE(" + column + ") >= {0}", start);
            }
            if (end != null && !end.isBlank()) {
                wrapper.apply("DATE(" + column + ") <= {0}", end);
            }
            return;
        }

        Object startValue = normalizeScalar(value);
        Object endValue = normalizeScalar(valueTo);
        if (startValue != null && endValue != null) {
            wrapper.between(column, startValue, endValue);
        } else if (startValue != null) {
            wrapper.ge(column, startValue);
        } else if (endValue != null) {
            wrapper.le(column, endValue);
        }
    }

    private boolean isRuleEmpty(FilterRuleRequest rule) {
        if (rule == null) {
            return true;
        }
        if (rule.getValue() == null && rule.getValueTo() == null) {
            return true;
        }
        if (rule.getValue() instanceof String value && value.isBlank() && rule.getValueTo() == null) {
            return true;
        }
        if (rule.getValue() instanceof Collection<?> values && values.isEmpty() && rule.getValueTo() == null) {
            return true;
        }
        return false;
    }

    private List<String> effectiveQuickSearchFields(FormPageConfig config, List<String> fallback) {
        if (config.getQuickSearchFields() != null && !config.getQuickSearchFields().isEmpty()) {
            return config.getQuickSearchFields();
        }
        return fallback;
    }

    private boolean matchKeyword(Map<String, Object> item, String keyword, List<String> fields) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String search = keyword.trim().toLowerCase(Locale.ROOT);
        return fields.stream()
            .map(item::get)
            .filter(Objects::nonNull)
            .map(value -> String.valueOf(value).toLowerCase(Locale.ROOT))
            .anyMatch(value -> value.contains(search));
    }

    private boolean matchRules(Map<String, Object> item, String filterLogic, List<FilterRuleRequest> rules) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        List<Boolean> results = rules.stream()
            .filter(rule -> !isRuleEmpty(rule))
            .map(rule -> matchRule(item, rule))
            .toList();
        if (results.isEmpty()) {
            return true;
        }
        if ("OR".equalsIgnoreCase(filterLogic)) {
            return results.stream().anyMatch(Boolean::booleanValue);
        }
        return results.stream().allMatch(Boolean::booleanValue);
    }

    private boolean matchRule(Map<String, Object> item, FilterRuleRequest rule) {
        Object raw = item.get(firstNonBlank(rule.getQueryKey(), rule.getFieldKey()));
        if (raw == null) {
            return false;
        }
        String operator = firstNonBlank(rule.getOperator(), defaultOperator(rule.getControlType(), rule.getMatchMode()));
        return switch (operator) {
            case "contains" -> String.valueOf(raw).toLowerCase(Locale.ROOT)
                .contains(stringValue(rule.getValue()).toLowerCase(Locale.ROOT));
            case "equals" -> Objects.equals(String.valueOf(raw), String.valueOf(normalizeScalar(rule.getValue())));
            case "gt" -> compareNumbers(raw, rule.getValue()) > 0;
            case "lt" -> compareNumbers(raw, rule.getValue()) < 0;
            case "in" -> normalizeList(rule.getValue()).stream()
                .map(String::valueOf)
                .anyMatch(value -> Objects.equals(String.valueOf(raw), value));
            case "on" -> Objects.equals(String.valueOf(raw), String.valueOf(rule.getValue()));
            case "between" -> betweenValues(raw, rule.getValue(), rule.getValueTo());
            default -> false;
        };
    }

    private Page<Map<String, Object>> sliceMapPage(List<Map<String, Object>> records, TableQueryRequest request) {
        int pageNum = safePageNum(request);
        int pageSize = safePageSize(request);
        int start = Math.max(0, (pageNum - 1) * pageSize);
        int end = Math.min(records.size(), start + pageSize);
        List<Map<String, Object>> slice = start >= records.size() ? List.of() : records.subList(start, end);
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, records.size());
        page.setRecords(slice);
        return page;
    }

    private int compareNumbers(Object left, Object right) {
        BigDecimal leftValue = new BigDecimal(String.valueOf(left));
        BigDecimal rightValue = new BigDecimal(String.valueOf(normalizeScalar(right)));
        return leftValue.compareTo(rightValue);
    }

    private boolean betweenValues(Object raw, Object left, Object right) {
        BigDecimal target = new BigDecimal(String.valueOf(raw));
        if (left != null && !String.valueOf(left).isBlank()) {
            if (target.compareTo(new BigDecimal(String.valueOf(normalizeScalar(left)))) < 0) {
                return false;
            }
        }
        if (right != null && !String.valueOf(right).isBlank()) {
            if (target.compareTo(new BigDecimal(String.valueOf(normalizeScalar(right)))) > 0) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> toOptionItem(Object record, String labelField, String valueField) {
        Map<String, Object> raw = objectMapper.convertValue(record, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", raw.get(labelField));
        item.put("value", raw.get(valueField));
        item.put("raw", raw);
        return item;
    }

    private String guessLabelField(FormPageConfig config) {
        return config.getFields().stream()
            .map(field -> field.getFieldKey())
            .filter(field -> List.of("name", "title", "label", "username", "nickname", "realName").contains(field))
            .findFirst()
            .orElse(config.getFields().isEmpty() ? "label" : config.getFields().get(0).getFieldKey());
    }

    private String guessValueField(FormPageConfig config) {
        return config.getFields().stream()
            .map(field -> field.getFieldKey())
            .filter(field -> List.of("id", "code", "value", "userId").contains(field))
            .findFirst()
            .orElse(config.getFields().isEmpty() ? "value" : config.getFields().get(0).getFieldKey());
    }

    private FormPageConfig resolveConfig(String pageKey, String routePath) {
        FormPageConfig config = null;
        if (pageKey != null && !pageKey.isBlank()) {
            config = formConfigPageService.getConfigByPageKey(pageKey);
        }
        if (config == null && routePath != null && !routePath.isBlank()) {
            config = formConfigPageService.getConfigByRoutePath(routePath);
        }
        if (config == null) {
            throw new IllegalArgumentException("未找到表单配置: " + firstNonBlank(pageKey, routePath));
        }
        return config;
    }

    private String endpointToPageKey(String endpoint) {
        return switch (endpoint) {
            case "/user/page" -> "user";
            case "/admin/venues/page" -> "venue";
            case "/equipment/page" -> "equipment";
            case "/admin/coaches/page" -> "coach";
            case "/admin/course-schedules/page" -> "course";
            case "/admin/private-schedules/page" -> "private-schedule";
            case "/admin/bookings/page" -> "reservation";
            case "/admin/payments/page" -> "payment-order";
            case "/admin/member-assets/page" -> "member-assets";
            case "/admin/checkins/page" -> "checkin-record";
            case "/admin/conflicts/page" -> "schedule-conflict";
            case "/dict/list" -> "dict";
            case "/menu/list" -> "menu";
            default -> null;
        };
    }

    private int safePageNum(TableQueryRequest request) {
        return request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
    }

    private int safePageSize(TableQueryRequest request) {
        return request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 200);
    }

    private Map<String, String> mapOf(String... items) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < items.length; i += 2) {
            map.put(items[i], items[i + 1]);
        }
        return map;
    }

    private String defaultOperator(String controlType, String matchMode) {
        if ("exact".equalsIgnoreCase(matchMode)) {
            return "equals";
        }
        if ("text".equals(controlType) || "contains".equals(matchMode)) {
            return "contains";
        }
        if ("date-range".equals(controlType) || "number-range".equals(controlType)) {
            return "between";
        }
        if ("date".equals(controlType)) {
            return "on";
        }
        return "equals";
    }

    private Object normalizeScalar(Object raw) {
        if (raw instanceof List<?> list) {
            return list.isEmpty() ? null : list.get(0);
        }
        return raw;
    }

    private List<Object> normalizeList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(value -> (Object) value)
                    .toList();
        }
        if (raw.getClass().isArray()) {
            int length = Array.getLength(raw);
            List<Object> values = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                Object value = Array.get(raw, index);
                if (value != null) {
                    values.add(value);
                }
            }
            return values;
        }
        return List.of(raw);
    }

    private List<String> toStringList(Object raw) {
        return normalizeList(raw).stream().map(String::valueOf).toList();
    }

    private String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw);
        return value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
