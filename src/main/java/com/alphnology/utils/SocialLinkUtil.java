package com.alphnology.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.SvgIcon;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author me@fredpena.dev
 * @created 15/06/2025  - 14:05
 */
public class SocialLinkUtil {

    private SocialLinkUtil() {
    }

    public static Component createSocialIconLink(String url) {
        String domain = extractDomain(url);
        SvgIcon icon = resolveIconForDomain(domain);
        icon.setSize("30px");
        icon.setTooltipText(domain);

        Anchor anchor = new Anchor(url, icon);
        anchor.setTarget("_blank");
        anchor.getStyle().set("margin-right", "0.5rem");

        return anchor;
    }

    private static String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return "";
            return host.startsWith("www.") ? host.substring(4).toLowerCase() : host.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    private static SvgIcon resolveIconForDomain(String domain) {
        String base = domain.split("\\.")[0];

        Optional<LineAwesomeIcon> matched = Arrays.stream(LineAwesomeIcon.values())
                .filter(icon -> icon.getSvgName().equalsIgnoreCase(base))
                .findFirst();

        return matched.map(LineAwesomeIcon::create).orElseGet(() ->
                switch (domain) {
                    case "x.com" -> LineAwesomeIcon.TWITTER.create();
                    case "medium.com" -> LineAwesomeIcon.BOOK_SOLID.create();
                    default -> LineAwesomeIcon.GLOBE_SOLID.create();
                }
        );
    }
}

