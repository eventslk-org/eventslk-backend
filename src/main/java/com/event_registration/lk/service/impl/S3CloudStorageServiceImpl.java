package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.response.PresignedUrlResponse;
import com.event_registration.lk.service.CloudStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.Instant;

/**
 * S3 presigned-upload implementation.
 *
 * <p>Credentials: static access/secret keys are used only when explicitly
 * configured (local dev). When absent, the AWS default provider chain applies,
 * which is what production should use — an IAM role attached to the workload
 * (EC2 instance profile / EKS IRSA / ECS task role). No keys in config or env.
 *
 * <p>The signed request pins {@code Content-Type} and {@code Cache-Control},
 * so the client PUT must send both headers with exactly these values:
 * the declared image type, and {@code public, max-age=31536000, immutable}.
 * Uploaded objects are keyed by UUID and never rewritten, so browsers and any
 * CDN in front (see {@code aws.s3.public-base-url}) can cache them forever.
 *
 * <p>Mock mode: when no bucket is configured the service returns placeholder
 * URLs so the rest of the app keeps working in local dev without AWS.
 */
@Service
@Slf4j
public class S3CloudStorageServiceImpl implements CloudStorageService {

    static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final String bucketName;
    private final String publicBaseUrl;
    private final S3Presigner presigner;
    private final boolean s3Enabled;

    public S3CloudStorageServiceImpl(
            @Value("${aws.s3.bucket-name:}") String bucketName,
            @Value("${aws.s3.region:ap-south-1}") String region,
            @Value("${aws.s3.public-base-url:}") String publicBaseUrl,
            @Value("${aws.s3.access-key:}") String accessKey,
            @Value("${aws.s3.secret-key:}") String secretKey) {

        this.bucketName = bucketName;
        this.s3Enabled = !bucketName.isBlank();
        this.publicBaseUrl = !publicBaseUrl.isBlank()
                ? stripTrailingSlash(publicBaseUrl)
                : String.format("https://%s.s3.%s.amazonaws.com", bucketName, region);

        if (!s3Enabled) {
            log.warn("aws.s3.bucket-name not configured — cloud storage runs in MOCK mode (placeholder URLs).");
            this.presigner = null;
            return;
        }

        AwsCredentialsProvider credentials = !accessKey.isBlank() && !secretKey.isBlank()
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                : DefaultCredentialsProvider.create();

        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();
        log.info("[s3] presigner ready: bucket={} region={} credentials={}",
                bucketName, region, !accessKey.isBlank() ? "static (dev)" : "default chain (IAM role)");
    }

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String objectKey, String contentType, int expirationMinutes) {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expirationMinutes));

        if (!s3Enabled) {
            log.info("[s3 mock] returning placeholder URLs for key={}", objectKey);
            return PresignedUrlResponse.builder()
                    .uploadUrl("http://localhost:8081/mock-upload-endpoint")
                    .imageUrl("https://placehold.co/600x400?text=No+S3+Image")
                    .expiresAt(expiresAt)
                    .build();
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r ->
                r.signatureDuration(Duration.ofMinutes(expirationMinutes))
                 .putObjectRequest(putRequest));

        String imageUrl = publicBaseUrl + "/" + objectKey;
        log.info("[s3] presigned PUT for key={} type={} expires={}", objectKey, contentType, expiresAt);
        return PresignedUrlResponse.builder()
                .uploadUrl(presignedRequest.url().toString())
                .imageUrl(imageUrl)
                .expiresAt(expiresAt)
                .build();
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
