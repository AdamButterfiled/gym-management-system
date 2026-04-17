package com.gym.vo;

import com.gym.entity.BookingOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MemberHomeVO {
    private BigDecimal balance;
    private String activeMembershipName;
    private Integer remainingPrivateSessions;
    private List<BookingOrder> upcomingBookings;
}
