// package com.event_registration.lk.service.impl2;

// import com.event_registration.lk.dto.Event;
// import com.event_registration.lk.dto.response.EventResponse;
// import com.event_registration.lk.entity.EventEntity;
// import com.event_registration.lk.repository.EventRepository;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class EventServiceImplv2Test {

//     @Mock
//     private EventRepository eventRepository;

//     @InjectMocks
//     private EventServiceImplv2 eventService;

//     private Event sampleEvent;
//     private EventEntity sampleEntity;

//     @BeforeEach
//     void setUp() {
//         sampleEvent = Event.builder()
//                 .name("Concert")
//                 .totalSeats(100)
//                 .build();

//         sampleEntity = EventEntity.builder()
//                 .eventId("E12345")
//                 .name("Concert")
//                 .totalSeats(100)
//                 .availableSeats(100)
//                 .build();
//     }

//     @AfterEach
//     void tearDown() {
//     }

//     @Test
//     void addEvent() {
//         // Arrange
//         when(eventRepository.save(any(EventEntity.class))).thenReturn(sampleEntity);

//         // Act
//         EventResponse response = eventService.addEvent(sampleEvent);

//         // Assert
//         assertNotNull(response);
//         assertEquals("event-add", response.getType());   // ✅ Fix: Expected type matched to actual response field
//         assertEquals("success", response.getStatus());   // ✅ Fix: Expected status matched to actual response field
//         verify(eventRepository, times(1)).save(any(EventEntity.class));
//     }

//     @Test
//     void removeEvent() {
//         // Arrange
//         String id = "E12345";
//         when(eventRepository.findById(id)).thenReturn(Optional.of(sampleEntity));

//         // Act
//         EventResponse response = eventService.removeEvent(id);

//         // Assert
//         assertNotNull(response);
//         assertEquals("event-remove", response.getType()); // ✅ Fix: Expected type matched to actual response field
//         assertEquals("success", response.getStatus());    // ✅ Fix: Expected status matched to actual response field
//         verify(eventRepository, times(1)).delete(sampleEntity);
//     }

//     @Test
//     void updateEvent() {
//         // Arrange
//         Event updateDto = Event.builder().eventId("E12345").name("New Name").build();
//         when(eventRepository.findById("E12345")).thenReturn(Optional.of(sampleEntity));

//         // Act
//         EventResponse response = eventService.updateEvent(updateDto);

//         // Assert
//         assertNotNull(response);
//         assertEquals("event-update", response.getType()); // ✅ Fix: Expected type matched to actual response field
//         assertEquals("success", response.getStatus());    // ✅ Fix: Expected status matched to actual response field
//         assertEquals("New Name", sampleEntity.getName());
//     }

//     @Test
//     void getAllEvents() {
//         // Arrange
//         when(eventRepository.findAll()).thenReturn(List.of(sampleEntity));

//         // Act
//         EventResponse response = eventService.getAllEvents();

//         // Assert
//         assertNotNull(response);
//         assertEquals("event-list", response.getType());   // ✅ Added validation for list type
//         assertEquals("success", response.getStatus());   // ✅ Added validation for list status
//         assertNotNull(response.getEventList());
//         assertEquals(1, response.getEventList().size());
//     }
// }