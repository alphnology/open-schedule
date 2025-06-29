package com.alphnology.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * @author me@fredpena.dev
 * @created 29/06/2025  - 18:28
 */
@PageTitle("About")
@Route(value = "about")
@Menu(order = 3, icon = LineAwesomeIconUrl.INFO_SOLID)
@AnonymousAllowed
public class AboutView extends VerticalLayout {

    public AboutView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        addClassNames(LumoUtility.MaxWidth.SCREEN_MEDIUM);
        getStyle()
                .set("justify-self", "center")
                .set("text-align", "center");

        add(new H2("About Open Schedule"));
        add(new Paragraph("A modern, open-source solution for managing event schedules."));

        Paragraph mission = new Paragraph(
                "The idea for Open Schedule was sparked during the organization of JconfDominicana 2025, a community-driven Java conference. " +
                "Our mission is to provide a beautiful, easy-to-use platform that empowers event organizers and delights attendees. " +
                "This project is open for contributions on GitHub."
        );
        mission.addClassNames(LumoUtility.Margin.Top.LARGE, LumoUtility.Margin.Bottom.MEDIUM);
        add(mission);

        Anchor githubLink = new Anchor("https://github.com/alphnology/open-schedule", "View on GitHub");
        githubLink.setTarget("_blank");
        add(githubLink);

        H2 maintainedBy = new H2("Maintained By");
        maintainedBy.addClassNames(LumoUtility.Margin.Top.XLARGE);
        add(maintainedBy);

        Image logo = new Image("images/developed.png", "Alphnology Logo");
        logo.setWidth("200px");

        Anchor logoLink = new Anchor("https://alphnology.com", logo);
        logoLink.setTarget("_blank");
        add(logoLink);
    }
}