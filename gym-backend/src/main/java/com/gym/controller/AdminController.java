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
import com.gym.vo.AdminOverviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private GymV2Service gymV2Service;

    @GetMapping("/overview")
    public Result<AdminOverviewVO> overview() {
        return Result.success(gymV2Service.getAdminOverview());
    }

    @GetMapping("/analytics")
    public Result<Map<String, Object>> analytics() {
        return Result.success(gymV2Service.getAdminAnalytics());
    }

    @GetMapping("/venues/page")
    public Result<Page<Venue>> pageVenues(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(defaultValue = "") String name) {
        return Result.success(gymV2Service.pageVenues(pageNum, pageSize, name));
    }

    @PostMapping("/venues")
    public Result<Boolean> saveVenue(@RequestBody Venue venue) {
        return Result.success(gymV2Service.saveVenue(venue));
    }

    @DeleteMapping("/venues/{id}")
    public Result<Boolean> deleteVenue(@PathVariable Long id) {
        return Result.success(gymV2Service.deleteVenue(id));
    }

    @GetMapping("/coaches/page")
    public Result<Page<Coach>> pageCoaches(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(defaultValue = "") String name) {
        return Result.success(gymV2Service.pageCoaches(pageNum, pageSize, name));
    }

    @PostMapping("/coaches")
    public Result<Boolean> saveCoach(@RequestBody Coach coach) {
        return Result.success(gymV2Service.saveCoach(coach));
    }

    @DeleteMapping("/coaches/{id}")
    public Result<Boolean> deleteCoach(@PathVariable Long id) {
        return Result.success(gymV2Service.deleteCoach(id));
    }

    @GetMapping("/course-schedules/page")
    public Result<Page<CourseSchedule>> pageCourses(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(defaultValue = "") String name) {
        return Result.success(gymV2Service.pageCourseSchedules(pageNum, pageSize, name));
    }

    @PostMapping("/course-schedules")
    public Result<Boolean> saveCourse(@RequestBody CourseSchedule schedule) {
        return Result.success(gymV2Service.saveCourseSchedule(schedule));
    }

    @DeleteMapping("/course-schedules/{id}")
    public Result<Boolean> deleteCourse(@PathVariable Long id) {
        return Result.success(gymV2Service.deleteCourseSchedule(id));
    }

    @GetMapping("/private-schedules/page")
    public Result<Page<PrivateSchedule>> pagePrivateSchedules(@RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestParam(required = false) Long coachId) {
        return Result.success(gymV2Service.pagePrivateSchedules(pageNum, pageSize, coachId));
    }

    @PostMapping("/private-schedules")
    public Result<Boolean> savePrivateSchedule(@RequestBody PrivateSchedule schedule) {
        return Result.success(gymV2Service.savePrivateSchedule(schedule));
    }

    @DeleteMapping("/private-schedules/{id}")
    public Result<Boolean> deletePrivateSchedule(@PathVariable Long id) {
        return Result.success(gymV2Service.deletePrivateSchedule(id));
    }

    @GetMapping("/bookings/page")
    public Result<Page<BookingOrder>> pageBookings(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(defaultValue = "") String resourceType,
                                                   @RequestParam(defaultValue = "") String status) {
        return Result.success(gymV2Service.pageBookings(pageNum, pageSize, resourceType, status));
    }

    @GetMapping("/payments/page")
    public Result<Page<PaymentOrder>> pagePayments(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(defaultValue = "") String paymentType,
                                                   @RequestParam(defaultValue = "") String status) {
        return Result.success(gymV2Service.pagePayments(pageNum, pageSize, paymentType, status));
    }

    @GetMapping("/member-assets/page")
    public Result<Page<Map<String, Object>>> pageMemberAssets(@RequestParam(defaultValue = "1") Integer pageNum,
                                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                                              @RequestParam(defaultValue = "") String keyword) {
        return Result.success(gymV2Service.pageMemberAssets(pageNum, pageSize, keyword));
    }

    @GetMapping("/checkins/page")
    public Result<Page<CheckinRecord>> pageCheckins(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(gymV2Service.pageCheckins(pageNum, pageSize));
    }

    @PostMapping("/checkins/consume")
    public Result<Boolean> consumeCheckin(@RequestBody CheckinConsumeRequest request) {
        return Result.success(gymV2Service.consumeCheckinToken(request.getToken(), "ADMIN"));
    }

    @GetMapping("/conflicts/page")
    public Result<Page<ScheduleConflict>> pageConflicts(@RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(gymV2Service.pageConflicts(pageNum, pageSize));
    }

    @GetMapping("/membership-packages")
    public Result<List<MembershipPackage>> membershipPackages() {
        return Result.success(gymV2Service.listMembershipPackages());
    }

    @PostMapping("/membership-packages")
    public Result<Boolean> saveMembershipPackage(@RequestBody MembershipPackage membershipPackage) {
        return Result.success(gymV2Service.saveMembershipPackage(membershipPackage));
    }

    @GetMapping("/private-packages")
    public Result<List<PrivatePackage>> privatePackages() {
        return Result.success(gymV2Service.listPrivatePackages());
    }

    @PostMapping("/private-packages")
    public Result<Boolean> savePrivatePackage(@RequestBody PrivatePackage privatePackage) {
        return Result.success(gymV2Service.savePrivatePackage(privatePackage));
    }
}
