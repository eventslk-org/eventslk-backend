package com.event_registration.lk.controller;

import com.event_registration.lk.dto.Event;
import com.event_registration.lk.dto.response.EventResponse;
import com.event_registration.lk.dto.response.PresignedUrlResponse;
import com.event_registration.lk.service.CloudStorageService;
import com.event_registration.lk.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/event")
@CrossOrigin
@Slf4j
public class EventController {

    private final EventService eventService;
    private final CloudStorageService cloudStorageService;

    public EventController(EventService eventService, CloudStorageService cloudStorageService) {
        this.eventService = eventService;
        this.cloudStorageService = cloudStorageService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> addEvent(@Valid @RequestBody Event event) {
        log.info("Received event: name={}, location={}, seats={}", event.getName(), event.getLocation(), event.getTotalSeats());
        EventResponse response = eventService.addEvent(event);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<EventResponse> deleteEvent(@PathVariable String eventId) {
        EventResponse response = eventService.removeEvent(eventId);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping
    public ResponseEntity<EventResponse> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /** Public event details. Permitted without authentication in SecurityConfig. */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable String eventId) {
        EventResponse response = eventService.getEventById(eventId);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @PutMapping
    public ResponseEntity<EventResponse> updateEvent(@Valid @RequestBody Event event) {
        EventResponse response = eventService.updateEvent(event);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** Image types the upload endpoint will sign for. */
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/avif", "image/gif");

    /**
     * Returns a short-lived S3 presigned PUT URL so the frontend can upload an
     * event image directly to S3 without passing binary data through this service.
     *
     * Flow:
     *   1. Admin calls  GET /event/upload-url?filename=banner.jpg&contentType=image/jpeg
     *   2. Frontend PUTs the file to the returned {@code uploadUrl}, sending the
     *      same Content-Type plus Cache-Control "public, max-age=31536000, immutable"
     *      (both are part of the signature).
     *   3. Frontend passes the returned {@code imageUrl} inside the Event JSON when
     *      calling POST /event or PUT /event.
     *
     * Requires ADMIN role (enforced by SecurityConfig).
     */
    @GetMapping("/upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @RequestParam @NotBlank(message = "filename is required") String filename,
            @RequestParam @NotBlank(message = "contentType is required") String contentType) {
        if (!ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported contentType '" + contentType + "'. Allowed: " + ALLOWED_IMAGE_TYPES);
        }
        String sanitised = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = "events/" + UUID.randomUUID() + "/" + sanitised;
        PresignedUrlResponse response =
                cloudStorageService.generatePresignedUploadUrl(objectKey, contentType.toLowerCase(Locale.ROOT), 15);
        log.info("[upload-url] generated for key={} type={}", objectKey, contentType);
        return ResponseEntity.ok(response);
    }
}
