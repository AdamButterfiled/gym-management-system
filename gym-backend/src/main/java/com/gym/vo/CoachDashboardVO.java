package com.gym.vo;

import lombok.Data;

@Data
public class CoachDashboardVO {
    private Long todayLessons;
    private Long pendingApprovals;
    private Long totalStudents;
    private Long todayCheckins;
}
