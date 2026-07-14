package com.event_registration.lk.service;

import com.event_registration.lk.dto.Event;
import com.event_registration.lk.dto.response.EventResponse;

import java.util.ArrayList;

public interface EventService {

    public EventResponse addEvent(Event event);
    public EventResponse removeEvent(String id);
    public EventResponse updateEvent(Event event);
    public EventResponse getAllEvents();
    public EventResponse getEventById(String id);

}
