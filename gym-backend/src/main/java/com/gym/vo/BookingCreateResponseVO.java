package com.gym.vo;

import com.gym.entity.BookingOrder;
import com.gym.entity.PaymentOrder;
import lombok.Data;

@Data
public class BookingCreateResponseVO {
    private BookingOrder booking;
    private PaymentOrder paymentOrder;
    private Boolean checkinEligible;
    private String message;
}
