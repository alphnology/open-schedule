package com.alphnology.utils;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.FileDownloadHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 11:17
 */
@Slf4j
public class ImageUtils {

    private ImageUtils() {
    }

    public static Image getMainImage() {
        return getDefaultMainImage();
    }

    public static Image getDefaultMainImage() {
        for (Path candidate : resolveLogoCandidates()) {
            log.info("Loading image from {}", candidate);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                log.info("Header image found at: {}", candidate.toAbsolutePath());
                FileDownloadHandler downloadHandler = DownloadHandler.forFile(candidate.toFile());
                log.info("Using external image as header {}", candidate.toAbsolutePath());
                return new Image(downloadHandler, "Logo");
            }
        }

        log.info("Using default image as header");
        return new Image("images/logo.png", "Open Schedule");
    }

    private static List<Path> resolveLogoCandidates() {
        String configuredAssetsDir = System.getenv("APP_ASSETS_DIR");
        String configuredLogoPath = System.getenv("APP_LOGO_PATH");

        Path workingDirectory = Paths.get(System.getProperty("user.dir"));

        return Stream.of(
                pathFrom(configuredLogoPath),
                pathFrom(configuredAssetsDir, "logo.png"),
                Paths.get("/assets/logo.png"),
                workingDirectory.resolve("assets/logo.png"),
                Paths.get("assets/logo.png")
        ).filter(Objects::nonNull).distinct().toList();
    }

    private static Path pathFrom(String rawPath) {
        return StringUtils.hasText(rawPath) ? Paths.get(rawPath.trim()) : null;
    }

    private static Path pathFrom(String rawDirectory, String filename) {
        return StringUtils.hasText(rawDirectory) ? Paths.get(rawDirectory.trim()).resolve(filename) : null;
    }
}
