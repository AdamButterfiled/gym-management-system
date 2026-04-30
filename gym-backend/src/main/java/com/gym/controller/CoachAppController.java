package com.gym.controller;

import com.gym.common.Result;
import com.gym.entity.BodyMetric;
import com.gym.entity.BookingOrder;
import com.gym.entity.TrainingLog;
import com.gym.service.GymV2Service;
import com.gym.vo.CoachDashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coach")
@CrossOrigin
@PreAuthorize("hasAuthority('COACH')")
public class CoachAppController {

    @Autowired
    private GymV2Service gymV2Service;

    @GetMapping("/home")
    public Result<CoachDashboardVO> home() {
        return Result.success(gymV2Service.getCoachDashboard());
    }

    @GetMapping("/bookings/pending")
    public Result<List<BookingOrder>> pendingBookings() {
        return Result.success(gymV2Service.listCoachPendingBookings());
    }

    @PostMapping("/bookings/{id}/approve")
    public Result<Boolean> approve(@PathVariable Long id) {
        return Result.success(gymV2Service.reviewCoachBooking(id, true));
    }

    @PostMapping("/bookings/{id}/reject")
    public Result<Boolean> reject(@PathVariable Long id) {
        return Result.success(gymV2Service.reviewCoachBooking(id, false));
    }

    @GetMapping("/body-metrics")
    public Result<List<BodyMetric>> bodyMetrics(@RequestParam(required = false) Long userId) {
        return Result.success(gymV2Service.listBodyMetrics(userId));
    }

    @PostMapping("/body-metrics")
    public Result<Boolean> saveBodyMetric(@RequestBody BodyMetric bodyMetric) {
        return Result.success(gymV2Service.saveBodyMetric(bodyMetric));
    }

    @GetMapping("/training-logs")
    public Result<List<TrainingLog>> trainingLogs(@RequestParam(required = false) Long userId) {
        return Result.success(gymV2Service.listTrainingLogs(userId));
    }

    @PostMapping("/training-logs")
    public Result<Boolean> saveTrainingLog(@RequestBody TrainingLog trainingLog) {
        return Result.success(gymV2Service.saveTrainingLog(trainingLog));
    }
}
