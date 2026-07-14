package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.Event;
import com.event_registration.lk.dto.PriceRange;
import com.event_registration.lk.dto.response.EventResponse;
import com.event_registration.lk.entity.EventEntity;
import com.event_registration.lk.repository.EventRepository;
import com.event_registration.lk.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import java.time.LocalDateTime;

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
                            .totalSeats(event.getTotalSeats())
                            .availableSeats(event.getTotalSeats())
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

    @Override
    public EventResponse getEventById(String id) {
        return eventRepository.findById(id)
                .map(entity -> {
                    ArrayList<Event> eventList = new ArrayList<>();
                    eventList.add(objectMapper.convertValue(entity, Event.class));
                    return new EventResponse("event-details", "success", eventList);
                })
                .orElseGet(() -> new EventResponse("event-details", "event not exists"));
    }

    private String generateEventId() {
        String prefix = "E";
        String uniquePart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 9)
                .toUpperCase();

        return prefix + uniquePart;
    }

    @PostConstruct
    private void seedMultipleEvents() {
        // Seed only an empty catalogue: without this guard every restart
        // re-inserts the same 10 events under fresh random IDs.
        if (eventRepository.count() > 0) {
            log.info("Event seed skipped: catalogue already has data.");
            return;
        }
        // Reusable price tiers
        List<PriceRange> standardPrices = List.of(
                new PriceRange("Lecturer", 15000.0),
                new PriceRange("Postgraduate", 7000.0),
                new PriceRange("Undergraduate", 3000.0)
        );

        List<PriceRange> premiumPrices = List.of(
                new PriceRange("Professional", 25000.0),
                new PriceRange("Student", 5000.0)
        );

        List<Event> initialEvents = List.of(
                createEventDto(
                        "Enterprise Software Design",
                        "Introduction to enterprise software design with Spring Boot",
                        standardPrices,
                        List.of("2025-09-22T09:00:00", "2025-09-22T16:00:00"),
                        "A7-406 University of Kelaniya",
                        100
                ),
                createEventDto(
                        "Advanced Machine Learning",
                        "Deep dive into neural networks and predictive modeling",
                        premiumPrices,
                        List.of("2025-10-05T10:00:00", "2025-10-06T17:00:00"),
                        "Main Auditorium, University of Colombo",
                        250
                ),
                createEventDto(
                        "Cloud Computing with AWS",
                        "Hands-on workshop on deploying scalable cloud architecture",
                        standardPrices,
                        List.of("2025-10-15T08:30:00", "2025-10-15T15:30:00"),
                        "Lab 3, SLIIT Campus",
                        50
                ),
                createEventDto(
                        "Full Stack Web Development",
                        "Building modern web apps with React and Node.js",
                        standardPrices,
                        List.of("2025-11-01T09:00:00", "2025-11-02T16:00:00"),
                        "A7-406 University of Kelaniya",
                        120
                ),
                createEventDto(
                        "Cybersecurity Fundamentals",
                        "Essential practices for securing enterprise data",
                        premiumPrices,
                        List.of("2025-11-12T13:00:00", "2025-11-12T18:00:00"),
                        "Virtual Event (Zoom)",
                        500
                ),
                createEventDto(
                        "Data Science & Analytics",
                        "Extracting business value from big data using Python",
                        standardPrices,
                        List.of("2025-11-20T09:00:00", "2025-11-20T17:00:00"),
                        "B-Block, University of Moratuwa",
                        80
                ),
                createEventDto(
                        "Mobile App Dev with Flutter",
                        "Cross-platform mobile development from scratch",
                        standardPrices,
                        List.of("2025-12-05T09:00:00", "2025-12-05T16:00:00"),
                        "Lab 1, IIT Campus",
                        60
                ),
                createEventDto(
                        "UI/UX Design Masterclass",
                        "Creating user-centric interfaces with Figma",
                        standardPrices,
                        List.of("2025-12-10T10:00:00", "2025-12-10T15:00:00"),
                        "Design Studio, NSBM Green University",
                        40
                ),
                createEventDto(
                        "Blockchain & Web3 Seminar",
                        "Understanding decentralized applications and smart contracts",
                        premiumPrices,
                        List.of("2026-01-15T14:00:00", "2026-01-15T19:00:00"),
                        "BMICH, Colombo",
                        300
                ),
                createEventDto(
                        "DevOps & CI/CD Pipelines",
                        "Automating deployments using Docker, Kubernetes, and Jenkins",
                        standardPrices,
                        List.of("2026-02-05T09:00:00", "2026-02-06T17:00:00"),
                        "A7-406 University of Kelaniya",
                        100
                )
        );

        // Loop through the list and save each one
        for (Event event : initialEvents) {
            addEvent(event);
        }

        System.out.println("Successfully seeded 10 initial events.");
    }

    // Helper method converts the String dates to LocalDateTime automatically
    private Event createEventDto(String name, String description, List<PriceRange> priceRanges,
                                 List<String> dateStrings, String location, int totalSeats) {
        
        ArrayList<LocalDateTime> parsedDates = new ArrayList<>();
        for (String dateStr : dateStrings) {
            parsedDates.add(LocalDateTime.parse(dateStr));
        }

        return Event.builder()
                .name(name)
                .description(description)
                .priceRanges(priceRanges)
                .dates(parsedDates)
                .location(location)
                .totalSeats(totalSeats)
                .availableSeats(totalSeats) // Sets available seats to equal total seats initially
                .build();
    }
}
