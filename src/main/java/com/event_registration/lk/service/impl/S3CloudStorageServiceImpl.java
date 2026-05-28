// package com.event_registration.lk.service.impl;

// import com.event_registration.lk.dto.response.PresignedUrlResponse;
// import com.event_registration.lk.service.CloudStorageService;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
// import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.s3.presigner.S3Presigner;
// import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
// import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// import java.time.Duration;
// import java.time.Instant;

// @Service
// @Slf4j
// public class S3CloudStorageServiceImpl implements CloudStorageService {

//     private final String bucketName;
//     private final String region;
//     private final S3Presigner presigner;

//     public S3CloudStorageServiceImpl(
//             @Value("${aws.s3.bucket-name}") String bucketName,
//             @Value("${aws.s3.region}") String region,
//             @Value("${aws.s3.access-key}") String accessKey,
//             @Value("${aws.s3.secret-key}") String secretKey){
//         this.bucketName = bucketName;
//         this.region = region;
//         this.presigner = S3Presigner.builder()
//                 .region(Region.of(region))
//                 .credentialsProvider(
//                         StaticCredentialsProvider.create(
//                                 AwsBasicCredentials.create(accessKey, secretKey)))
//                 .build();
//     }

//     @Override
//     public PresignedUrlResponse generatePresignedUploadUrl(String objectKey, int expirationMinutes) {
//         PutObjectRequest putRequest = PutObjectRequest.builder()
//                 .bucket(bucketName)
//                 .key(objectKey)
//                 .contentType("image/*")
//                 .build();

//         PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r ->
//                 r.signatureDuration(Duration.ofMinutes(expirationMinutes))
//                  .putObjectRequest(putRequest));

//         String uploadUrl = presignedRequest.url().toString();
//         // Public read URL (assumes bucket policy or CloudFront distribution grants public access)
//         String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
//         Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expirationMinutes));

//         log.info("[s3] generated presigned PUT URL for key={} expires={}", objectKey, expiresAt);
//         return PresignedUrlResponse.builder()
//                 .uploadUrl(uploadUrl)
//                 .imageUrl(imageUrl)
//                 .expiresAt(expiresAt)
//                 .build();
//     }
// }

package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.response.PresignedUrlResponse;
import com.event_registration.lk.service.CloudStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class S3CloudStorageServiceImpl implements CloudStorageService {

    private final String bucketName;
    private final String region;
    private S3Presigner presigner; // Removed final so we can conditionally initialize it
    private final boolean isS3Enabled;

    public S3CloudStorageServiceImpl(
            // Adding :fallback strings tells Spring not to crash if they are missing
            @Value("${aws.s3.bucket-name:NOT_CONFIGURED}") String bucketName,
            @Value("${aws.s3.region:us-east-1}") String region,
            @Value("${aws.s3.access-key:none}") String accessKey,
            @Value("${aws.s3.secret-key:none}") String secretKey) {
        
        this.bucketName = bucketName;
        this.region = region;
        
        // Check if we should actually initialize AWS S3
        if ("NOT_CONFIGURED".equals(bucketName) || "none".equals(accessKey)) {
            log.warn("⚠️ AWS S3 is not configured. Cloud storage will be bypassed with mock data.");
            this.isS3Enabled = false;
            this.presigner = null;
        } else {
            this.isS3Enabled = true;
            this.presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
    }

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String objectKey, int expirationMinutes) {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expirationMinutes));

        // If S3 isn't set up, return a local/dummy URL to keep the frontend happy
        if (!isS3Enabled) {
            log.info("[s3 mock] Bypassing S3. Returning dummy presigned URL for key={}", objectKey);
            return PresignedUrlResponse.builder()
                    .uploadUrl("http://localhost:8081/mock-upload-endpoint") 
                    .imageUrl("https://placehold.co/600x400?text=No+S3+Image") // Standard placeholder image
                    .expiresAt(expiresAt)
                    .build();
        }

        // Original S3 logic runs only if keys exist
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("image/*")
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r ->
                r.signatureDuration(Duration.ofMinutes(expirationMinutes))
                 .putObjectRequest(putRequest));

        String uploadUrl = presignedRequest.url().toString();
        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);

        log.info("[s3] generated presigned PUT URL for key={} expires={}", objectKey, expiresAt);
        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .imageUrl(imageUrl)
                .expiresAt(expiresAt)
                .build();
    }
}