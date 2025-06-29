package com.alphnology.utils;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 11:17
 */
@Slf4j
public class ImageUtils {

    private ImageUtils() {
    }

    public static Image getMainImage() {
        Image drawerImage = null;

        Path headerImage = Paths.get("assets/logo.png");
        log.info("Loading image from {}", headerImage);

        if (Files.exists(headerImage)) {
            log.info("Header image found at: {}", headerImage.toAbsolutePath());
            StreamResource resource = new StreamResource("header.png", () -> {
                try {
                    return new FileInputStream(headerImage.toFile());
                } catch (FileNotFoundException e) {
                    return null;
                }
            });

            log.info("Using external image as header {}", headerImage.toAbsolutePath());
            drawerImage = new Image(resource, "Logo");
        }

        if (drawerImage == null) {
            log.info("Using default image as header");
            drawerImage = new Image("images/logo.png", "Open Schedule");
        }

        return drawerImage;
    }
}
