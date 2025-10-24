package com.alphnology.utils;

import com.alphnology.data.Contactable;
import com.alphnology.services.QrService;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
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
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    /**
     * Builds a vCard string from a Speaker object.
     *
     * @param contactable The Contactable data.
     * @return A vCard 3.0 formatted string.
     */
    public static String buildVCard(Contactable contactable) {
        String fn = esc(contactable.fullName());
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD").append("\r\n");
        sb.append("VERSION:3.0").append("\r\n");
        sb.append("N:").append(fn).append(";;;;").append("\r\n");
        sb.append("FN:").append(fn).append("\r\n");
        if (contactable.company() != null) sb.append("ORG:").append(esc(contactable.company())).append("\r\n");
        if (contactable.title() != null) sb.append("TITLE:").append(esc(contactable.title())).append("\r\n");
        if (contactable.email() != null)
            sb.append("EMAIL;TYPE=INTERNET:").append(esc(contactable.email())).append("\r\n");
        if (contactable.phone() != null) sb.append("TEL;TYPE=CELL:").append(esc(contactable.phone())).append("\r\n");
        sb.append("REV:").append(LocalDate.now()).append("\r\n");
        sb.append("END:VCARD").append("\r\n");
        return sb.toString();
    }


    public static String getVCardUrl(Contactable contactable, String type) {
        HttpServletRequest request = ((VaadinServletRequest) VaadinRequest.getCurrent()).getHttpServletRequest();
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        String portPart = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;

        return "%s://%s%s/vcard/%d/%s".formatted(scheme, serverName, portPart, contactable.code(), type);
    }

    public static void openQr(Contactable contactable, String currentType, QrService qrService) {
        String vCardUrl = VCardUtil.getVCardUrl(contactable, currentType);

        DownloadHandler qrResource = downloadHandler(qrService, vCardUrl);

        Dialog qrDialog = new Dialog();
        qrDialog.setDraggable(true);
        qrDialog.setHeaderTitle("Scan to Share");
        Image qrImage = new Image(qrResource, "Profile QR Code");
        qrImage.setWidth("300px");
        qrDialog.add(qrImage);
        qrDialog.open();
    }

    public static DownloadHandler downloadHandler(QrService qrService, String vCardUrl) {
        return downloadHandler(qrService, vCardUrl, "vcard-qr");
    }

    public static DownloadHandler downloadHandler(QrService qrService, String vCardUrl, String fileName) {
        byte[] qrCodeBytes = qrService.generatePng(vCardUrl, 256);

        return DownloadHandler.fromInputStream((event1) -> {
            try {
                return new DownloadResponse(new ByteArrayInputStream(qrCodeBytes), "%s.png".formatted(fileName), null, qrCodeBytes.length);
            } catch (Exception e) {
                return DownloadResponse.error(500);
            }
        });

    }



}
