package com.alphnology.infrastructure.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;
    private final MinioClient publicMinioClient;
    private final StorageProperties props;

    public MinioObjectStorageService(MinioClient minioClient,
                                     @Qualifier("publicMinioClient") MinioClient publicMinioClient,
                                     StorageProperties props) {
        this.minioClient = minioClient;
        this.publicMinioClient = publicMinioClient;
        this.props = props;
    }

    @Override
    public void upload(String key, InputStream data, long size, String contentType) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
            log.debug("Uploaded object: {}", key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload object: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
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
            minioClient.removeObject(RemoveObjectArgs.builder()
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
            return publicMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
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
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(key)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(props.getBucket())
                .build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(props.getBucket())
                    .build());
            log.info("Created MinIO bucket: {}", props.getBucket());
        }
    }
}
