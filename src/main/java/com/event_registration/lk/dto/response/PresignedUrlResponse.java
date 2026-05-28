package com.event_registration.lk.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PresignedUrlResponse(
        String uploadUrl,   // PUT this URL from the frontend to upload the file directly to S3
        String imageUrl,    // store this value in the Event JSON payload after upload completes
        Instant expiresAt
) {}
