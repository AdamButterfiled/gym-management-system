package com.gym.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.BookingCreateRequest;
import com.gym.dto.PaymentCreateRequest;
import com.gym.entity.BodyMetric;
import com.gym.entity.BookingItem;
import com.gym.entity.BookingOrder;
import com.gym.entity.CheckinRecord;
import com.gym.entity.Coach;
import com.gym.entity.CourseSchedule;
import com.gym.entity.MemberMembership;
import com.gym.entity.MemberPrivatePackage;
import com.gym.entity.MembershipPackage;
import com.gym.entity.PaymentOrder;
import com.gym.entity.PrivatePackage;
import com.gym.entity.PrivateSchedule;
import com.gym.entity.ResourceSlot;
import com.gym.entity.ScheduleConflict;
import com.gym.entity.TrainingLog;
import com.gym.entity.User;
import com.gym.entity.Venue;
import com.gym.mapper.BodyMetricMapper;
import com.gym.mapper.BookingItemMapper;
import com.gym.mapper.BookingOrderMapper;
import com.gym.mapper.CheckinRecordMapper;
import com.gym.mapper.CoachMapper;
import com.gym.mapper.CourseScheduleMapper;
import com.gym.mapper.MemberMembershipMapper;
import com.gym.mapper.MemberPrivatePackageMapper;
import com.gym.mapper.MembershipPackageMapper;
import com.gym.mapper.PaymentOrderMapper;
import com.gym.mapper.PrivatePackageMapper;
import com.gym.mapper.PrivateScheduleMapper;
import com.gym.mapper.ResourceSlotMapper;
import com.gym.mapper.ScheduleConflictMapper;
import com.gym.mapper.TrainingLogMapper;
import com.gym.mapper.UserMapper;
import com.gym.mapper.VenueMapper;
import com.gym.service.GymV2Service;
import com.gym.utils.SecurityUtils;
import com.gym.vo.AdminOverviewVO;
import com.gym.vo.BookingCreateResponseVO;
import com.gym.vo.CheckinTokenVO;
import com.gym.vo.CoachDashboardVO;
import com.gym.vo.MemberHomeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GymV2ServiceImpl implements GymV2Service {

    private static final DateTimeFormatter ORDER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VenueMapper venueMapper;
    @Autowired
    private CoachMapper coachMapper;
    @Autowired
    private MembershipPackageMapper membershipPackageMapper;
    @Autowired
    private MemberMembershipMapper memberMembershipMapper;
    @Autowired
    private PrivatePackageMapper privatePackageMapper;
    @Autowired
    private MemberPrivatePackageMapper memberPrivatePackageMapper;
    @Autowired
    private CourseScheduleMapper courseScheduleMapper;
    @Autowired
    private PrivateScheduleMapper privateScheduleMapper;
    @Autowired
    private ResourceSlotMapper resourceSlotMapper;
    @Autowired
    private BookingOrderMapper bookingOrderMapper;
    @Autowired
    private BookingItemMapper bookingItemMapper;
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private CheckinRecordMapper checkinRecordMapper;
    @Autowired
    private BodyMetricMapper bodyMetricMapper;
    @Autowired
    private TrainingLogMapper trainingLogMapper;
    @Autowired
    private ScheduleConflictMapper scheduleConflictMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public AdminOverviewVO getAdminOverview() {
        AdminOverviewVO overview = new AdminOverviewVO();
        LocalDate today = LocalDate.now();

        overview.setMemberCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "MEMBER")));
        overview.setCoachCount(coachMapper.selectCount(new LambdaQueryWrapper<Coach>().eq(Coach::getStatus, 1)));
        overview.setTodayBookings(bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .ge(BookingOrder::getCreatedAt, today.atStartOfDay())
                .lt(BookingOrder::getCreatedAt, today.plusDays(1).atStartOfDay())));
        overview.setPendingCoachApprovals(bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getResourceType, "PRIVATE_COACH")
                .eq(BookingOrder::getStatus, "CREATED")));

        BigDecimal totalRevenue = sumPayments(null);
        BigDecimal todayRevenue = sumPayments(today);
        overview.setTotalRevenue(totalRevenue);
        overview.setTodayRevenue(todayRevenue);

        long totalSlots = resourceSlotMapper.selectCount(null);
        List<ResourceSlot> slots = totalSlots == 0 ? Collections.emptyList() : resourceSlotMapper.selectList(null);
        long occupied = slots.stream().mapToLong(slot -> slot.getOccupiedCount() == null ? 0 : slot.getOccupiedCount()).sum();
        long capacity = slots.stream().mapToLong(slot -> slot.getCapacity() == null ? 0 : slot.getCapacity()).sum();
        overview.setVenueUsageRate(capacity == 0 ? 0D : round2((occupied * 100D) / capacity));

        List<CourseSchedule> schedules = courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>()
                .ge(CourseSchedule::getStartTime, today.atStartOfDay())
                .lt(CourseSchedule::getStartTime, today.plusDays(7).atStartOfDay()));
        int booked = schedules.stream().mapToInt(item -> item.getBookedCount() == null ? 0 : item.getBookedCount()).sum();
        int scheduleCapacity = schedules.stream().mapToInt(item -> item.getCapacity() == null ? 0 : item.getCapacity()).sum();
        overview.setCourseAttendanceRate(scheduleCapacity == 0 ? 0D : round2((booked * 100D) / scheduleCapacity));

        long confirmed = bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .in(BookingOrder::getStatus, List.of("CONFIRMED", "CHECKED_IN", "COMPLETED")));
        long checkedIn = bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getStatus, "CHECKED_IN"));
        overview.setCheckinRate(confirmed == 0 ? 0D : round2((checkedIn * 100D) / confirmed));
        return overview;
    }

    @Override
    public Map<String, Object> getAdminAnalytics() {
        Map<String, Object> result = new HashMap<>();
        List<String> days = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<Long> bookings = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            days.add(day.toString());
            revenues.add(sumPayments(day));
            bookings.add(bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                    .ge(BookingOrder::getCreatedAt, day.atStartOfDay())
                    .lt(BookingOrder::getCreatedAt, day.plusDays(1).atStartOfDay())));
        }

        List<Map<String, Object>> coachWorkload = coachMapper.selectList(new LambdaQueryWrapper<Coach>().eq(Coach::getStatus, 1))
                .stream()
                .map(coach -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("coachName", coach.getName());
                    item.put("privateLessons", bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                            .eq(BookingOrder::getCoachId, coach.getId())
                            .eq(BookingOrder::getResourceType, "PRIVATE_COACH")
                            .in(BookingOrder::getStatus, List.of("CREATED", "CONFIRMED", "CHECKED_IN", "COMPLETED"))));
                    item.put("groupLessons", courseScheduleMapper.selectCount(new LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getCoachId, coach.getId())));
                    return item;
                })
                .collect(Collectors.toList());

        result.put("days", days);
        result.put("revenues", revenues);
        result.put("bookings", bookings);
        result.put("coachWorkload", coachWorkload);
        result.put("overview", getAdminOverview());
        return result;
    }

    @Override
    public Page<Venue> pageVenues(Integer pageNum, Integer pageSize, String name) {
        Page<Venue> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Venue::getName, name);
        }
        wrapper.orderByDesc(Venue::getId);
        return venueMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean saveVenue(Venue venue) {
        if (venue.getId() == null) {
            return venueMapper.insert(venue) > 0;
        }
        return venueMapper.updateById(venue) > 0;
    }

    @Override
    public boolean deleteVenue(Long id) {
        return venueMapper.deleteById(id) > 0;
    }

    @Override
    public Page<Coach> pageCoaches(Integer pageNum, Integer pageSize, String name) {
        Page<Coach> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Coach::getName, name);
        }
        wrapper.orderByDesc(Coach::getId);
        return coachMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public boolean saveCoach(Coach coach) {
        User linkedUser = null;
        if (coach.getUserId() == null) {
            linkedUser = new User();
            linkedUser.setUsername(buildCoachUsername(coach));
            linkedUser.setPassword(passwordEncoder.encode("123456"));
            linkedUser.setRole("COACH");
            linkedUser.setRealName(coach.getName());
            linkedUser.setPhone(coach.getPhone());
            linkedUser.setType("COACH");
            linkedUser.setStatus("ACTIVE");
            userMapper.insert(linkedUser);
            coach.setUserId(linkedUser.getId());
        } else {
            linkedUser = userMapper.selectById(coach.getUserId());
        }

        if (coach.getId() == null) {
            coachMapper.insert(coach);
        } else {
            coachMapper.updateById(coach);
        }

        if (linkedUser != null) {
            linkedUser.setRealName(coach.getName());
            linkedUser.setPhone(coach.getPhone());
            linkedUser.setAvatar(coach.getAvatar());
            userMapper.updateById(linkedUser);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean deleteCoach(Long id) {
        Coach coach = coachMapper.selectById(id);
        if (coach == null) {
            return false;
        }
        coachMapper.deleteById(id);
        if (coach.getUserId() != null) {
            User user = userMapper.selectById(coach.getUserId());
            if (user != null) {
                user.setStatus("INACTIVE");
                userMapper.updateById(user);
            }
        }
        return true;
    }

    @Override
    public Page<CourseSchedule> pageCourseSchedules(Integer pageNum, Integer pageSize, String name) {
        Page<CourseSchedule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(CourseSchedule::getName, name);
        }
        wrapper.orderByDesc(CourseSchedule::getStartTime);
        return courseScheduleMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public boolean saveCourseSchedule(CourseSchedule schedule) {
        ensureScheduleNoConflict("GROUP_COURSE", schedule.getId(), schedule.getVenueId(), schedule.getCoachId(),
                schedule.getStartTime(), schedule.getEndTime());
        if (schedule.getBookedCount() == null) {
            schedule.setBookedCount(0);
        }
        if (schedule.getFlashSale() == null) {
            schedule.setFlashSale(0);
        }
        if (schedule.getStatus() == null) {
            schedule.setStatus("PUBLISHED");
        }
        if (schedule.getId() == null) {
            courseScheduleMapper.insert(schedule);
        } else {
            courseScheduleMapper.updateById(schedule);
        }
        upsertSlot("GROUP_COURSE", schedule.getId(), schedule.getVenueId(), schedule.getCoachId(),
                schedule.getStartTime(), schedule.getEndTime(), schedule.getCapacity(), "OPEN");
        return true;
    }

    @Override
    @Transactional
    public boolean deleteCourseSchedule(Long id) {
        courseScheduleMapper.deleteById(id);
        removeSlot("GROUP_COURSE", id);
        return true;
    }

    @Override
    public Page<PrivateSchedule> pagePrivateSchedules(Integer pageNum, Integer pageSize, Long coachId) {
        Page<PrivateSchedule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PrivateSchedule> wrapper = new LambdaQueryWrapper<>();
        if (coachId != null) {
            wrapper.eq(PrivateSchedule::getCoachId, coachId);
        }
        wrapper.orderByDesc(PrivateSchedule::getStartTime);
        return privateScheduleMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public boolean savePrivateSchedule(PrivateSchedule schedule) {
        ensureScheduleNoConflict("PRIVATE_COACH", schedule.getId(), schedule.getVenueId(), schedule.getCoachId(),
                schedule.getStartTime(), schedule.getEndTime());
        if (schedule.getBookedCount() == null) {
            schedule.setBookedCount(0);
        }
        if (schedule.getCapacity() == null) {
            schedule.setCapacity(1);
        }
        if (schedule.getStatus() == null) {
            schedule.setStatus("OPEN");
        }
        if (schedule.getId() == null) {
            privateScheduleMapper.insert(schedule);
        } else {
            privateScheduleMapper.updateById(schedule);
        }
        upsertSlot("PRIVATE_COACH", schedule.getId(), schedule.getVenueId(), schedule.getCoachId(),
                schedule.getStartTime(), schedule.getEndTime(), schedule.getCapacity(), "OPEN");
        return true;
    }

    @Override
    @Transactional
    public boolean deletePrivateSchedule(Long id) {
        privateScheduleMapper.deleteById(id);
        removeSlot("PRIVATE_COACH", id);
        return true;
    }

    @Override
    public Page<BookingOrder> pageBookings(Integer pageNum, Integer pageSize, String resourceType, String status) {
        Page<BookingOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BookingOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(resourceType)) {
            wrapper.eq(BookingOrder::getResourceType, resourceType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BookingOrder::getStatus, status);
        }
        wrapper.orderByDesc(BookingOrder::getCreatedAt);
        return bookingOrderMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<PaymentOrder> pagePayments(Integer pageNum, Integer pageSize, String paymentType, String status) {
        Page<PaymentOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(paymentType)) {
            wrapper.eq(PaymentOrder::getPaymentType, paymentType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PaymentOrder::getStatus, status);
        }
        wrapper.orderByDesc(PaymentOrder::getCreatedAt);
        return paymentOrderMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<CheckinRecord> pageCheckins(Integer pageNum, Integer pageSize) {
        Page<CheckinRecord> page = new Page<>(pageNum, pageSize);
        return checkinRecordMapper.selectPage(page, new LambdaQueryWrapper<CheckinRecord>().orderByDesc(CheckinRecord::getCheckinTime));
    }

    @Override
    public Page<ScheduleConflict> pageConflicts(Integer pageNum, Integer pageSize) {
        Page<ScheduleConflict> page = new Page<>(pageNum, pageSize);
        return scheduleConflictMapper.selectPage(page, new LambdaQueryWrapper<ScheduleConflict>().orderByDesc(ScheduleConflict::getCreatedAt));
    }

    @Override
    public Page<Map<String, Object>> pageMemberAssets(Integer pageNum, Integer pageSize, String keyword) {
        Page<User> userPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getRole, "MEMBER");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        wrapper.orderByDesc(User::getId);
        Page<User> pageResult = userMapper.selectPage(userPage, wrapper);

        List<Map<String, Object>> records = pageResult.getRecords().stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("realName", user.getRealName());
            item.put("balance", user.getBalance());
            MemberMembership membership = getActiveMembership(user.getId());
            item.put("membership", membership == null ? null : membership.getMembershipName());
            item.put("membershipEndDate", membership == null ? null : membership.getEndDate());
            int remainingSessions = memberPrivatePackageMapper.selectList(new LambdaQueryWrapper<MemberPrivatePackage>()
                    .eq(MemberPrivatePackage::getUserId, user.getId())
                    .eq(MemberPrivatePackage::getStatus, "ACTIVE"))
                    .stream()
                    .mapToInt(MemberPrivatePackage::getRemainingSessions)
                    .sum();
            item.put("remainingPrivateSessions", remainingSessions);
            return item;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, pageResult.getTotal());
        page.setRecords(records);
        return page;
    }

    @Override
    public List<MembershipPackage> listMembershipPackages() {
        return membershipPackageMapper.selectList(new LambdaQueryWrapper<MembershipPackage>()
                .eq(MembershipPackage::getStatus, "ACTIVE")
                .orderByAsc(MembershipPackage::getPrice));
    }

    @Override
    public boolean saveMembershipPackage(MembershipPackage membershipPackage) {
        if (membershipPackage.getId() == null) {
            return membershipPackageMapper.insert(membershipPackage) > 0;
        }
        return membershipPackageMapper.updateById(membershipPackage) > 0;
    }

    @Override
    public List<PrivatePackage> listPrivatePackages() {
        return privatePackageMapper.selectList(new LambdaQueryWrapper<PrivatePackage>()
                .eq(PrivatePackage::getStatus, "ACTIVE")
                .orderByAsc(PrivatePackage::getPrice));
    }

    @Override
    public boolean savePrivatePackage(PrivatePackage privatePackage) {
        if (privatePackage.getId() == null) {
            return privatePackageMapper.insert(privatePackage) > 0;
        }
        return privatePackageMapper.updateById(privatePackage) > 0;
    }

    @Override
    public MemberHomeVO getMemberHome() {
        User user = currentUser();
        MemberHomeVO home = new MemberHomeVO();
        home.setBalance(user.getBalance() == null ? BigDecimal.ZERO : user.getBalance());
        MemberMembership membership = getActiveMembership(user.getId());
        home.setActiveMembershipName(membership == null ? null : membership.getMembershipName());
        int remainingSessions = memberPrivatePackageMapper.selectList(new LambdaQueryWrapper<MemberPrivatePackage>()
                .eq(MemberPrivatePackage::getUserId, user.getId())
                .eq(MemberPrivatePackage::getStatus, "ACTIVE"))
                .stream()
                .mapToInt(MemberPrivatePackage::getRemainingSessions)
                .sum();
        home.setRemainingPrivateSessions(remainingSessions);
        home.setUpcomingBookings(bookingOrderMapper.selectList(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, user.getId())
                .in(BookingOrder::getStatus, List.of("CREATED", "PENDING_PAY", "CONFIRMED"))
                .ge(BookingOrder::getStartTime, LocalDateTime.now())
                .orderByAsc(BookingOrder::getStartTime)
                .last("limit 5")));
        return home;
    }

    @Override
    public List<Venue> listMemberVenues() {
        return venueMapper.selectList(new LambdaQueryWrapper<Venue>()
                .eq(Venue::getStatus, 1)
                .orderByAsc(Venue::getId));
    }

    @Override
    public List<Map<String, Object>> listMemberCourses() {
        return courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>()
                .ge(CourseSchedule::getStartTime, LocalDateTime.now())
                .eq(CourseSchedule::getStatus, "PUBLISHED")
                .orderByAsc(CourseSchedule::getStartTime))
                .stream()
                .map(schedule -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", schedule.getId());
                    item.put("name", schedule.getName());
                    item.put("coachName", coachName(schedule.getCoachId()));
                    item.put("venueName", venueName(schedule.getVenueId()));
                    item.put("startTime", schedule.getStartTime());
                    item.put("endTime", schedule.getEndTime());
                    item.put("capacity", schedule.getCapacity());
                    item.put("bookedCount", schedule.getBookedCount());
                    item.put("availableCount", schedule.getCapacity() - schedule.getBookedCount());
                    item.put("flashSale", schedule.getFlashSale());
                    item.put("price", schedule.getNormalPrice());
                    item.put("flashSalePrice", schedule.getFlashSalePrice());
                    item.put("description", schedule.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listMemberCoaches() {
        return coachMapper.selectList(new LambdaQueryWrapper<Coach>().eq(Coach::getStatus, 1).orderByAsc(Coach::getId))
                .stream()
                .map(coach -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", coach.getId());
                    item.put("name", coach.getName());
                    item.put("specialization", coach.getSpecialization());
                    item.put("intro", coach.getIntro());
                    item.put("hourlyPrice", coach.getHourlyPrice());
                    item.put("rating", coach.getRating());
                    item.put("packages", privatePackageMapper.selectList(new LambdaQueryWrapper<PrivatePackage>()
                            .eq(PrivatePackage::getCoachId, coach.getId())
                            .eq(PrivatePackage::getStatus, "ACTIVE")));
                    item.put("schedules", privateScheduleMapper.selectList(new LambdaQueryWrapper<PrivateSchedule>()
                            .eq(PrivateSchedule::getCoachId, coach.getId())
                            .eq(PrivateSchedule::getStatus, "OPEN")
                            .ge(PrivateSchedule::getStartTime, LocalDateTime.now())
                            .orderByAsc(PrivateSchedule::getStartTime)
                            .last("limit 5")));
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingOrder> listMemberBookings() {
        return bookingOrderMapper.selectList(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, currentUser().getId())
                .orderByDesc(BookingOrder::getCreatedAt));
    }

    @Override
    public List<PaymentOrder> listMemberPayments() {
        return paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getUserId, currentUser().getId())
                .orderByDesc(PaymentOrder::getCreatedAt));
    }

    @Override
    @Transactional
    public BookingCreateResponseVO createBooking(BookingCreateRequest request) {
        User user = currentUser();
        if (!StringUtils.hasText(request.getResourceType()) || request.getResourceId() == null) {
            throw new IllegalArgumentException("预约参数不完整");
        }
        if (StringUtils.hasText(request.getIdempotentKey())) {
            BookingOrder existing = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                    .eq(BookingOrder::getUserId, user.getId())
                    .eq(BookingOrder::getIdempotentKey, request.getIdempotentKey()));
            if (existing != null) {
                BookingCreateResponseVO response = new BookingCreateResponseVO();
                response.setBooking(existing);
                response.setCheckinEligible("CONFIRMED".equals(existing.getStatus()));
                response.setMessage("重复请求已幂等返回");
                return response;
            }
        }

        BookingOrder booking = new BookingOrder();
        booking.setOrderNo(generateOrderNo("BK"));
        booking.setUserId(user.getId());
        booking.setResourceType(request.getResourceType());
        booking.setSource(StringUtils.hasText(request.getSource()) ? request.getSource() : "WEB");
        booking.setIdempotentKey(request.getIdempotentKey());
        booking.setPaymentStatus("PAID");
        booking.setStatus("CONFIRMED");
        booking.setAmount(BigDecimal.ZERO);
        booking.setRemark("");

        ResourceSlot slot;
        BookingItem item = new BookingItem();
        item.setQuantity(1);
        item.setAmount(BigDecimal.ZERO);
        PaymentOrder paymentOrder = null;

        switch (request.getResourceType()) {
            case "GROUP_COURSE" -> {
                CourseSchedule schedule = requireCourse(request.getResourceId());
                ensureMemberTimeConflict(user.getId(), schedule.getStartTime(), schedule.getEndTime(), null);
                slot = requireSlot("GROUP_COURSE", schedule.getId());
                reserveSlot(schedule.getId(), slot.getId(), schedule.getFlashSale() != null && schedule.getFlashSale() == 1);
                courseScheduleMapper.adjustBookedCount(schedule.getId(), 1);

                booking.setResourceId(schedule.getId());
                booking.setResourceName(schedule.getName());
                booking.setVenueId(schedule.getVenueId());
                booking.setCoachId(schedule.getCoachId());
                booking.setBookingDate(schedule.getStartTime().toLocalDate());
                booking.setStartTime(schedule.getStartTime());
                booking.setEndTime(schedule.getEndTime());
                booking.setAmount(resolveCoursePrice(schedule));
                booking.setStatus(booking.getAmount().compareTo(BigDecimal.ZERO) > 0 ? "PENDING_PAY" : "CONFIRMED");
                booking.setPaymentStatus(booking.getAmount().compareTo(BigDecimal.ZERO) > 0 ? "UNPAID" : "PAID");
                booking.setRemark(schedule.getFlashSale() != null && schedule.getFlashSale() == 1 ? "秒杀课程待支付" : "团课预约");
                bookingOrderMapper.insert(booking);

                item.setBookingOrderId(booking.getId());
                item.setSlotId(slot.getId());
                item.setAmount(booking.getAmount());
                bookingItemMapper.insert(item);

                if (booking.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    paymentOrder = buildPaymentOrder(user.getId(), "BOOKING", "BOOKING", booking.getId(), booking.getAmount(), null);
                    paymentOrderMapper.insert(paymentOrder);
                }
            }
            case "PRIVATE_COACH" -> {
                PrivateSchedule schedule = requirePrivateSchedule(request.getResourceId());
                ensureMemberTimeConflict(user.getId(), schedule.getStartTime(), schedule.getEndTime(), null);
                slot = requireSlot("PRIVATE_COACH", schedule.getId());
                if (resourceSlotMapper.reserve(slot.getId(), 1) == 0) {
                    throw new IllegalStateException("私教时段已被预约");
                }
                privateScheduleMapper.adjustBookedCount(schedule.getId(), 1);

                MemberPrivatePackage memberPackage = memberPrivatePackageMapper.selectOne(new LambdaQueryWrapper<MemberPrivatePackage>()
                        .eq(MemberPrivatePackage::getUserId, user.getId())
                        .eq(MemberPrivatePackage::getCoachId, schedule.getCoachId())
                        .eq(MemberPrivatePackage::getStatus, "ACTIVE")
                        .gt(MemberPrivatePackage::getRemainingSessions, 0)
                        .orderByAsc(MemberPrivatePackage::getId)
                        .last("limit 1"));
                if (memberPackage == null || memberPrivatePackageMapper.consumeSession(memberPackage.getId()) == 0) {
                    resourceSlotMapper.release(slot.getId(), 1);
                    privateScheduleMapper.adjustBookedCount(schedule.getId(), -1);
                    throw new IllegalStateException("私教剩余课时不足");
                }

                booking.setResourceId(schedule.getId());
                booking.setResourceName("私教课 - " + coachName(schedule.getCoachId()));
                booking.setVenueId(schedule.getVenueId());
                booking.setCoachId(schedule.getCoachId());
                booking.setBookingDate(schedule.getStartTime().toLocalDate());
                booking.setStartTime(schedule.getStartTime());
                booking.setEndTime(schedule.getEndTime());
                booking.setStatus("CREATED");
                booking.setRemark("等待教练确认");
                bookingOrderMapper.insert(booking);

                item.setBookingOrderId(booking.getId());
                item.setSlotId(slot.getId());
                item.setPackageId(memberPackage.getPackageId());
                item.setMemberPackageId(memberPackage.getId());
                bookingItemMapper.insert(item);
            }
            case "VENUE" -> {
                Venue venue = requireVenue(request.getResourceId());
                if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
                    throw new IllegalArgumentException("场馆预约需要合法的时间段");
                }
                ensureMemberTimeConflict(user.getId(), request.getStartTime(), request.getEndTime(), null);
                slot = findOrCreateVenueSlot(venue, request.getStartTime(), request.getEndTime());
                if (resourceSlotMapper.reserve(slot.getId(), 1) == 0) {
                    throw new IllegalStateException("该场馆时段已被占用");
                }

                MemberMembership membership = getActiveMembership(user.getId());
                BigDecimal amount = membership == null
                        ? calculateVenueBookingAmount(venue, request.getStartTime(), request.getEndTime())
                        : BigDecimal.ZERO;
                booking.setResourceId(venue.getId());
                booking.setResourceName(venue.getName());
                booking.setVenueId(venue.getId());
                booking.setBookingDate(request.getStartTime().toLocalDate());
                booking.setStartTime(request.getStartTime());
                booking.setEndTime(request.getEndTime());
                booking.setAmount(amount);
                booking.setStatus(amount.compareTo(BigDecimal.ZERO) > 0 ? "PENDING_PAY" : "CONFIRMED");
                booking.setPaymentStatus(amount.compareTo(BigDecimal.ZERO) > 0 ? "UNPAID" : "PAID");
                booking.setRemark(membership == null ? "场馆预约待支付" : "会员场馆预约");
                bookingOrderMapper.insert(booking);

                item.setBookingOrderId(booking.getId());
                item.setSlotId(slot.getId());
                item.setAmount(amount);
                bookingItemMapper.insert(item);

                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    paymentOrder = buildPaymentOrder(user.getId(), "BOOKING", "BOOKING", booking.getId(), amount, null);
                    paymentOrderMapper.insert(paymentOrder);
                }
            }
            default -> throw new IllegalArgumentException("不支持的预约资源类型");
        }

        BookingCreateResponseVO response = new BookingCreateResponseVO();
        response.setBooking(bookingOrderMapper.selectById(booking.getId()));
        response.setPaymentOrder(paymentOrder);
        response.setCheckinEligible("CONFIRMED".equals(booking.getStatus()));
        response.setMessage("预约创建成功");
        return response;
    }

    @Override
    @Transactional
    public boolean cancelBooking(Long bookingId) {
        BookingOrder booking = bookingOrderMapper.selectById(bookingId);
        User user = currentUser();
        if (booking == null || !Objects.equals(booking.getUserId(), user.getId())) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (List.of("CANCELLED", "REFUNDED").contains(booking.getStatus())) {
            return true;
        }
        restoreBookingResources(booking, true);
        return true;
    }

    @Override
    @Transactional
    public PaymentOrder createPayment(PaymentCreateRequest request) {
        User user = currentUser();
        if (!StringUtils.hasText(request.getPaymentType())) {
            throw new IllegalArgumentException("支付类型不能为空");
        }
        return switch (request.getPaymentType()) {
            case "RECHARGE" -> insertPaymentOrder(buildPaymentOrder(user.getId(), "RECHARGE", "BALANCE", null, request.getAmount(), null));
            case "MEMBERSHIP" -> {
                MembershipPackage membershipPackage = membershipPackageMapper.selectById(request.getTargetId());
                if (membershipPackage == null) {
                    throw new IllegalArgumentException("会籍套餐不存在");
                }
                yield insertPaymentOrder(buildPaymentOrder(user.getId(), "MEMBERSHIP", "MEMBERSHIP_PACKAGE", membershipPackage.getId(), membershipPackage.getPrice(), membershipPackage.getName()));
            }
            case "PRIVATE_PACKAGE" -> {
                PrivatePackage privatePackage = privatePackageMapper.selectById(request.getTargetId());
                if (privatePackage == null) {
                    throw new IllegalArgumentException("私教课包不存在");
                }
                yield insertPaymentOrder(buildPaymentOrder(user.getId(), "PRIVATE_PACKAGE", "PRIVATE_PACKAGE", privatePackage.getId(), privatePackage.getPrice(), privatePackage.getName()));
            }
            case "BOOKING" -> {
                BookingOrder booking = bookingOrderMapper.selectById(request.getTargetId());
                if (booking == null || !Objects.equals(booking.getUserId(), user.getId())) {
                    throw new IllegalArgumentException("预约订单不存在");
                }
                PaymentOrder existing = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getTargetType, "BOOKING")
                        .eq(PaymentOrder::getTargetId, booking.getId())
                        .eq(PaymentOrder::getStatus, "UNPAID")
                        .last("limit 1"));
                if (existing != null) {
                    yield existing;
                }
                yield insertPaymentOrder(buildPaymentOrder(user.getId(), "BOOKING", "BOOKING", booking.getId(), booking.getAmount(), booking.getResourceName()));
            }
            default -> throw new IllegalArgumentException("不支持的支付类型");
        };
    }

    @Override
    @Transactional
    public PaymentOrder payOrder(String paymentNo) {
        User user = currentUser();
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentNo, paymentNo)
                .eq(PaymentOrder::getUserId, user.getId())
                .last("limit 1"));
        if (paymentOrder == null) {
            throw new IllegalArgumentException("支付单不存在");
        }
        if ("PAID".equals(paymentOrder.getStatus())) {
            return paymentOrder;
        }

        if (!"RECHARGE".equals(paymentOrder.getPaymentType())) {
            BigDecimal balance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
            if (balance.compareTo(paymentOrder.getAmount()) < 0) {
                throw new IllegalStateException("余额不足，请先充值");
            }
            user.setBalance(balance.subtract(paymentOrder.getAmount()));
            userMapper.updateById(user);
        }

        paymentOrder.setStatus("PAID");
        paymentOrder.setPaidAt(LocalDateTime.now());
        paymentOrderMapper.updateById(paymentOrder);

        switch (paymentOrder.getPaymentType()) {
            case "RECHARGE" -> {
                user.setBalance((user.getBalance() == null ? BigDecimal.ZERO : user.getBalance()).add(paymentOrder.getAmount()));
                userMapper.updateById(user);
            }
            case "MEMBERSHIP" -> grantMembership(user.getId(), paymentOrder.getTargetId());
            case "PRIVATE_PACKAGE" -> grantPrivatePackage(user.getId(), paymentOrder.getTargetId());
            case "BOOKING" -> confirmPaidBooking(paymentOrder.getTargetId(), paymentOrder.getId());
            default -> throw new IllegalArgumentException("未知支付类型");
        }
        return paymentOrderMapper.selectById(paymentOrder.getId());
    }

    @Override
    @Transactional
    public CheckinTokenVO createCheckinToken(Long bookingId) {
        User user = currentUser();
        BookingOrder booking = bookingOrderMapper.selectById(bookingId);
        if (booking == null || !Objects.equals(booking.getUserId(), user.getId())) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!"CONFIRMED".equals(booking.getStatus()) && !"CHECKED_IN".equals(booking.getStatus())) {
            throw new IllegalStateException("当前预约状态不能生成签到码");
        }

        booking.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        booking.setQrExpireTime(LocalDateTime.now().plusMinutes(10));
        bookingOrderMapper.updateById(booking);

        CheckinTokenVO vo = new CheckinTokenVO();
        vo.setBookingId(bookingId);
        vo.setToken(booking.getQrToken());
        vo.setExpireTime(booking.getQrExpireTime());
        return vo;
    }

    @Override
    @Transactional
    public boolean consumeCheckinToken(String token, String operatorName) {
        BookingOrder booking = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getQrToken, token)
                .last("limit 1"));
        if (booking == null) {
            throw new IllegalArgumentException("签到码无效");
        }
        if (booking.getQrExpireTime() == null || booking.getQrExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("签到码已过期");
        }
        if ("CHECKED_IN".equals(booking.getStatus())) {
            throw new IllegalStateException("请勿重复签到");
        }
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new IllegalStateException("预约尚未确认，无法签到");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(booking.getStartTime().minusHours(1)) || now.isAfter(booking.getEndTime().plusHours(1))) {
            throw new IllegalStateException("当前不在签到时间窗口内");
        }
        booking.setStatus("CHECKED_IN");
        booking.setCheckedInAt(now);
        bookingOrderMapper.updateById(booking);

        CheckinRecord record = new CheckinRecord();
        record.setBookingOrderId(booking.getId());
        record.setUserId(booking.getUserId());
        record.setCheckinCode(token);
        record.setCheckinTime(now);
        record.setStatus("SUCCESS");
        record.setOperatorName(StringUtils.hasText(operatorName) ? operatorName : "SELF");
        checkinRecordMapper.insert(record);
        return true;
    }

    @Override
    public CoachDashboardVO getCoachDashboard() {
        Coach coach = currentCoach();
        CoachDashboardVO dashboard = new CoachDashboardVO();
        LocalDate today = LocalDate.now();

        long groupLessons = courseScheduleMapper.selectCount(new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getCoachId, coach.getId())
                .ge(CourseSchedule::getStartTime, today.atStartOfDay())
                .lt(CourseSchedule::getStartTime, today.plusDays(1).atStartOfDay()));
        long privateLessons = privateScheduleMapper.selectCount(new LambdaQueryWrapper<PrivateSchedule>()
                .eq(PrivateSchedule::getCoachId, coach.getId())
                .ge(PrivateSchedule::getStartTime, today.atStartOfDay())
                .lt(PrivateSchedule::getStartTime, today.plusDays(1).atStartOfDay()));

        dashboard.setTodayLessons(groupLessons + privateLessons);
        dashboard.setPendingApprovals(bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getCoachId, coach.getId())
                .eq(BookingOrder::getResourceType, "PRIVATE_COACH")
                .eq(BookingOrder::getStatus, "CREATED")));
        dashboard.setTotalStudents((long) bookingOrderMapper.selectList(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getCoachId, coach.getId()))
                .stream().map(BookingOrder::getUserId).distinct().count());
        dashboard.setTodayCheckins(bookingOrderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getCoachId, coach.getId())
                .eq(BookingOrder::getStatus, "CHECKED_IN")
                .ge(BookingOrder::getCheckedInAt, today.atStartOfDay())
                .lt(BookingOrder::getCheckedInAt, today.plusDays(1).atStartOfDay())));
        return dashboard;
    }

    @Override
    public List<BookingOrder> listCoachPendingBookings() {
        return bookingOrderMapper.selectList(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getCoachId, currentCoach().getId())
                .eq(BookingOrder::getResourceType, "PRIVATE_COACH")
                .eq(BookingOrder::getStatus, "CREATED")
                .orderByAsc(BookingOrder::getStartTime));
    }

    @Override
    @Transactional
    public boolean reviewCoachBooking(Long bookingId, boolean approved) {
        Coach coach = currentCoach();
        BookingOrder booking = bookingOrderMapper.selectById(bookingId);
        if (booking == null || !Objects.equals(booking.getCoachId(), coach.getId())) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!"CREATED".equals(booking.getStatus())) {
            return true;
        }
        if (approved) {
            booking.setStatus("CONFIRMED");
            booking.setRemark("教练已确认");
            bookingOrderMapper.updateById(booking);
            return true;
        }
        restoreBookingResources(booking, false);
        return true;
    }

    @Override
    public List<BodyMetric> listBodyMetrics(Long userId) {
        LambdaQueryWrapper<BodyMetric> wrapper = new LambdaQueryWrapper<BodyMetric>()
                .eq(BodyMetric::getCoachId, currentCoach().getId())
                .orderByDesc(BodyMetric::getMeasuredAt);
        if (userId != null) {
            wrapper.eq(BodyMetric::getUserId, userId);
        }
        return bodyMetricMapper.selectList(wrapper);
    }

    @Override
    public boolean saveBodyMetric(BodyMetric bodyMetric) {
        Coach coach = currentCoach();
        bodyMetric.setCoachId(coach.getId());
        if (bodyMetric.getId() != null) {
            BodyMetric existing = bodyMetricMapper.selectById(bodyMetric.getId());
            if (existing == null || !Objects.equals(existing.getCoachId(), coach.getId())) {
                throw new IllegalArgumentException("体测记录不存在");
            }
        }
        if (bodyMetric.getMeasuredAt() == null) {
            bodyMetric.setMeasuredAt(LocalDateTime.now());
        }
        if (bodyMetric.getHeight() != null && bodyMetric.getHeight().compareTo(BigDecimal.ZERO) > 0
                && bodyMetric.getWeight() != null) {
            BigDecimal heightInMeter = bodyMetric.getHeight().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            bodyMetric.setBmi(bodyMetric.getWeight().divide(heightInMeter.multiply(heightInMeter), 2, RoundingMode.HALF_UP));
        }
        if (bodyMetric.getId() == null) {
            return bodyMetricMapper.insert(bodyMetric) > 0;
        }
        return bodyMetricMapper.updateById(bodyMetric) > 0;
    }

    @Override
    public List<TrainingLog> listTrainingLogs(Long userId) {
        LambdaQueryWrapper<TrainingLog> wrapper = new LambdaQueryWrapper<TrainingLog>()
                .eq(TrainingLog::getCoachId, currentCoach().getId())
                .orderByDesc(TrainingLog::getTrainDate);
        if (userId != null) {
            wrapper.eq(TrainingLog::getUserId, userId);
        }
        return trainingLogMapper.selectList(wrapper);
    }

    @Override
    public boolean saveTrainingLog(TrainingLog trainingLog) {
        Coach coach = currentCoach();
        trainingLog.setCoachId(coach.getId());
        if (trainingLog.getId() != null) {
            TrainingLog existing = trainingLogMapper.selectById(trainingLog.getId());
            if (existing == null || !Objects.equals(existing.getCoachId(), coach.getId())) {
                throw new IllegalArgumentException("训练日志不存在");
            }
        }
        if (trainingLog.getTrainDate() == null) {
            trainingLog.setTrainDate(LocalDate.now());
        }
        if (trainingLog.getId() == null) {
            return trainingLogMapper.insert(trainingLog) > 0;
        }
        return trainingLogMapper.updateById(trainingLog) > 0;
    }

    private PaymentOrder insertPaymentOrder(PaymentOrder order) {
        paymentOrderMapper.insert(order);
        return order;
    }

    private PaymentOrder buildPaymentOrder(Long userId, String paymentType, String targetType, Long targetId, BigDecimal amount, String payload) {
        PaymentOrder order = new PaymentOrder();
        order.setPaymentNo(generateOrderNo("PAY"));
        order.setUserId(userId);
        order.setPaymentType(paymentType);
        order.setTargetType(targetType);
        order.setTargetId(targetId);
        order.setAmount(amount == null ? BigDecimal.ZERO : amount);
        order.setStatus("UNPAID");
        order.setPayloadJson(payload);
        return order;
    }

    private void confirmPaidBooking(Long bookingId, Long paymentOrderId) {
        BookingOrder booking = bookingOrderMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        booking.setPaymentStatus("PAID");
        if ("PENDING_PAY".equals(booking.getStatus())) {
            booking.setStatus("CONFIRMED");
        }
        booking.setRemark("支付完成 #" + paymentOrderId);
        bookingOrderMapper.updateById(booking);
    }

    private void grantMembership(Long userId, Long packageId) {
        MembershipPackage membershipPackage = membershipPackageMapper.selectById(packageId);
        if (membershipPackage == null) {
            throw new IllegalArgumentException("会籍套餐不存在");
        }
        MemberMembership membership = new MemberMembership();
        membership.setUserId(userId);
        membership.setPackageId(packageId);
        membership.setMembershipName(membershipPackage.getName());
        membership.setStartDate(LocalDate.now());
        membership.setEndDate(LocalDate.now().plusDays(membershipPackage.getDays()));
        membership.setStatus("ACTIVE");
        memberMembershipMapper.insert(membership);
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setType("VIP");
            userMapper.updateById(user);
        }
    }

    private void grantPrivatePackage(Long userId, Long packageId) {
        PrivatePackage privatePackage = privatePackageMapper.selectById(packageId);
        if (privatePackage == null) {
            throw new IllegalArgumentException("私教课包不存在");
        }
        MemberPrivatePackage memberPackage = new MemberPrivatePackage();
        memberPackage.setUserId(userId);
        memberPackage.setCoachId(privatePackage.getCoachId());
        memberPackage.setPackageId(packageId);
        memberPackage.setPackageName(privatePackage.getName());
        memberPackage.setTotalSessions(privatePackage.getTotalSessions());
        memberPackage.setRemainingSessions(privatePackage.getTotalSessions());
        memberPackage.setStatus("ACTIVE");
        memberPrivatePackageMapper.insert(memberPackage);
    }

    private void ensureMemberTimeConflict(Long userId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        List<BookingOrder> conflicts = bookingOrderMapper.selectList(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getUserId, userId)
                .in(BookingOrder::getStatus, List.of("CREATED", "PENDING_PAY", "CONFIRMED", "CHECKED_IN"))
                .lt(BookingOrder::getStartTime, end)
                .gt(BookingOrder::getEndTime, start));
        boolean hasConflict = conflicts.stream().anyMatch(item -> !Objects.equals(item.getId(), excludeBookingId));
        if (hasConflict) {
            throw new IllegalStateException("个人日程冲突，不能重复预约");
        }
    }

    private void ensureScheduleNoConflict(String resourceType, Long resourceId, Long venueId, Long coachId,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        List<ScheduleConflict> conflicts = new ArrayList<>();

        if (venueId != null) {
            courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>()
                    .eq(CourseSchedule::getVenueId, venueId)
                    .lt(CourseSchedule::getStartTime, endTime)
                    .gt(CourseSchedule::getEndTime, startTime))
                    .stream()
                    .filter(item -> !Objects.equals(item.getId(), resourceId))
                    .forEach(item -> conflicts.add(buildConflict(resourceType, resourceId, "VENUE_CONFLICT", item.getId(),
                            "场地与团课排期冲突", startTime, endTime)));

            privateScheduleMapper.selectList(new LambdaQueryWrapper<PrivateSchedule>()
                    .eq(PrivateSchedule::getVenueId, venueId)
                    .lt(PrivateSchedule::getStartTime, endTime)
                    .gt(PrivateSchedule::getEndTime, startTime))
                    .stream()
                    .filter(item -> !Objects.equals(item.getId(), resourceId))
                    .forEach(item -> conflicts.add(buildConflict(resourceType, resourceId, "VENUE_CONFLICT", item.getId(),
                            "场地与私教排班冲突", startTime, endTime)));
        }

        if (coachId != null) {
            courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>()
                    .eq(CourseSchedule::getCoachId, coachId)
                    .lt(CourseSchedule::getStartTime, endTime)
                    .gt(CourseSchedule::getEndTime, startTime))
                    .stream()
                    .filter(item -> !Objects.equals(item.getId(), resourceId))
                    .forEach(item -> conflicts.add(buildConflict(resourceType, resourceId, "COACH_CONFLICT", item.getId(),
                            "教练与团课排期冲突", startTime, endTime)));

            privateScheduleMapper.selectList(new LambdaQueryWrapper<PrivateSchedule>()
                    .eq(PrivateSchedule::getCoachId, coachId)
                    .lt(PrivateSchedule::getStartTime, endTime)
                    .gt(PrivateSchedule::getEndTime, startTime))
                    .stream()
                    .filter(item -> !Objects.equals(item.getId(), resourceId))
                    .forEach(item -> conflicts.add(buildConflict(resourceType, resourceId, "COACH_CONFLICT", item.getId(),
                            "教练与私教排班冲突", startTime, endTime)));
        }

        if (!conflicts.isEmpty()) {
            conflicts.forEach(scheduleConflictMapper::insert);
            throw new IllegalStateException(conflicts.get(0).getMessage());
        }
    }

    private ScheduleConflict buildConflict(String resourceType, Long resourceId, String type, Long referenceId,
                                           String message, LocalDateTime startTime, LocalDateTime endTime) {
        ScheduleConflict conflict = new ScheduleConflict();
        conflict.setResourceType(resourceType);
        conflict.setResourceId(resourceId);
        conflict.setConflictType(type);
        conflict.setReferenceId(referenceId);
        conflict.setMessage(message);
        conflict.setStartTime(startTime);
        conflict.setEndTime(endTime);
        return conflict;
    }

    private void upsertSlot(String resourceType, Long resourceId, Long venueId, Long coachId,
                            LocalDateTime startTime, LocalDateTime endTime, Integer capacity, String status) {
        ResourceSlot existing = resourceSlotMapper.selectOne(new LambdaQueryWrapper<ResourceSlot>()
                .eq(ResourceSlot::getResourceType, resourceType)
                .eq(ResourceSlot::getResourceId, resourceId)
                .last("limit 1"));
        if (existing == null) {
            ResourceSlot slot = new ResourceSlot();
            slot.setResourceType(resourceType);
            slot.setResourceId(resourceId);
            slot.setVenueId(venueId);
            slot.setCoachId(coachId);
            slot.setSlotDate(startTime.toLocalDate());
            slot.setStartTime(startTime);
            slot.setEndTime(endTime);
            slot.setCapacity(capacity);
            slot.setOccupiedCount(0);
            slot.setVersion(0);
            slot.setStatus(status);
            resourceSlotMapper.insert(slot);
            return;
        }

        existing.setVenueId(venueId);
        existing.setCoachId(coachId);
        existing.setSlotDate(startTime.toLocalDate());
        existing.setStartTime(startTime);
        existing.setEndTime(endTime);
        existing.setCapacity(capacity);
        existing.setStatus(status);
        resourceSlotMapper.updateById(existing);
    }

    private void removeSlot(String resourceType, Long resourceId) {
        resourceSlotMapper.delete(new LambdaQueryWrapper<ResourceSlot>()
                .eq(ResourceSlot::getResourceType, resourceType)
                .eq(ResourceSlot::getResourceId, resourceId));
    }

    private ResourceSlot requireSlot(String resourceType, Long resourceId) {
        ResourceSlot slot = resourceSlotMapper.selectOne(new LambdaQueryWrapper<ResourceSlot>()
                .eq(ResourceSlot::getResourceType, resourceType)
                .eq(ResourceSlot::getResourceId, resourceId)
                .last("limit 1"));
        if (slot == null) {
            throw new IllegalStateException("预约时段未发布");
        }
        return slot;
    }

    private void reserveSlot(Long resourceId, Long slotId, boolean flashSale) {
        if (flashSale && !reserveFlashSale(resourceId)) {
            throw new IllegalStateException("秒杀名额已抢光");
        }
        if (resourceSlotMapper.reserve(slotId, 1) == 0) {
            if (flashSale) {
                restoreFlashSale(resourceId);
            }
            throw new IllegalStateException("预约名额不足");
        }
    }

    private boolean reserveFlashSale(Long resourceId) {
        if (stringRedisTemplate == null) {
            return true;
        }
        String key = "gym:seckill:" + resourceId;
        try {
            String existing = stringRedisTemplate.opsForValue().get(key);
            if (existing == null) {
                CourseSchedule schedule = requireCourse(resourceId);
                int available = Math.max(schedule.getCapacity() - schedule.getBookedCount(), 0);
                stringRedisTemplate.opsForValue().set(key, String.valueOf(available), 2, TimeUnit.HOURS);
            }
            Long value = stringRedisTemplate.opsForValue().decrement(key);
            return value != null && value >= 0;
        } catch (DataAccessException ex) {
            return true;
        }
    }

    private void restoreFlashSale(Long resourceId) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().increment("gym:seckill:" + resourceId);
        } catch (DataAccessException ignored) {
        }
    }

    private ResourceSlot findOrCreateVenueSlot(Venue venue, LocalDateTime startTime, LocalDateTime endTime) {
        ResourceSlot slot = resourceSlotMapper.selectOne(new LambdaQueryWrapper<ResourceSlot>()
                .eq(ResourceSlot::getResourceType, "VENUE")
                .eq(ResourceSlot::getResourceId, venue.getId())
                .eq(ResourceSlot::getStartTime, startTime)
                .eq(ResourceSlot::getEndTime, endTime)
                .last("limit 1"));
        if (slot != null) {
            return slot;
        }
        ResourceSlot newSlot = new ResourceSlot();
        newSlot.setResourceType("VENUE");
        newSlot.setResourceId(venue.getId());
        newSlot.setVenueId(venue.getId());
        newSlot.setSlotDate(startTime.toLocalDate());
        newSlot.setStartTime(startTime);
        newSlot.setEndTime(endTime);
        newSlot.setCapacity(1);
        newSlot.setOccupiedCount(0);
        newSlot.setVersion(0);
        newSlot.setStatus("OPEN");
        resourceSlotMapper.insert(newSlot);
        return newSlot;
    }

    private void restoreBookingResources(BookingOrder booking, boolean refundToBalance) {
        BookingItem item = bookingItemMapper.selectOne(new LambdaQueryWrapper<BookingItem>()
                .eq(BookingItem::getBookingOrderId, booking.getId())
                .last("limit 1"));
        if (item != null && item.getSlotId() != null) {
            resourceSlotMapper.release(item.getSlotId(), 1);
        }

        if ("GROUP_COURSE".equals(booking.getResourceType())) {
            courseScheduleMapper.adjustBookedCount(booking.getResourceId(), -1);
            restoreFlashSale(booking.getResourceId());
        }
        if ("PRIVATE_COACH".equals(booking.getResourceType())) {
            privateScheduleMapper.adjustBookedCount(booking.getResourceId(), -1);
            if (item != null) {
                MemberPrivatePackage memberPackage = null;
                if (item.getMemberPackageId() != null) {
                    memberPackage = memberPrivatePackageMapper.selectById(item.getMemberPackageId());
                }
                if (memberPackage == null && item.getPackageId() != null) {
                    memberPackage = memberPrivatePackageMapper.selectOne(new LambdaQueryWrapper<MemberPrivatePackage>()
                            .eq(MemberPrivatePackage::getPackageId, item.getPackageId())
                            .eq(MemberPrivatePackage::getUserId, booking.getUserId())
                            .last("limit 1"));
                }
                if (memberPackage != null && Objects.equals(memberPackage.getUserId(), booking.getUserId())) {
                    memberPrivatePackageMapper.restoreSession(memberPackage.getId());
                }
            }
        }

        if (refundToBalance && "PAID".equals(booking.getPaymentStatus()) && booking.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            User user = userMapper.selectById(booking.getUserId());
            user.setBalance((user.getBalance() == null ? BigDecimal.ZERO : user.getBalance()).add(booking.getAmount()));
            userMapper.updateById(user);
            booking.setStatus("REFUNDED");
            booking.setPaymentStatus("REFUNDED");
            PaymentOrder payment = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTargetType, "BOOKING")
                    .eq(PaymentOrder::getTargetId, booking.getId())
                    .eq(PaymentOrder::getStatus, "PAID")
                    .last("limit 1"));
            if (payment != null) {
                payment.setStatus("REFUNDED");
                paymentOrderMapper.updateById(payment);
            }
        } else {
            booking.setStatus("CANCELLED");
            PaymentOrder payment = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTargetType, "BOOKING")
                    .eq(PaymentOrder::getTargetId, booking.getId())
                    .eq(PaymentOrder::getStatus, "UNPAID")
                    .last("limit 1"));
            if (payment != null) {
                payment.setStatus("CLOSED");
                paymentOrderMapper.updateById(payment);
            }
        }
        bookingOrderMapper.updateById(booking);
    }

    private CourseSchedule requireCourse(Long id) {
        CourseSchedule schedule = courseScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("团课排期不存在");
        }
        return schedule;
    }

    private PrivateSchedule requirePrivateSchedule(Long id) {
        PrivateSchedule schedule = privateScheduleMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("私教排班不存在");
        }
        return schedule;
    }

    private Venue requireVenue(Long id) {
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new IllegalArgumentException("场馆不存在");
        }
        return venue;
    }

    private BigDecimal calculateVenueBookingAmount(Venue venue, LocalDateTime startTime, LocalDateTime endTime) {
        BigDecimal pricePerHour = venue.getPricePerHour() == null ? BigDecimal.ZERO : venue.getPricePerHour();
        if (pricePerHour.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerHour
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private MemberMembership getActiveMembership(Long userId) {
        return memberMembershipMapper.selectOne(new LambdaQueryWrapper<MemberMembership>()
                .eq(MemberMembership::getUserId, userId)
                .eq(MemberMembership::getStatus, "ACTIVE")
                .ge(MemberMembership::getEndDate, LocalDate.now())
                .orderByDesc(MemberMembership::getEndDate)
                .last("limit 1"));
    }

    private User currentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException("未登录");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new IllegalStateException("登录用户不存在");
        }
        return user;
    }

    private Coach currentCoach() {
        User user = currentUser();
        Coach coach = coachMapper.selectOne(new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, user.getId()).last("limit 1"));
        if (coach == null) {
            throw new IllegalStateException("当前账号未绑定教练档案");
        }
        return coach;
    }

    private String buildCoachUsername(Coach coach) {
        String base = StringUtils.hasText(coach.getPhone()) ? coach.getPhone() : "coach" + System.currentTimeMillis();
        String candidate = "coach_" + base.substring(Math.max(0, base.length() - Math.min(base.length(), 6)));
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, candidate).last("limit 1"));
        return exists == null ? candidate : candidate + "_" + System.currentTimeMillis();
    }

    private String generateOrderNo(String prefix) {
        return prefix + ORDER_FORMATTER.format(LocalDateTime.now()) + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private BigDecimal resolveCoursePrice(CourseSchedule schedule) {
        if (schedule.getFlashSale() != null && schedule.getFlashSale() == 1) {
            return schedule.getFlashSalePrice() == null ? BigDecimal.ZERO : schedule.getFlashSalePrice();
        }
        return schedule.getNormalPrice() == null ? BigDecimal.ZERO : schedule.getNormalPrice();
    }

    private String coachName(Long coachId) {
        if (coachId == null) {
            return "-";
        }
        Coach coach = coachMapper.selectById(coachId);
        return coach == null ? "-" : coach.getName();
    }

    private String venueName(Long venueId) {
        if (venueId == null) {
            return "-";
        }
        Venue venue = venueMapper.selectById(venueId);
        return venue == null ? "-" : venue.getName();
    }

    private BigDecimal sumPayments(LocalDate day) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, "PAID");
        if (day != null) {
            wrapper.ge(PaymentOrder::getPaidAt, day.atStartOfDay())
                    .lt(PaymentOrder::getPaidAt, day.plusDays(1).atStartOfDay());
        }
        return paymentOrderMapper.selectList(wrapper).stream()
                .map(PaymentOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
