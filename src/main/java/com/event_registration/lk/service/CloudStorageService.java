package com.event_registration.lk.service;

import com.event_registration.lk.dto.response.PresignedUrlResponse;

public interface CloudStorageService {

    /**
     * Generates a short-lived S3 presigned PUT URL so the frontend can upload
     * an image directly to S3 without routing binary data through this service.
     *
     * @param objectKey         S3 object key (e.g. "events/uuid/photo.jpg")
     * @param contentType       MIME type baked into the signature; the client's
     *                          PUT must send the same {@code Content-Type} header
     * @param expirationMinutes lifetime of the presigned URL
     * @return upload URL for the client + final CDN/S3 URL to store in the DB
     */
    PresignedUrlResponse generatePresignedUploadUrl(String objectKey, String contentType, int expirationMinutes);
}
