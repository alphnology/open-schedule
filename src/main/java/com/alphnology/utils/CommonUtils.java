package com.alphnology.utils;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.text.Normalizer;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author me@fredpena.dev
 * @created 15/11/2024  - 13:22
 */
public final class CommonUtils {

    private CommonUtils() {
    }

    public static void commentsFormat(TextArea comments, int charLimit) {
        comments.setClearButtonVisible(true);

        comments.setWidthFull();
        comments.setMaxLength(charLimit);
        comments.setValueChangeMode(ValueChangeMode.EAGER);
        comments.addValueChangeListener(e ->
                e.getSource().setHelperText(e.getValue().length() + "/" + charLimit)
        );
    }

    public static String normalizeText(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }

    public static <C> ComboBox.ItemFilter<C> comboBoxItemFilter(Function<C, String> propertyExtractor, BiPredicate<String, String> filterLogic) {
        return (item, filterText) -> filterLogic.test(normalizeText(propertyExtractor.apply(item)), normalizeText(filterText));
    }

    public static String getBaseUrl() {
        HttpServletRequest request = ((VaadinServletRequest) VaadinRequest.getCurrent()).getHttpServletRequest();
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        String portPart = (serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort;
        return scheme + "://" + serverName + portPart;
    }

}
