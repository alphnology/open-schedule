package com.alphnology.utils;

import lombok.Getter;
import org.vaadin.addons.tatu.ColorPicker;

@Getter
public enum ColorUtils {


    DEFAULT("#0088DC", "Blue Cola"),
    CANCEL("#FF6347", "Tomato"),
    CONFIRM("#59910F", "Basil"),
    FINISHED("#846C96", "Blueberry"),
    NOT_AVAILABLE("#616469", "Graphite"),
    HOLIDAY("#FF1A1A", "Yelp Red"),
    HENKEL("#E1000F", "Henkel Red"),
    TANGERINE("#FA9336", "Tangerine"),
    BANANA("#F7D15F", "Banana"),
    LAVENDER("#9995DB", "Lavender"),
    GRAPE("#8D5C74", "Grape"),
    FLAMINGO("#F7969E", "Flamingo"),
    SAGE("#639A8E", "Sage"),
    PEACOCK("#0C7478", "Peacock");


    private final ColorPicker.ColorPreset colorPreset;


    ColorUtils(String color, String caption) {
        this.colorPreset = new ColorPicker.ColorPreset(color, caption);

    }
}
