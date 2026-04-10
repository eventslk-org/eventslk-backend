package com.event_registration.lk.dto.response;

import com.event_registration.lk.dto.BookingOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
@AllArgsConstructor
public class BookingResponse {
    private String status;
    private String message;
    ArrayList<BookingOrder> orderList;

    public BookingResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

}
