package com.alphnology.views.rate;

import com.alphnology.data.Session;
import com.alphnology.data.SessionRating;
import com.alphnology.data.User;
import com.alphnology.services.SessionRatingService;
import com.alphnology.utils.Broadcaster;
import com.alphnology.utils.CommonUtils;
import com.alphnology.utils.NotificationUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;

import static com.alphnology.utils.Broadcaster.RATE_SESSION;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 13:34
 */
public class RatingDialog extends Dialog {

    private final Binder<SessionRating> binder = new Binder<>(SessionRating.class);
    private final SessionRating sessionRating;

    private final H4 title = new H4();
    private final TextArea comment = new TextArea("Comments");
    private final Button saveButton = new Button("Submit Rating");
    private final Button cancelButton = new Button("Cancel");

    private final HorizontalLayout starsLayout = new HorizontalLayout();
    private final List<Icon> stars = new ArrayList<>();
    private int currentRating = 0;

    private final SessionRatingService sessionRatingService;
    private final Runnable callback;

    /**
     * Constructs a RatingDialog.
     */

    public RatingDialog(SessionRatingService sessionRatingService, Session session, SessionRating existingRating, Runnable callback) {
        this.sessionRatingService = sessionRatingService;
        this.callback = callback;
        this.sessionRating = (existingRating != null) ? existingRating : new SessionRating();
        this.sessionRating.setSession(session);
        this.sessionRating.setUsers(VaadinSession.getCurrent().getAttribute(User.class));

        binder.setBean(this.sessionRating);

        setHeaderTitle("Rate this Session");
        setDraggable(true);
        setResizable(true);
        setWidthFull();

        addClassNames(LumoUtility.MaxWidth.SCREEN_LARGE);

        add(createDialogLayout());
        createFooter();

        binder.forField(comment)
                .withValidator(c -> c == null || c.length() <= 200, "Comment cannot exceed 200 characters.")
                .bind(SessionRating::getComment, SessionRating::setComment);

        if (this.sessionRating.getScore() > 0) {
            setRating(this.sessionRating.getScore());
            saveButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
        }
    }

    private VerticalLayout createDialogLayout() {
        title.setText(sessionRating.getSession().getTitle());
        title.getStyle().set("margin-top", "0");

        CommonUtils.commentsFormat(comment, 200);

        createStarRatingComponent();

        VerticalLayout dialogLayout = new VerticalLayout(title, starsLayout, comment);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        return dialogLayout;
    }

    private void createStarRatingComponent() {
        starsLayout.setSpacing(false);
        for (int i = 1; i <= 5; i++) {
            Icon star = VaadinIcon.STAR_O.create();
            star.getStyle().set("cursor", "pointer");
            star.setColor("var(--lumo-primary-color)");
            int starIndex = i;

            star.addClickListener(event -> {
                setRating(starIndex);
                binder.getBean().setScore(currentRating);
                saveButton.setEnabled(true);
            });

            star.getElement().addEventListener("mouseover", e -> updateStars(starIndex, false));
            star.getElement().addEventListener("mouseout", e -> updateStars(currentRating, true));

            stars.add(star);
            starsLayout.add(star);
        }
    }

    private void setRating(int rating) {
        this.currentRating = rating;
        updateStars(rating, true);
    }

    private void updateStars(int rating, boolean isFinal) {
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                stars.get(i).getElement().setAttribute("icon", "vaadin:star");
            } else {
                stars.get(i).getElement().setAttribute("icon", "vaadin:star-o");
            }
            if (!isFinal) {
                stars.get(i).getStyle().set("opacity", i < rating ? "1.0" : "0.5");
            } else {
                stars.get(i).getStyle().set("opacity", "1.0");
            }
        }
    }

    private void createFooter() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        getFooter().add(cancelButton);
        getFooter().add(saveButton);

        saveButton.addClickListener(event -> validateAndSave());
        cancelButton.addClickListener(event -> close());
    }

    private void validateAndSave() {
        if (currentRating == 0) {
            return;
        }
        try {
            binder.writeBean(sessionRating);
            sessionRating.setScore(currentRating);

            sessionRatingService.save(sessionRating);

            Broadcaster.broadcast(RATE_SESSION.formatted(sessionRating.getSession().getCode()));
            NotificationUtils.success("Rating saved!");

            if (callback != null) {
                callback.run();
            }
            close();
        } catch (ValidationException e) {
            // Handle validation errors (e.g., show a notification to the user)
            // For now, comment length is the only validation, handled by max-length.
        }
    }
}
