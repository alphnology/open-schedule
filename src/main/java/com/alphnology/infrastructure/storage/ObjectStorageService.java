package com.alphnology.infrastructure.storage;

import java.io.InputStream;

public interface ObjectStorageService {

    void upload(String key, InputStream data, long size, String contentType);

    InputStream download(String key);

    void delete(String key);

    String getSignedUrl(String key);

    boolean exists(String key);
}
