package com.gym.controller;

import com.gym.common.Result;
import com.gym.dto.BookingCreateRequest;
import com.gym.dto.CheckinConsumeRequest;
import com.gym.dto.PaymentCreateRequest;
import com.gym.entity.BookingOrder;
import com.gym.entity.PaymentOrder;
import com.gym.entity.Venue;
import com.gym.service.GymV2Service;
import com.gym.vo.BookingCreateResponseVO;
import com.gym.vo.CheckinTokenVO;
import com.gym.vo.MemberHomeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/member")
@CrossOrigin
public class MemberController {

    @Autowired
    private GymV2Service gymV2Service;

    @GetMapping("/home")
    public Result<MemberHomeVO> home() {
        return Result.success(gymV2Service.getMemberHome());
    }

    @GetMapping("/venues")
    public Result<List<Venue>> venues() {
        return Result.success(gymV2Service.listMemberVenues());
    }

    @GetMapping("/group-courses")
    public Result<List<Map<String, Object>>> groupCourses() {
        return Result.success(gymV2Service.listMemberCourses());
    }

    @GetMapping("/coaches")
    public Result<List<Map<String, Object>>> coaches() {
        return Result.success(gymV2Service.listMemberCoaches());
    }

    @GetMapping("/bookings")
    public Result<List<BookingOrder>> bookings() {
        return Result.success(gymV2Service.listMemberBookings());
    }

    @PostMapping("/bookings")
    public Result<BookingCreateResponseVO> createBooking(@RequestBody BookingCreateRequest request) {
        return Result.success(gymV2Service.createBooking(request));
    }

    @PostMapping("/bookings/{id}/cancel")
    public Result<Boolean> cancelBooking(@PathVariable Long id) {
        return Result.success(gymV2Service.cancelBooking(id));
    }

    @GetMapping("/payments")
    public Result<List<PaymentOrder>> payments() {
        return Result.success(gymV2Service.listMemberPayments());
    }

    @PostMapping("/payments")
    public Result<PaymentOrder> createPayment(@RequestBody PaymentCreateRequest request) {
        return Result.success(gymV2Service.createPayment(request));
    }

    @PostMapping("/payments/{paymentNo}/pay")
    public Result<PaymentOrder> pay(@PathVariable String paymentNo) {
        return Result.success(gymV2Service.payOrder(paymentNo));
    }

    @GetMapping("/membership-packages")
    public Result<?> membershipPackages() {
        return Result.success(gymV2Service.listMembershipPackages());
    }

    @GetMapping("/private-packages")
    public Result<?> privatePackages() {
        return Result.success(gymV2Service.listPrivatePackages());
    }

    @GetMapping("/checkin/{bookingId}")
    public Result<CheckinTokenVO> checkinToken(@PathVariable Long bookingId) {
        return Result.success(gymV2Service.createCheckinToken(bookingId));
    }

    @PostMapping("/checkin/consume")
    public Result<Boolean> consumeCheckin(@RequestBody CheckinConsumeRequest request) {
        return Result.success(gymV2Service.consumeCheckinToken(request.getToken(), "SELF"));
    }
}
