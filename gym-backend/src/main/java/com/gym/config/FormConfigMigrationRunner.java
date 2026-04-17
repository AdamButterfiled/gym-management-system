package com.gym.config;

import com.gym.dto.FormFieldConfig;
import com.gym.dto.FormPageConfig;
import com.gym.service.FormConfigPageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FormConfigMigrationRunner implements CommandLineRunner {

    private final FormConfigPageService formConfigPageService;

    public FormConfigMigrationRunner(FormConfigPageService formConfigPageService) {
        this.formConfigPageService = formConfigPageService;
    }

    @Override
    public void run(String... args) {
        defaultConfigs().forEach(this::upsertDefaultConfig);
    }

    private void upsertDefaultConfig(FormPageConfig defaults) {
        FormPageConfig existing = formConfigPageService.getConfigByPageKey(defaults.getPageKey());
        if (existing == null) {
            formConfigPageService.savePageConfig(defaults);
            return;
        }

        boolean changed = false;
        if (existing.getFormInputFollowSystemRadius() == null) {
            existing.setFormInputFollowSystemRadius(Boolean.FALSE);
            changed = true;
        }
        if (existing.getFields() == null) {
            existing.setFields(new ArrayList<>());
            changed = true;
        }

        Map<String, FormFieldConfig> existingFieldMap = new LinkedHashMap<>();
        existing.getFields().forEach(field -> existingFieldMap.put(field.getFieldKey(), field));

        for (FormFieldConfig defaultField : defaults.getFields()) {
            if (!existingFieldMap.containsKey(defaultField.getFieldKey())) {
                existing.getFields().add(defaultField);
                changed = true;
            }
        }

        if (changed) {
            formConfigPageService.savePageConfig(existing);
        }
    }

    private List<FormPageConfig> defaultConfigs() {
        return List.of(
            userConfig(),
            venueConfig(),
            equipmentConfig(),
            coachConfig(),
            courseConfig(),
            privateScheduleConfig(),
            reservationConfig(),
            paymentConfig(),
            memberAssetConfig(),
            checkinConfig(),
            conflictConfig(),
            analyticsConfig(),
            repairConfig(),
            dictConfig(),
            menuConfig()
        );
    }

    private FormPageConfig userConfig() {
        return page(
            "user",
            "/user",
            "用户信息管理",
            "搜索用户名、昵称或邮箱",
            List.of("username", "nickname", "email"),
            field("id", "ID", "id", "number", false, false),
            field("username", "用户名", "username", "text", true, true),
            field("realName", "真实姓名", "realName", "text", false, false),
            field("nickname", "昵称", "nickname", "text", true, true),
            field("email", "邮箱", "email", "text", false, true),
            selectField("role", "角色", "role", staticSource(
                option("管理员", "ADMIN"),
                option("员工", "STAFF"),
                option("会员", "MEMBER"),
                option("教练", "COACH")
            )),
            field("phone", "手机号", "phone", "text", true, true),
            field("balance", "余额", "balance", "number", false, true),
            field("status", "状态", "status", "select", false, true),
            field("createdAt", "创建时间", "createdAt", "date-range", false, true)
        );
    }

    private FormPageConfig venueConfig() {
        return page(
            "venue",
            "/venue",
            "场馆资源管理",
            "搜索场馆名称或位置",
            List.of("name", "location"),
            field("id", "ID", "id", "number", false, false),
            field("name", "场馆名称", "name", "text", true, true),
            field("location", "位置", "location", "text", true, true),
            field("capacity", "容量", "capacity", "number-range", false, true),
            field("pricePerHour", "价格", "pricePerHour", "number-range", false, true),
            field("layoutJson", "布局", "layoutJson", "text", false, false),
            selectField("status", "状态", "status", staticSource(
                option("开放", 1),
                option("关闭", 0)
            )), 
            field("openTime", "开放时间", "openTime", "text", false, false),
            field("closeTime", "关闭时间", "closeTime", "text", false, false),
            field("description", "场馆说明", "description", "text", false, false)
        );
    }

    private FormPageConfig equipmentConfig() {
        return page(
            "equipment",
            "/equipment",
            "器材信息管理",
            "搜索器材名称或描述",
            List.of("name", "description"),
            field("id", "ID", "id", "number", false, false),
            field("name", "器材名称", "name", "text", true, true),
            field("description", "描述", "description", "text", false, true),
            field("quantity", "数量", "quantity", "number-range", false, true),
            selectField("status", "状态", "status", staticSource(
                option("可用", "AVAILABLE"),
                option("使用中", "IN_USE"),
                option("维护中", "MAINTENANCE")
            )),
            remoteFormField("venueId", "所属场馆", "venueId", "venue", "name", "id", List.of("name", "location"))
        );
    }

    private FormPageConfig coachConfig() {
        return page(
            "coach",
            "/coach",
            "教练信息管理",
            "搜索教练姓名或电话",
            List.of("name", "phone"),
            field("id", "ID", "id", "number", false, false),
            field("name", "姓名", "name", "text", true, true),
            field("phone", "电话", "phone", "text", true, true),
            field("specialization", "专长", "specialization", "text", false, true),
            field("hourlyPrice", "时薪", "hourlyPrice", "number-range", false, true),
            field("rating", "评分", "rating", "number-range", false, true),
            selectField("status", "状态", "status", staticSource(
                option("在职", 1),
                option("离职", 0)
            )),
            selectField("gender", "性别", "gender", dictSource("gender")),
            field("entryDate", "入职日期", "entryDate", "date-range", false, true),
            field("age", "年龄", "age", "number", false, false),
            field("intro", "教练简介", "intro", "text", false, false)
        );
    }

    private FormPageConfig courseConfig() {
        return page(
            "course",
            "/course",
            "团课排期管理",
            "搜索团课名称",
            List.of("name"),
            field("name", "课程名称", "name", "text", true, true),
            remoteFormField("coachId", "教练", "coachId", "coach", "name", "id", List.of("name", "phone", "specialization")),
            remoteFormField("venueId", "场馆", "venueId", "venue", "name", "id", List.of("name", "location")),
            field("period", "时间", "startTime", "date-range", false, false),
            field("bookedCount", "人数", "bookedCount", "number-range", false, true),
            field("price", "价格", "normalPrice", "number-range", false, true),
            selectField("status", "状态", "status", staticSource(
                option("已发布", "PUBLISHED"),
                option("已关闭", "CLOSED")
            )),
            field("startTime", "开始时间", "startTime", "date", false, false),
            field("endTime", "结束时间", "endTime", "date", false, false),
            field("capacity", "容量", "capacity", "number", false, false),
            field("normalPrice", "常规价格", "normalPrice", "number", false, false),
            field("flashSale", "秒杀开关", "flashSale", "select", false, false),
            field("flashSalePrice", "秒杀价格", "flashSalePrice", "number", false, false),
            field("description", "课程说明", "description", "text", false, false)
        );
    }

    private FormPageConfig privateScheduleConfig() {
        return page(
            "private-schedule",
            "/private-schedule",
            "私教排班管理",
            "搜索教练或场馆",
            List.of(),
            remoteFormField("coachId", "教练", "coachId", "coach", "name", "id", List.of("name", "phone", "specialization")),
            remoteFormField("venueId", "场馆", "venueId", "venue", "name", "id", List.of("name", "location")),
            field("period", "时段", "startTime", "date-range", false, false),
            field("bookedCount", "预约情况", "bookedCount", "number-range", false, true),
            selectField("status", "状态", "status", staticSource(
                option("开放预约", "OPEN"),
                option("已关闭", "CLOSED")
            )),
            field("startTime", "开始时间", "startTime", "date", false, false),
            field("endTime", "结束时间", "endTime", "date", false, false),
            field("capacity", "可约人数", "capacity", "number", false, false),
            field("description", "备注", "description", "text", false, true)
        );
    }

    private FormPageConfig reservationConfig() {
        return page(
            "reservation",
            "/reservation",
            "预约订单管理",
            "搜索资源名或备注",
            List.of("resourceName", "remark"),
            field("orderNo", "订单号", "orderNo", "text", false, true),
            remoteFormField("userId", "用户", "userId", "user", "username", "id", List.of("username", "nickname", "realName")),
            selectField("resourceType", "资源类型", "resourceType", staticSource(
                option("场馆", "VENUE"),
                option("团课", "GROUP_COURSE"),
                option("私教", "PRIVATE_COACH")
            )),
            field("resourceName", "资源名称", "resourceName", "text", true, true),
            field("period", "预约时间", "startTime", "date-range", false, false),
            field("amount", "金额", "amount", "number-range", false, true),
            selectField("paymentStatus", "支付状态", "paymentStatus", dictSource("payment_status")),
            selectField("status", "预约状态", "status", dictSource("booking_status")),
            field("remark", "备注", "remark", "text", false, true)
        );
    }

    private FormPageConfig paymentConfig() {
        return page(
            "payment-order",
            "/payment-orders",
            "支付订单管理",
            "搜索支付单号",
            List.of("paymentNo"),
            field("paymentNo", "支付单号", "paymentNo", "text", true, true),
            remoteFormField("userId", "用户", "userId", "user", "username", "id", List.of("username", "nickname", "realName")),
            selectField("paymentType", "支付类型", "paymentType", staticSource(
                option("充值", "RECHARGE"),
                option("会籍", "MEMBERSHIP"),
                option("私教课包", "PRIVATE_PACKAGE"),
                option("预约", "BOOKING")
            )),
            field("targetType", "目标类型", "targetType", "text", false, true),
            field("targetId", "目标ID", "targetId", "number", false, true),
            field("amount", "金额", "amount", "number-range", false, true),
            selectField("status", "状态", "status", dictSource("payment_status")),
            field("paidAt", "支付时间", "paidAt", "date-range", false, true)
        );
    }

    private FormPageConfig memberAssetConfig() {
        return page(
            "member-assets",
            "/member-assets",
            "会员资产中心",
            "搜索用户名或真实姓名",
            List.of("username", "realName"),
            remoteFormField("userId", "会员", "userId", "user", "username", "id", List.of("username", "nickname", "realName")),
            field("username", "用户名", "username", "text", true, true),
            field("realName", "姓名", "realName", "text", true, true),
            field("balance", "钱包余额", "balance", "number-range", false, true),
            field("membership", "当前会籍", "membership", "text", false, true),
            field("membershipEndDate", "会籍到期", "membershipEndDate", "date-range", false, true),
            field("remainingPrivateSessions", "剩余私教课时", "remainingPrivateSessions", "number-range", false, true),
            field("membershipPackageName", "会籍套餐名称", "name", "text", false, false),
            field("membershipPackagePrice", "会籍套餐价格", "price", "number", false, false),
            field("membershipPackageDays", "会籍套餐时长", "days", "number", false, false),
            field("membershipPackageDescription", "会籍套餐说明", "description", "text", false, false),
            remoteFormField("privatePackageCoachId", "课包绑定教练", "coachId", "coach", "name", "id", List.of("name", "phone", "specialization")),
            field("privatePackageName", "私教课包名称", "name", "text", false, false),
            field("privatePackagePrice", "私教课包价格", "price", "number", false, false),
            field("privatePackageTotalSessions", "私教课包总课时", "totalSessions", "number", false, false),
            field("privatePackageDescription", "私教课包说明", "description", "text", false, false)
        );
    }

    private FormPageConfig checkinConfig() {
        return page(
            "checkin-record",
            "/checkin-records",
            "签到核销记录",
            "搜索签到码",
            List.of("checkinCode"),
            field("id", "记录ID", "id", "number", false, false),
            field("bookingOrderId", "预约单ID", "bookingOrderId", "number", false, true),
            remoteFormField("userId", "用户", "userId", "user", "username", "id", List.of("username", "nickname", "realName")),
            field("checkinCode", "签到码", "checkinCode", "text", true, true),
            field("checkinTime", "签到时间", "checkinTime", "date-range", false, true),
            selectField("status", "状态", "status", staticSource(
                option("成功", "SUCCESS"),
                option("失败", "FAILED")
            )),
            field("operatorName", "核销人", "operatorName", "text", false, true)
        );
    }

    private FormPageConfig conflictConfig() {
        return page(
            "schedule-conflict",
            "/schedule-conflicts",
            "排期冲突日志",
            "搜索冲突消息",
            List.of("message"),
            field("id", "ID", "id", "number", false, false),
            field("resourceType", "资源类型", "resourceType", "text", false, true),
            field("conflictType", "冲突类型", "conflictType", "text", false, true),
            field("message", "消息", "message", "text", true, true),
            field("startTime", "开始时间", "startTime", "date-range", false, true),
            field("endTime", "结束时间", "endTime", "date-range", false, true),
            field("createdAt", "记录时间", "createdAt", "date-range", false, true)
        );
    }

    private FormPageConfig analyticsConfig() {
        return page(
            "analytics",
            "/analytics",
            "经营分析中心",
            "",
            List.of(),
            field("coachName", "教练", "coachName", "text", false, false),
            field("privateLessons", "私教预约数", "privateLessons", "number-range", false, false),
            field("groupLessons", "团课排期数", "groupLessons", "number-range", false, false)
        );
    }

    private FormPageConfig repairConfig() {
        return page(
            "repair",
            "/repair",
            "报修工单管理",
            "",
            List.of(),
            field("id", "序号", "id", "number", false, false),
            field("description", "报修描述", "description", "text", false, false),
            field("status", "状态", "status", "select", false, false),
            field("createdAt", "报修时间", "createdAt", "date-range", false, false)
        );
    }

    private FormPageConfig dictConfig() {
        return page(
            "dict",
            "/sys/dict",
            "数据字典管理",
            "搜索字典标签或类型",
            List.of("dictLabel", "dictType"),
            field("id", "ID", "id", "number", false, false),
            field("dictName", "字典名称", "dictName", "text", false, false),
            field("dictType", "字典类型", "dictType", "text", true, true),
            field("dictLabel", "字典标签", "dictLabel", "text", true, true),
            field("dictValue", "字典值", "dictValue", "text", false, true),
            field("sort", "排序", "sort", "number-range", false, true),
            field("status", "状态", "status", "select", false, false),
            field("remark", "备注", "remark", "text", false, false)
        );
    }

    private FormPageConfig menuConfig() {
        return page(
            "menu",
            "/sys/menu",
            "菜单权限管理",
            "搜索菜单标题",
            List.of("title"),
            field("id", "ID", "id", "number", false, false),
            field("title", "菜单标题", "title", "text", true, true),
            field("name", "路由名称", "name", "text", false, true),
            field("path", "路由路径", "path", "text", false, true),
            field("component", "组件路径", "component", "text", false, true),
            field("icon", "图标", "icon", "text", false, true),
            field("parentId", "父菜单", "parentId", "number", false, true),
            field("sort", "排序", "sort", "number-range", false, true),
            field("roles", "权限", "roles", "text", false, true),
            selectField("hidden", "隐藏", "hidden", staticSource(
                option("显示", false),
                option("隐藏", true)
            )),
            selectField("componentStyle", "样式配置", "componentStyle", staticSource(
                option("默认", "default"),
                option("透明玻璃", "glass")
            ))
        );
    }

    private FormPageConfig page(String pageKey,
                                String routePath,
                                String pageTitle,
                                String quickSearchPlaceholder,
                                List<String> quickSearchFields,
                                FormFieldConfig... fields) {
        FormPageConfig config = new FormPageConfig();
        config.setPageKey(pageKey);
        config.setRoutePath(routePath);
        config.setPageTitle(pageTitle);
        config.setEnabled(Boolean.TRUE);
        config.setQuickSearchPlaceholder(quickSearchPlaceholder);
        config.setQuickSearchFields(quickSearchFields);
        config.setDefaultFilterLogic("AND");
        config.setFields(new ArrayList<>(List.of(fields)));
        return config;
    }

    private FormFieldConfig field(String fieldKey,
                                  String label,
                                  String queryKey,
                                  String controlType,
                                  boolean quickSearchEnabled,
                                  boolean filterEnabled) {
        FormFieldConfig field = new FormFieldConfig();
        field.setFieldKey(fieldKey);
        field.setLabel(label);
        field.setQueryKey(queryKey);
        field.setColumnVisible(Boolean.TRUE);
        field.setColumnOrder(999);
        field.setFilterEnabled(filterEnabled);
        field.setQuickSearchEnabled(quickSearchEnabled);
        field.setControlType(controlType);
        field.setOperatorSet(defaultOperators(controlType));
        field.setDefaultOperator(defaultOperators(controlType).get(0));
        field.setDefaultMatchMode("text".equals(controlType) ? "fuzzy" : "exact");
        field.setAllowMatchModeToggle("text".equals(controlType));
        field.setAllowMultiple(false);
        field.setOptionSourceType(null);
        field.setOptionSourceConfig(new LinkedHashMap<>());
        field.setPlaceholder("请输入" + label);
        field.setHiddenButFilterable(false);
        return field;
    }

    private FormFieldConfig selectField(String fieldKey,
                                        String label,
                                        String queryKey,
                                        Map<String, Object> sourceConfig) {
        FormFieldConfig field = field(fieldKey, label, queryKey, "select", false, true);
        field.setOptionSourceType(String.valueOf(sourceConfig.get("optionSourceType")));
        field.setOptionSourceConfig(sourceConfig);
        field.setPlaceholder("请选择" + label);
        return field;
    }

    private FormFieldConfig remoteFormField(String fieldKey,
                                            String label,
                                            String queryKey,
                                            String sourcePageKey,
                                            String labelField,
                                            String valueField,
                                            List<String> searchFields) {
        FormFieldConfig field = field(fieldKey, label, queryKey, "remote-form", false, true);
        field.setOptionSourceType("remote-form");
        field.setOptionSourceConfig(remoteFormSource(sourcePageKey, labelField, valueField, searchFields));
        field.setPlaceholder("搜索选择" + label);
        return field;
    }

    private List<String> defaultOperators(String controlType) {
        return switch (controlType) {
            case "text" -> List.of("contains", "equals");
            case "select", "remote-api", "remote-form" -> List.of("equals", "in");
            case "date" -> List.of("on");
            case "date-range", "number-range" -> List.of("between");
            case "number" -> List.of("equals", "gt", "lt");
            default -> List.of("equals");
        };
    }

    private Map<String, Object> dictSource(String dictType) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("optionSourceType", "dict");
        config.put("dictType", dictType);
        return config;
    }

    private Map<String, Object> staticSource(Map<String, Object>... options) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("optionSourceType", "static");
        config.put("options", List.of(options));
        return config;
    }

    private Map<String, Object> remoteFormSource(String sourcePageKey,
                                                 String labelField,
                                                 String valueField,
                                                 List<String> searchFields) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("optionSourceType", "remote-form");
        config.put("sourcePageKey", sourcePageKey);
        config.put("labelField", labelField);
        config.put("valueField", valueField);
        config.put("searchFields", searchFields);
        config.put("sourceFieldKeys", searchFields);
        config.put("joinField", valueField);
        config.put("multiple", false);
        return config;
    }

    private Map<String, Object> option(String label, Object value) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("label", label);
        option.put("value", value);
        return option;
    }
}
