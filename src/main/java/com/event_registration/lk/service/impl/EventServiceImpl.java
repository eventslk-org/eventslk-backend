package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.Event;
import com.event_registration.lk.dto.response.EventResponse;
import com.event_registration.lk.entity.EventEntity;
import com.event_registration.lk.repository.EventRepository;
import com.event_registration.lk.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Service
// @Primary //main service for event handling
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    ObjectMapper objectMapper;

    public EventServiceImpl(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    //Done
    @Override
    public EventResponse addEvent(Event event) {

        try{
            eventRepository.save(
                    EventEntity.builder()
                            .eventId(generateEventId())
                            .name(event.getName())
                            .description(event.getDescription())
                            .dates(event.getDates())
                            .priceRanges(event.getPriceRanges())
                            .location(event.getLocation())
                            // .image(event.getImage())
                            .imageUrl(null)
                            .build()
            );
            return new EventResponse("event-add","success");
        }catch (Exception e){
            return new EventResponse("event-add","event already exists");
        }
    }
    //Done
    @Override
    public EventResponse removeEvent(String id) {
        Boolean result = eventRepository.existsEventEntityByEventIdContainingIgnoreCase(id);
        if (result == true){
            eventRepository.delete(eventRepository.findEventEntityByEventIdContainingIgnoreCase(id));
            return new EventResponse("event-remove","success");
        }
        return new EventResponse("event-remove","event not exists");

    }
    //Done
    @Override
    public EventResponse updateEvent(Event event) {
        Boolean result = eventRepository.existsEventEntityByEventIdContainingIgnoreCase(event.getEventId());
        if (result == true){
            eventRepository.delete(eventRepository.findEventEntityByEventIdContainingIgnoreCase(event.getEventId()));
            eventRepository.save(
                    EventEntity.builder()
                            .eventId(event.getEventId())
                            .name(event.getName())
                            .description(event.getDescription())
                            .dates(event.getDates())
                            .location(event.getLocation())
                            //.image(event.getImage())
                            .imageUrl(null)
                            .priceRanges(event.getPriceRanges())
                            .build()
            );
            return new EventResponse("event-update","success");
        }
        return new EventResponse("event-update","event not exists");
    }
    //Done
    @Override
    public EventResponse getAllEvents() {
        log.info("get-all event service layer");
        ArrayList<Event> eventList = new ArrayList<>();

        Iterable<EventEntity> eventEntityIterable = eventRepository.findAll();
        Iterator<EventEntity> eventEntityIterator = eventEntityIterable.iterator();
        while (eventEntityIterator.hasNext()){
            Event event = objectMapper.convertValue(eventEntityIterator.next(), Event.class);
            eventList.add(event);
        }
        return new EventResponse("event-list","success",eventList);
    }

    private String generateEventId() {
        String prefix = "E";
        String uniquePart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 9)
                .toUpperCase();

        return prefix + uniquePart;
    }

}
