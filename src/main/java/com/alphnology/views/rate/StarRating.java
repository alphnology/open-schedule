package com.alphnology.views.rate;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 12:09
 */
public class StarRating extends CustomField<Integer> {

    private final HorizontalLayout starsLayout = new HorizontalLayout();
    private final List<Icon> stars = new ArrayList<>();
    private int currentValue = 0;
    private boolean isReadOnly = false;

    public StarRating() {
        this(5); // Default to 5 stars
    }

    public StarRating(int maxRating) {
        starsLayout.setSpacing(false);
        for (int i = 1; i <= maxRating; i++) {
            Icon star = VaadinIcon.STAR_O.create();
            star.getStyle().set("cursor", "pointer");
            star.setColor("var(--lumo-primary-color)");
            int starIndex = i;

            star.addClickListener(event -> {
                if (!isReadOnly) {
                    setValue(starIndex);
                }
            });

            // Visual feedback on hover
            star.getElement().addEventListener("mouseover", e -> {
                if (!isReadOnly) {
                    updateStars(starIndex);
                }
            });
            star.getElement().addEventListener("mouseout", e -> {
                if (!isReadOnly) {
                    updateStars(currentValue);
                }
            });

            stars.add(star);
            starsLayout.add(star);
        }
        add(starsLayout);
    }

    @Override
    protected Integer generateModelValue() {
        return currentValue;
    }

    @Override
    protected void setPresentationValue(Integer newPresentationValue) {
        this.currentValue = (newPresentationValue == null) ? 0 : newPresentationValue;
        updateStars(this.currentValue);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        this.isReadOnly = readOnly;
        stars.forEach(star -> star.getStyle().set("cursor", readOnly ? "default" : "pointer"));
    }

    private void updateStars(int rating) {
        for (int i = 0; i < stars.size(); i++) {
            Icon star = stars.get(i);
            if (i < rating) {
                star.getElement().setAttribute("icon", "vaadin:star");
            } else {
                star.getElement().setAttribute("icon", "vaadin:star-o");
            }
        }
    }
}