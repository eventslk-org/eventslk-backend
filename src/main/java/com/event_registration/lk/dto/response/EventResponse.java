package com.event_registration.lk.dto.response;

import com.event_registration.lk.dto.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class EventResponse {
    private String status;
    private String message;
    private ArrayList<Event> eventList;

    public EventResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public EventResponse(String status, ArrayList<Event> eventList) {
        this.status = status;
        this.eventList = eventList;
    }
}
