package com.alphnology.utils;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * @author me@fredpena.dev
 * @created 17/10/2025  - 20:44
 */
public class DownloadHandlerUtils {

    private DownloadHandlerUtils() {
    }

    public static InputStreamDownloadHandler fromByte(byte[] array) {
        return DownloadHandler.fromInputStream((event) -> {
            try {
                return new DownloadResponse(new ByteArrayInputStream(array), UUID.randomUUID().toString(), null, array.length);
            } catch (Exception e) {
                return DownloadResponse.error(500);
            }
        });
    }

}
