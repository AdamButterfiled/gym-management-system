package com.gym.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.BookingCreateRequest;
import com.gym.dto.PaymentCreateRequest;
import com.gym.entity.BodyMetric;
import com.gym.entity.BookingOrder;
import com.gym.entity.CheckinRecord;
import com.gym.entity.Coach;
import com.gym.entity.CourseSchedule;
import com.gym.entity.MembershipPackage;
import com.gym.entity.PaymentOrder;
import com.gym.entity.PrivatePackage;
import com.gym.entity.PrivateSchedule;
import com.gym.entity.ScheduleConflict;
import com.gym.entity.TrainingLog;
import com.gym.entity.Venue;
import com.gym.vo.AdminOverviewVO;
import com.gym.vo.BookingCreateResponseVO;
import com.gym.vo.CheckinTokenVO;
import com.gym.vo.CoachDashboardVO;
import com.gym.vo.MemberHomeVO;

import java.util.List;
import java.util.Map;

public interface GymV2Service {
    AdminOverviewVO getAdminOverview();

    Map<String, Object> getAdminAnalytics();

    Page<Venue> pageVenues(Integer pageNum, Integer pageSize, String name);

    boolean saveVenue(Venue venue);

    boolean deleteVenue(Long id);

    Page<Coach> pageCoaches(Integer pageNum, Integer pageSize, String name);

    boolean saveCoach(Coach coach);

    boolean deleteCoach(Long id);

    Page<CourseSchedule> pageCourseSchedules(Integer pageNum, Integer pageSize, String name);

    boolean saveCourseSchedule(CourseSchedule schedule);

    boolean deleteCourseSchedule(Long id);

    Page<PrivateSchedule> pagePrivateSchedules(Integer pageNum, Integer pageSize, Long coachId);

    boolean savePrivateSchedule(PrivateSchedule schedule);

    boolean deletePrivateSchedule(Long id);

    Page<BookingOrder> pageBookings(Integer pageNum, Integer pageSize, String resourceType, String status);

    Page<PaymentOrder> pagePayments(Integer pageNum, Integer pageSize, String paymentType, String status);

    Page<CheckinRecord> pageCheckins(Integer pageNum, Integer pageSize);

    Page<ScheduleConflict> pageConflicts(Integer pageNum, Integer pageSize);

    Page<Map<String, Object>> pageMemberAssets(Integer pageNum, Integer pageSize, String keyword);

    List<MembershipPackage> listMembershipPackages();

    boolean saveMembershipPackage(MembershipPackage membershipPackage);

    List<PrivatePackage> listPrivatePackages();

    boolean savePrivatePackage(PrivatePackage privatePackage);

    MemberHomeVO getMemberHome();

    List<Venue> listMemberVenues();

    List<Map<String, Object>> listMemberCourses();

    List<Map<String, Object>> listMemberCoaches();

    List<BookingOrder> listMemberBookings();

    List<PaymentOrder> listMemberPayments();

    BookingCreateResponseVO createBooking(BookingCreateRequest request);

    boolean cancelBooking(Long bookingId);

    PaymentOrder createPayment(PaymentCreateRequest request);

    PaymentOrder payOrder(String paymentNo);

    CheckinTokenVO createCheckinToken(Long bookingId);

    boolean consumeCheckinToken(String token, String operatorName);

    CoachDashboardVO getCoachDashboard();

    List<BookingOrder> listCoachPendingBookings();

    boolean reviewCoachBooking(Long bookingId, boolean approved);

    List<BodyMetric> listBodyMetrics(Long userId);

    boolean saveBodyMetric(BodyMetric bodyMetric);

    List<TrainingLog> listTrainingLogs(Long userId);

    boolean saveTrainingLog(TrainingLog trainingLog);
}
