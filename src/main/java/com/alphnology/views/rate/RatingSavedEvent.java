package com.alphnology.views.rate;

import com.alphnology.data.SessionRating;
import lombok.Getter;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 14:10
 */
@Getter
public class RatingSavedEvent {

    private final SessionRating rating;

    public RatingSavedEvent(SessionRating rating) {
        this.rating = rating;
    }

}