package com.alphnology.configurations;

import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

@Component
@RequiredArgsConstructor
public class ExternalBrandingVaadinInitListener implements VaadinServiceInitListener {

    private final ExternalBrandingService externalBrandingService;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler(externalCssRequestHandler());
        event.addIndexHtmlRequestListener(response ->
                externalBrandingService.getExternalCssUrl().ifPresent(url ->
                        response.getDocument().head()
                                .appendElement("link")
                                .attr("rel", "stylesheet")
                                .attr("href", url)
                )
        );
    }

    private RequestHandler externalCssRequestHandler() {
        return (session, request, response) -> {
            String path = request.getPathInfo();
            if (path == null || !externalBrandingService.matchesRequestPath(path)) {
                return false;
            }

            return externalBrandingService.getExternalCssPath().map(cssPath -> {
                try {
                    byte[] bytes = Files.readAllBytes(cssPath);
                    response.setContentType("text/css; charset=UTF-8");
                    response.setHeader("Cache-Control", "public, max-age=300");
                    response.setContentLength(bytes.length);
                    response.getOutputStream().write(bytes);
                    return true;
                } catch (Exception exception) {
                    throw new RuntimeException("Failed to serve external branding CSS: " + cssPath, exception);
                }
            }).orElse(false);
        };
    }
}
