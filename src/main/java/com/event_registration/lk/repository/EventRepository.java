package com.event_registration.lk.repository;

import com.event_registration.lk.entity.EventEntity;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<EventEntity,String> {
    EventEntity findEventEntityByEventIdContainingIgnoreCase(String eventId);
    Boolean existsEventEntityByEventIdContainingIgnoreCase(String eventId);
}
