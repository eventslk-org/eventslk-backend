package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.BookingOrder;
import com.event_registration.lk.dto.request.BookingRequest;
import com.event_registration.lk.dto.response.BookingResponse;
import com.event_registration.lk.entity.BookingOrderEntity;
import com.event_registration.lk.repository.BookingRepository;
import com.event_registration.lk.repository.EventRepository;
import com.event_registration.lk.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// ──────────────────────────────────────────────────────────────────────────────
// @ExtendWith(MockitoExtension.class)
//   → JUnit 5 සමඟ Mockito use කරන්න මේ annotation ඕනෑ.
//   → මේ නැතිව @Mock, @InjectMocks වැඩ කරන්නේ නෑ.
// ──────────────────────────────────────────────────────────────────────────────
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    // ──────────────────────────────────────────────────────────────────────────
    // @Mock → Real database / real object වෙනුවට FAKE object හදනවා.
    //
    // ඇයි fake? → BookingServiceImpl test කරන්නේ service logic.
    //   Real DB connect වුනොත්:
    //     - Tests slow වෙනවා
    //     - MySQL running නොතිබ්බොත් fail වෙනවා
    //     - Test data conflict වෙනවා
    //   Fake (Mock) use කළොත්:
    //     - Super fast
    //     - DB නැතිවත් run වෙනවා
    //     - ඕනෑ behavior define කරන්න පුළුවන්
    // ──────────────────────────────────────────────────────────────────────────
    @Mock
    private BookingRepository bookingRepository;  // Fake BookingRepository

    @Mock
    private EventRepository eventRepository;      // Fake EventRepository

    @Mock
    private UserRepository userRepository;        // Fake UserRepository

    @Mock
    private ObjectMapper objectMapper;            // Fake ObjectMapper (BookingOrder → Entity convert කරන්න)

    // ──────────────────────────────────────────────────────────────────────────
    // @InjectMocks → ඉහත @Mock හදපු fake objects, මේ class ට inject කරනවා.
    //   එනම්: BookingServiceImpl හි constructor ට
    //          (bookingRepository, eventRepository, userRepository, objectMapper)
    //          Mockito automatically fake objects දානවා.
    // ──────────────────────────────────────────────────────────────────────────
    @InjectMocks
    private BookingServiceImpl bookingService;    // Real service — fake dependencies සමඟ

    // ──────────────────────────────────────────────────────────────────────────
    // Test Data — සියලු tests වලට common data variables
    // ──────────────────────────────────────────────────────────────────────────
    private BookingRequest validBookingRequest;
    private BookingOrderEntity sampleBookingEntity;

    // ──────────────────────────────────────────────────────────────────────────
    // @BeforeEach → හැම @Test method එකට කලින් automatically run වෙනවා.
    //   Use: test data prepare කිරීමට.
    // ──────────────────────────────────────────────────────────────────────────
    @BeforeEach
    void setUp() {
        // Valid booking request object හදනවා — tests වලදී reuse කරනවා
        validBookingRequest = BookingRequest.builder()
                .eventId("E12345678A")
                .userId(1L)
                .localDateTime(LocalDateTime.now())
                .build();

        // Sample booking entity — getUserBookingDetails test සඳහා
        sampleBookingEntity = BookingOrderEntity.builder()
                .bookingId("B12345678A")
                .userId(1L)
                .eventId("E12345678A")
                .ticketNumber("TKT0000012300001052025")
                .orderedDate(LocalDateTime.now())
                .orderStatus("confirmed")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // @AfterEach → හැම @Test method එකෙන් පස්සේ automatically run වෙනවා.
    //   Use: cleanup / reset කිරීමට.
    //   Mockito use කරනවිට සාමාන්‍යයෙන් empty ලෙස තබා ගන්න පුළුවන්.
    // ──────────────────────────────────────────────────────────────────────────
    @AfterEach
    void tearDown() {
        // Mockito automatically reset කරනවා — extra cleanup ඕනෑ නෑ
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  bookEvent() METHOD TESTS
    //  BookingServiceImpl.java → bookEvent() method එකේ logic:
    //    1. Event exists check කිරීම
    //    2. User exists check කිරීම
    //    3. BookingOrder object හදා save කිරීම
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("bookEvent | Event සහ User දෙකම ඇති නම් → booking success විය යුතුයි")
    void bookEvent_shouldReturnSuccess_whenEventAndUserExist() {

        // ── ARRANGE (සූදානම් කිරීම) ──────────────────────────────────────────
        // Fake eventRepository ට කියනවා:
        //   "existsEventEntityByEventIdContainingIgnoreCase('E12345678A') call කළොත් true return කරන්න"
        //   → Event exist කරනවා කියා simulate කිරීම
        when(eventRepository.existsEventEntityByEventIdContainingIgnoreCase("E12345678A"))
                .thenReturn(true);

        // Fake userRepository ට කියනවා:
        //   "existsByUserId(1L) call කළොත් true return කරන්න"
        //   → User exist කරනවා කියා simulate කිරීම
        when(userRepository.existsByUserId(1L))
                .thenReturn(true);

        // Fake objectMapper ට කියනවා:
        //   "convertValue() call කළොත් sample entity return කරන්න"
        //   → BookingOrder DTO → BookingOrderEntity convert simulate කිරීම
        when(objectMapper.convertValue(any(BookingOrder.class), eq(BookingOrderEntity.class)))
                .thenReturn(sampleBookingEntity);

        // Fake bookingRepository ට countByOrderedDateBetween mock කිරීම
        //   → generateTicketNumber() method ඇතුළෙදී call වෙනවා
        when(bookingRepository.countByOrderedDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);

        // ── ACT (ක්‍රියාව) ────────────────────────────────────────────────────
        // Real bookEvent() method call කිරීම — fake objects inject කළා නිසා
        // DB access නෑ, ඒත් real logic run වෙනවා
        BookingResponse response = bookingService.bookEvent(validBookingRequest);

        // ── ASSERT (verify කිරීම) ─────────────────────────────────────────────
        // Response null නොවිය යුතුයි
        assertNotNull(response, "Response null නොවිය යුතුයි");

        // Status "booking" විය යුතුයි
        assertEquals("booking", response.getStatus(), "Status 'booking' විය යුතුයි");

        // Message "success" විය යුතුයි
        assertEquals("success", response.getMessage(), "Message 'success' විය යුතුයි");

        // OrderList empty නොවිය යුතුයි — booking order list ඇති විය යුතුයි
        assertNotNull(response.getOrderList(), "OrderList null නොවිය යුතුයි");
        assertFalse(response.getOrderList().isEmpty(), "OrderList empty නොවිය යුතුයි");

        // Verify: bookingRepository.save() හරියටම 1 වතාවක් call විය යුතුයි
        verify(bookingRepository, times(1)).save(any(BookingOrderEntity.class));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("bookEvent | Event නොමැති නම් → 'event not found' return විය යුතුයි")
    void bookEvent_shouldReturnEventNotFound_whenEventDoesNotExist() {

        // ── ARRANGE ──────────────────────────────────────────────────────────
        // Event exist නෑ කියා simulate කිරීම → false return
        when(eventRepository.existsEventEntityByEventIdContainingIgnoreCase("E12345678A"))
                .thenReturn(false);

        // ── ACT ───────────────────────────────────────────────────────────────
        BookingResponse response = bookingService.bookEvent(validBookingRequest);

        // ── ASSERT ────────────────────────────────────────────────────────────
        assertNotNull(response);
        assertEquals("booking", response.getStatus());

        // Message "unsuccess : event not found" contain කරන්නේද?
        assertTrue(response.getMessage().contains("event not found"),
                "Message 'event not found' contain කරන්නේ නෑ: " + response.getMessage());

        // CRITICAL: Event නෑ නම් save() කිසිදාකවත් call නොවිය යුතුයි!
        verify(bookingRepository, never()).save(any());

        // User check ත් කිරීම ඕනෑ නෑ — event fail වූ නිසා
        verify(userRepository, never()).existsByUserId(any());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("bookEvent | User නොමැති නම් → 'user not found' return විය යුතුයි")
    void bookEvent_shouldReturnUserNotFound_whenUserDoesNotExist() {

        // ── ARRANGE ──────────────────────────────────────────────────────────
        // Event ඇති, User නෑ → simulate කිරීම
        when(eventRepository.existsEventEntityByEventIdContainingIgnoreCase("E12345678A"))
                .thenReturn(true);  // Event ඇත

        when(userRepository.existsByUserId(1L))
                .thenReturn(false); // User නෑ

        // ── ACT ───────────────────────────────────────────────────────────────
        BookingResponse response = bookingService.bookEvent(validBookingRequest);

        // ── ASSERT ────────────────────────────────────────────────────────────
        assertNotNull(response);
        assertEquals("booking", response.getStatus());

        // Message "unsuccess : user not found" contain කරන්නේද?
        assertTrue(response.getMessage().contains("user not found"),
                "Message 'user not found' contain කරන්නේ නෑ: " + response.getMessage());

        // User නෑ නිසා save() call නොවිය යුතුයි
        verify(bookingRepository, never()).save(any());
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  cancelBooking() METHOD TESTS
    //  BookingServiceImpl.java → cancelBooking() method logic:
    //    1. Booking ID exist කරනවාද check
    //    2. Exist නෑ නම් → error message
    //    3. Exist නම් → deleteById() call කිරීම
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("cancelBooking | Booking ID ඇති නම් → delete වී success return විය යුතුයි")
    void cancelBooking_shouldDeleteAndReturnSuccess_whenBookingExists() {

        // ── ARRANGE ──────────────────────────────────────────────────────────
        // Booking "B12345678A" exist කරනවා කියා simulate
        when(bookingRepository.existsByBookingId("B12345678A"))
                .thenReturn(true);

        // deleteById() → void method, doNothing() use කිරීම
        // (Mockito void methods mock කරන්නේ doNothing() වලින්)
        doNothing().when(bookingRepository).deleteById("B12345678A");

        // ── ACT ───────────────────────────────────────────────────────────────
        BookingResponse response = bookingService.cancelBooking("B12345678A");

        // ── ASSERT ────────────────────────────────────────────────────────────
        assertNotNull(response);
        assertEquals("booking-cancel", response.getStatus());
        assertEquals("success", response.getMessage());

        // Verify: deleteById() හරියටම 1 වතාවක් "B12345678A" සමඟ call විය යුතුයි
        verify(bookingRepository, times(1)).deleteById("B12345678A");
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelBooking | Booking ID නොමැති නම් → 'no record exists' return විය යුතුයි")
    void cancelBooking_shouldReturnNoRecord_whenBookingDoesNotExist() {

        // ── ARRANGE ──────────────────────────────────────────────────────────
        // Booking exist නෑ → false return
        when(bookingRepository.existsByBookingId("INVALID_ID"))
                .thenReturn(false);

        // ── ACT ───────────────────────────────────────────────────────────────
        BookingResponse response = bookingService.cancelBooking("INVALID_ID");

        // ── ASSERT ────────────────────────────────────────────────────────────
        assertNotNull(response);
        assertEquals("booking-cancel", response.getStatus());

        // "no record exists" message contain කරන්නේද?
        assertTrue(response.getMessage().contains("no record exists"),
                "Message 'no record exists' contain කරන්නේ නෑ: " + response.getMessage());

        // CRITICAL: Booking නෑ නිසා deleteById() call නොවිය යුතුයි!
        verify(bookingRepository, never()).deleteById(any());
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  getUserBookingDetails() METHOD TESTS
    //  BookingServiceImpl.java → getUserBookingDetails() method logic:
    //    1. findAllByUserId(userId) call කිරීම
    //    2. Empty list නම් → "no record exists" message
    //    3. List ඇත නම් → BookingOrder list return
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getUserBookingDetails | User ට bookings ඇති නම් → booking list return විය යුතුයි")
    void getUserBookingDetails_shouldReturnBookingList_whenBookingsExist() {

        // ── ARRANGE ──────────────────────────────────────────────────────────
        // userId=1 ට booking entity ලැයිස්තුවක් ඇති කියා simulate
        when(bookingRepository.findAllByUserId(1L))
                .thenReturn(List.of(sampleBookingEntity));

        // ── ACT ───────────────────────────────────────────────────────────────
        BookingResponse response = bookingService.getUserBookingDetails(1L);

        // ── ASSERT ────────────────────────────────────────────────────────────
        assertNotNull(response);
        assertEquals("booking-details", response.getStatus());

        // Message "record found" contain කරන්නේද?
        assertTrue(response.getMessage().contains("record found"),
                "Message 'record found' contain කරන්නේ නෑ: " + response.getMessage());

        // Order list null නොවිය යුතුයි හා empty නොවිය යුතුයි
        assertNotNull(response.getOrderList(), "OrderList null නොවිය යුතුයි");
        assertFalse(response.getOrderList().isEmpty(), "OrderList empty නොවිය යුතුයි");

        // Return වූ booking ගේ userId හරිද?
        assertEquals(1L, response.getOrderList().get(0).getUserId(),
                "Return වූ booking ගේ userId 1 විය යුතුයි");

        // Return වූ booking ගේ eventId හරිද?
        assertEquals("E12345678A", response.getOrderList().get(0).getEventId(),
                "Return වූ booking ගේ eventId 'E12345678A' විය යුතුයි");
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserBookingDetails | User ට bookings නොමැති නම් → 'no record exists' return විය යුතුයි")
    void getUserBookingDetails_shouldReturnNoRecord_whenNoBookingsExist() {

        // ── ARRANGE ──────────────────────────────────────────────────────────
        // userId=99 ට bookings නෑ → empty list return
        when(bookingRepository.findAllByUserId(99L))
                .thenReturn(Collections.emptyList());

        // ── ACT ───────────────────────────────────────────────────────────────
        BookingResponse response = bookingService.getUserBookingDetails(99L);

        // ── ASSERT ────────────────────────────────────────────────────────────
        assertNotNull(response);
        assertEquals("booking-details", response.getStatus());

        // "no record exists" message contain කරන්නේද?
        assertTrue(response.getMessage().contains("no record exists"),
                "Message 'no record exists' contain කරන්නේ නෑ: " + response.getMessage());

        // OrderList null විය යුතුයි (empty list case ට orderList set නෑ)
        assertNull(response.getOrderList(), "Bookings නෑ නිසා OrderList null විය යුතුයි");
    }
}
