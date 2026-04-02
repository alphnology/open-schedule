package com.alphnology.infrastructure.storage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class S3ObjectStorageService implements ObjectStorageService {

    private final MinioClient s3Client;
    private final MinioClient publicS3Client;
    private final StorageProperties props;

    public S3ObjectStorageService(MinioClient s3Client,
                                  @Qualifier("publicS3Client") MinioClient publicS3Client,
                                  StorageProperties props) {
        this.s3Client = s3Client;
        this.publicS3Client = publicS3Client;
        this.props = props;
    }

    @Override
    public void upload(String key, InputStream data, long size, String contentType) {
        try {
            if (data.markSupported()) {
                data.mark((int) Math.min(size, Integer.MAX_VALUE));
            }
            putObject(key, data, size, contentType);
            log.debug("Uploaded object: {}", key);
        } catch (ErrorResponseException e) {
            if (isNoSuchBucket(e)) {
                try {
                    resetStream(data, key);
                    createBucket();
                    putObject(key, data, size, contentType);
                    log.info("Created bucket {} and uploaded object {}", props.getBucket(), key);
                    return;
                } catch (Exception retryException) {
                    throw new RuntimeException("Failed to upload object: " + key, retryException);
                }
            }
            throw new RuntimeException("Failed to upload object: " + key, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload object: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return s3Client.getObject(GetObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download object: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .build());
            log.debug("Deleted object: {}", key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object: " + key, e);
        }
    }

    @Override
    public String getSignedUrl(String key) {
        try {
            return publicS3Client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .method(Method.GET)
                    .expiry(props.getSignedUrlExpirySeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signed URL for: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.statObject(StatObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void putObject(String key, InputStream data, long size, String contentType) throws Exception {
        s3Client.putObject(PutObjectArgs.builder()
                .bucket(props.getBucket())
                .object(key)
                .stream(data, size, -1)
                .contentType(contentType)
                .build());
    }

    private void createBucket() throws Exception {
        s3Client.makeBucket(MakeBucketArgs.builder()
                .bucket(props.getBucket())
                .build());
    }

    private boolean isNoSuchBucket(ErrorResponseException e) {
        return e.errorResponse() != null && "NoSuchBucket".equalsIgnoreCase(e.errorResponse().code());
    }

    private void resetStream(InputStream data, String key) throws Exception {
        if (!data.markSupported()) {
            throw new IllegalStateException("Upload stream cannot be retried for object: " + key);
        }
        data.reset();
    }
}
