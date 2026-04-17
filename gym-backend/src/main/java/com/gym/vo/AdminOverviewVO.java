package com.gym.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOverviewVO {
    private Long memberCount;
    private Long coachCount;
    private Long todayBookings;
    private Long pendingCoachApprovals;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private Double venueUsageRate;
    private Double courseAttendanceRate;
    private Double checkinRate;
}
