package com.alphnology.utils;

import com.alphnology.data.Speaker;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;

/**
 * Utility class for creating vCard (version 3.0) strings.
 *
 * @author me@fredpena.dev
 * @created 19/10/2025  - 19:14
 */
public class VCardUtil {

    private VCardUtil() {
    }

    /**
     * Escapes special characters for vCard format.
     *
     * @param s The string to escape.
     * @return The escaped string.
     */
    public static String esc(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    /**
     * Builds a vCard string from a Speaker object.
     *
     * @param speaker The speaker data.
     * @return A vCard 3.0 formatted string.
     */
    public static String buildVCard(Speaker speaker) {
        String fn = esc(speaker.getName());
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD").append("\r\n");
        sb.append("VERSION:3.0").append("\r\n");
        sb.append("N:").append(fn).append(";;;;").append("\r\n");
        sb.append("FN:").append(fn).append("\r\n");
        if (speaker.getCompany() != null) sb.append("ORG:").append(esc(speaker.getCompany())).append("\r\n");
        if (speaker.getTitle() != null) sb.append("TITLE:").append(esc(speaker.getTitle())).append("\r\n");
        if (speaker.getEmail() != null)
            sb.append("EMAIL;TYPE=INTERNET:").append(esc(speaker.getEmail())).append("\r\n");
        if (speaker.getPhone() != null) sb.append("TEL;TYPE=CELL:").append(esc(speaker.getPhone())).append("\r\n");
        sb.append("REV:").append(LocalDate.now()).append("\r\n");
        sb.append("END:VCARD").append("\r\n");
        return sb.toString();
    }


    public static String getVCardUrl(Speaker speaker) {
        HttpServletRequest request = ((VaadinServletRequest) VaadinRequest.getCurrent()).getHttpServletRequest();
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        String portPart = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;

        return "%s://%s%s/vcard/%d".formatted(scheme, serverName, portPart, speaker.getCode());
    }
}
