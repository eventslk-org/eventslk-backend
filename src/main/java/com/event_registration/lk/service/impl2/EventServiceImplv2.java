package com.event_registration.lk.service.impl2;

import com.event_registration.lk.config.CacheConfig;
import com.event_registration.lk.dto.Event;
import com.event_registration.lk.dto.PriceRange;
import com.event_registration.lk.dto.response.EventResponse;
import com.event_registration.lk.entity.EventEntity;
import com.event_registration.lk.repository.EventRepository;
import com.event_registration.lk.service.EventService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Primary EventService implementation.
 *
 * Key improvements over EventServiceImpl (v1):
 *  - image field replaced by imageUrl (S3 URL supplied by the client after direct upload).
 *  - updateEvent uses JPA dirty checking (fetch → mutate → auto-flush) instead of delete + re-insert,
 *    which previously wiped @Version and caused FK cascade issues.
 *  - Manual DTO ↔ entity mapping avoids ObjectMapper pitfalls with new seat/version fields.
 *  - totalSeats / availableSeats initialised on create; seat values are never overwritten by update.
 */
@Slf4j
@Service
@Primary
public class EventServiceImplv2 implements EventService {

    private final EventRepository eventRepository;

    public EventServiceImplv2(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.EVENTS_CACHE, allEntries = true)
    public EventResponse addEvent(Event event) {
        try {
            EventEntity entity = EventEntity.builder()
                    .eventId(generateEventId())
                    .name(event.getName())
                    .description(event.getDescription())
                    .dates(event.getDates())
                    .priceRanges(event.getPriceRanges())
                    .location(event.getLocation())
                    .imageUrl(event.getImageUrl())
                    .totalSeats(event.getTotalSeats())
                    .availableSeats(event.getTotalSeats())   // start fully available
                    .build();

            eventRepository.save(entity);
            log.info("[event-add] created eventId={} seats={}", entity.getEventId(), entity.getTotalSeats());
            return new EventResponse("event-add", "success");
        } catch (Exception e) {
            log.error("[event-add] failed: {}", e.getMessage());
            return new EventResponse("event-add", "failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.EVENTS_CACHE, allEntries = true)
    public EventResponse removeEvent(String id) {
        return eventRepository.findById(id)
                .map(entity -> {
                    eventRepository.delete(entity);
                    log.info("[event-remove] deleted eventId={}", id);
                    return new EventResponse("event-remove", "success");
                })
                .orElseGet(() -> {
                    log.warn("[event-remove] not found eventId={}", id);
                    return new EventResponse("event-remove", "event not exists");
                });
    }

    /**
     * Updates mutable event metadata via JPA dirty checking.
     * availableSeats and totalSeats are intentionally NOT overwritten here —
     * seat counts are managed atomically through the booking flow only.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.EVENTS_CACHE, allEntries = true)
    public EventResponse updateEvent(Event event) {
        return eventRepository.findById(event.getEventId())
                .map(entity -> {
                    entity.setName(event.getName());
                    entity.setDescription(event.getDescription());
                    entity.setDates(event.getDates());
                    entity.setLocation(event.getLocation());
                    entity.setImageUrl(event.getImageUrl());
                    entity.setPriceRanges(event.getPriceRanges());
                    // Hibernate dirty-checks and issues a targeted UPDATE on transaction commit.
                    log.info("[event-update] updated eventId={}", entity.getEventId());
                    return new EventResponse("event-update", "success");
                })
                .orElseGet(() -> {
                    log.warn("[event-update] not found eventId={}", event.getEventId());
                    return new EventResponse("event-update", "event not exists");
                });
    }

    @Override
    @Transactional
    @Cacheable(cacheNames = CacheConfig.EVENTS_CACHE, key = "'all'")
    public EventResponse getAllEvents() {
        log.info("[event-list] fetching all events");
        ArrayList<Event> eventList = new ArrayList<>();
        eventRepository.findAll().forEach(entity -> eventList.add(toDto(entity)));
        return new EventResponse("event-list", "success", eventList);
    }

    @Override
    @Transactional
    @Cacheable(cacheNames = CacheConfig.EVENTS_CACHE, key = "#id", unless = "#result.eventList == null")
    public EventResponse getEventById(String id) {
        return eventRepository.findById(id)
                .map(entity -> {
                    ArrayList<Event> eventList = new ArrayList<>();
                    eventList.add(toDto(entity));
                    return new EventResponse("event-details", "success", eventList);
                })
                .orElseGet(() -> {
                    log.warn("[event-details] not found eventId={}", id);
                    return new EventResponse("event-details", "event not exists");
                });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Event toDto(EventEntity e) {
        return Event.builder()
                .eventId(e.getEventId())
                .name(e.getName())
                .description(e.getDescription())
                .dates(e.getDates())
                .location(e.getLocation())
                .imageUrl(e.getImageUrl())
                .priceRanges(e.getPriceRanges() != null ? new ArrayList<>(e.getPriceRanges()) : new ArrayList<>())
                .totalSeats(e.getTotalSeats())
                .availableSeats(e.getAvailableSeats())
                .build();
    }

    private String generateEventId() {
        return "E" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }
}
