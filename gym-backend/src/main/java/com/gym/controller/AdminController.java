package com.gym.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.common.Result;
import com.gym.dto.CheckinConsumeRequest;
import com.gym.entity.BookingOrder;
import com.gym.entity.CheckinRecord;
import com.gym.entity.Coach;
import com.gym.entity.CourseSchedule;
import com.gym.entity.MembershipPackage;
import com.gym.entity.PaymentOrder;
import com.gym.entity.PrivatePackage;
import com.gym.entity.PrivateSchedule;
import com.gym.entity.ScheduleConflict;
import com.gym.entity.Venue;
import com.gym.service.GymV2Service;
import com.gym.service.MenuPermissionService;
import com.gym.utils.SecurityUtils;
import com.gym.vo.AdminOverviewVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class AdminController {
    private static final String DASHBOARD_PATH = "/dashboard";
    private static final String ANALYTICS_PATH = "/analytics";
    private static final String VENUE_PATH = "/venue";
    private static final String COURSE_PATH = "/course";
    private static final String PRIVATE_SCHEDULE_PATH = "/private-schedule";
    private static final String RESERVATION_PATH = "/reservation";
    private static final String MEMBER_ASSET_PATH = "/member-assets";
    private static final String PAYMENT_ORDER_PATH = "/payment-orders";
    private static final String CHECKIN_PATH = "/checkin-records";
    private static final String COACH_PATH = "/coach";
    private static final String EQUIPMENT_PATH = "/equipment";
    private static final String CONFLICT_PATH = "/schedule-conflicts";

    private final GymV2Service gymV2Service;
    private final MenuPermissionService menuPermissionService;

    public AdminController(GymV2Service gymV2Service, MenuPermissionService menuPermissionService) {
        this.gymV2Service = gymV2Service;
        this.menuPermissionService = menuPermissionService;
    }

    @GetMapping("/overview")
    public Result<AdminOverviewVO> overview() {
        if (!menuPermissionService.hasAnyMenuAction(
                MenuPermissionService.ACTION_VIEW,
                DASHBOARD_PATH,
                ANALYTICS_PATH)) {
            return denied("查看经营总览");
        }
        return Result.success(gymV2Service.getAdminOverview());
    }

    @GetMapping("/analytics")
    public Result<Map<String, Object>> analytics() {
        if (!menuPermissionService.hasMenuAction(ANALYTICS_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看经营分析");
        }
        return Result.success(gymV2Service.getAdminAnalytics());
    }

    @GetMapping("/venues/page")
    public Result<Page<Venue>> pageVenues(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(defaultValue = "") String name) {
        if (!menuPermissionService.hasAnyMenuAction(
                MenuPermissionService.ACTION_VIEW,
                VENUE_PATH,
                COURSE_PATH,
                PRIVATE_SCHEDULE_PATH,
                EQUIPMENT_PATH)) {
            return denied("查看场馆列表");
        }
        return Result.success(gymV2Service.pageVenues(pageNum, pageSize, name));
    }

    @PostMapping("/venues")
    public Result<Boolean> saveVenue(@RequestBody Venue venue) {
        String action = venue.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(VENUE_PATH, action)) {
            return denied("维护场馆资源");
        }
        return Result.success(gymV2Service.saveVenue(venue));
    }

    @DeleteMapping("/venues/{id}")
    public Result<Boolean> deleteVenue(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(VENUE_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除场馆资源");
        }
        return Result.success(gymV2Service.deleteVenue(id));
    }

    @GetMapping("/coaches/page")
    public Result<Page<Coach>> pageCoaches(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(defaultValue = "") String name) {
        if (!menuPermissionService.hasAnyMenuAction(
                MenuPermissionService.ACTION_VIEW,
                COACH_PATH,
                COURSE_PATH,
                PRIVATE_SCHEDULE_PATH,
                MEMBER_ASSET_PATH)) {
            return denied("查看教练列表");
        }
        return Result.success(gymV2Service.pageCoaches(pageNum, pageSize, name));
    }

    @PostMapping("/coaches")
    public Result<Boolean> saveCoach(@RequestBody Coach coach) {
        String action = coach.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(COACH_PATH, action)) {
            return denied("维护教练信息");
        }
        return Result.success(gymV2Service.saveCoach(coach));
    }

    @DeleteMapping("/coaches/{id}")
    public Result<Boolean> deleteCoach(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(COACH_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除教练信息");
        }
        return Result.success(gymV2Service.deleteCoach(id));
    }

    @GetMapping("/course-schedules/page")
    public Result<Page<CourseSchedule>> pageCourses(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(defaultValue = "") String name) {
        if (!menuPermissionService.hasMenuAction(COURSE_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看团课排期");
        }
        return Result.success(gymV2Service.pageCourseSchedules(pageNum, pageSize, name));
    }

    @PostMapping("/course-schedules")
    public Result<Boolean> saveCourse(@RequestBody CourseSchedule schedule) {
        String action = schedule.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(COURSE_PATH, action)) {
            return denied("维护团课排期");
        }
        return Result.success(gymV2Service.saveCourseSchedule(schedule));
    }

    @DeleteMapping("/course-schedules/{id}")
    public Result<Boolean> deleteCourse(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(COURSE_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除团课排期");
        }
        return Result.success(gymV2Service.deleteCourseSchedule(id));
    }

    @GetMapping("/private-schedules/page")
    public Result<Page<PrivateSchedule>> pagePrivateSchedules(@RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestParam(required = false) Long coachId) {
        if (!menuPermissionService.hasMenuAction(PRIVATE_SCHEDULE_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看私教排班");
        }
        return Result.success(gymV2Service.pagePrivateSchedules(pageNum, pageSize, coachId));
    }

    @PostMapping("/private-schedules")
    public Result<Boolean> savePrivateSchedule(@RequestBody PrivateSchedule schedule) {
        String action = schedule.getId() == null ? MenuPermissionService.ACTION_CREATE : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(PRIVATE_SCHEDULE_PATH, action)) {
            return denied("维护私教排班");
        }
        return Result.success(gymV2Service.savePrivateSchedule(schedule));
    }

    @DeleteMapping("/private-schedules/{id}")
    public Result<Boolean> deletePrivateSchedule(@PathVariable Long id) {
        if (!menuPermissionService.hasMenuAction(PRIVATE_SCHEDULE_PATH, MenuPermissionService.ACTION_DELETE)) {
            return denied("删除私教排班");
        }
        return Result.success(gymV2Service.deletePrivateSchedule(id));
    }

    @GetMapping("/bookings/page")
    public Result<Page<BookingOrder>> pageBookings(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(defaultValue = "") String resourceType,
                                                   @RequestParam(defaultValue = "") String status) {
        if (!menuPermissionService.hasMenuAction(RESERVATION_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看预约订单");
        }
        return Result.success(gymV2Service.pageBookings(pageNum, pageSize, resourceType, status));
    }

    @GetMapping("/payments/page")
    public Result<Page<PaymentOrder>> pagePayments(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(defaultValue = "") String paymentType,
                                                   @RequestParam(defaultValue = "") String status) {
        if (!menuPermissionService.hasMenuAction(PAYMENT_ORDER_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看支付订单");
        }
        return Result.success(gymV2Service.pagePayments(pageNum, pageSize, paymentType, status));
    }

    @GetMapping("/member-assets/page")
    public Result<Page<Map<String, Object>>> pageMemberAssets(@RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestParam(defaultValue = "") String keyword) {
        if (!menuPermissionService.hasMenuAction(MEMBER_ASSET_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看会员资产");
        }
        return Result.success(gymV2Service.pageMemberAssets(pageNum, pageSize, keyword));
    }

    @GetMapping("/checkins/page")
    public Result<Page<CheckinRecord>> pageCheckins(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!menuPermissionService.hasMenuAction(CHECKIN_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看签到记录");
        }
        return Result.success(gymV2Service.pageCheckins(pageNum, pageSize));
    }

    @PostMapping("/checkins/consume")
    public Result<Boolean> consumeCheckin(@RequestBody CheckinConsumeRequest request) {
        if (!menuPermissionService.hasMenuAction(CHECKIN_PATH, MenuPermissionService.ACTION_UPDATE)) {
            return denied("核销签到");
        }
        String operatorName = SecurityUtils.getCurrentUsername();
        return Result.success(gymV2Service.consumeCheckinToken(
                request.getToken(),
                operatorName == null || operatorName.isBlank() ? "ADMIN" : operatorName
        ));
    }

    @GetMapping("/conflicts/page")
    public Result<Page<ScheduleConflict>> pageConflicts(@RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!menuPermissionService.hasMenuAction(CONFLICT_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看排期冲突");
        }
        return Result.success(gymV2Service.pageConflicts(pageNum, pageSize));
    }

    @GetMapping("/membership-packages")
    public Result<List<MembershipPackage>> membershipPackages() {
        if (!menuPermissionService.hasMenuAction(MEMBER_ASSET_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看会员套餐");
        }
        return Result.success(gymV2Service.listMembershipPackages());
    }

    @PostMapping("/membership-packages")
    public Result<Boolean> saveMembershipPackage(@RequestBody MembershipPackage membershipPackage) {
        String action = membershipPackage.getId() == null
                ? MenuPermissionService.ACTION_CREATE
                : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(MEMBER_ASSET_PATH, action)) {
            return denied("维护会员套餐");
        }
        return Result.success(gymV2Service.saveMembershipPackage(membershipPackage));
    }

    @GetMapping("/private-packages")
    public Result<List<PrivatePackage>> privatePackages() {
        if (!menuPermissionService.hasMenuAction(MEMBER_ASSET_PATH, MenuPermissionService.ACTION_VIEW)) {
            return denied("查看私教套餐");
        }
        return Result.success(gymV2Service.listPrivatePackages());
    }

    @PostMapping("/private-packages")
    public Result<Boolean> savePrivatePackage(@RequestBody PrivatePackage privatePackage) {
        String action = privatePackage.getId() == null
                ? MenuPermissionService.ACTION_CREATE
                : MenuPermissionService.ACTION_UPDATE;
        if (!menuPermissionService.hasMenuAction(MEMBER_ASSET_PATH, action)) {
            return denied("维护私教套餐");
        }
        return Result.success(gymV2Service.savePrivatePackage(privatePackage));
    }

    private <T> Result<T> denied(String actionLabel) {
        return Result.error("当前角色无权限执行操作: " + actionLabel);
    }
}
