package com.alphnology.views.speakers;

import com.alphnology.data.Country;
import com.alphnology.data.Speaker;
import com.alphnology.services.SessionRatingService;
import com.alphnology.services.SessionService;
import com.alphnology.services.SpeakerService;
import com.alphnology.utils.CountryUtils;
import com.alphnology.views.rate.RatingEventBus;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alphnology.utils.PredicateUtils.createPredicateForSelectedItems;
import static com.alphnology.utils.PredicateUtils.predicateUnaccentLike;

@PageTitle("Speakers")
@Route("speaker")
@Menu(order = 1, icon = LineAwesomeIconUrl.USERS_SOLID)
@AnonymousAllowed
public class SpeakersView extends VerticalLayout {

private static final String SEARCH_PLACEHOLDER = "Search...";

    private final transient SpeakerService speakerService;

    private final OrderedList imageContainer = new OrderedList();
    private final TextField searchField = new TextField(SEARCH_PLACEHOLDER);
    private final MultiSelectComboBox<String> filterTitle = new MultiSelectComboBox<>("Title");
    private final MultiSelectComboBox<Country> filterCountry = new MultiSelectComboBox<>("Country");
    private final MultiSelectComboBox<String> filterCompany = new MultiSelectComboBox<>("Company");
    private final ComboBox<String> orderBy = new ComboBox<>("Order by");
    private final SpeakersViewDetails speakersViewDetails;



    public SpeakersView(SpeakerService speakerService, SessionService sessionService, SessionRatingService sessionRatingService, RatingEventBus ratingEventBus) {
        this.speakerService = speakerService;
        addClassNames("speakers-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.STRETCH);
        addClassNames(LumoUtility.MaxWidth.SCREEN_XLARGE, LumoUtility.Margin.Horizontal.AUTO, LumoUtility.Padding.Bottom.LARGE, LumoUtility.Padding.Horizontal.LARGE);

        speakersViewDetails = new SpeakersViewDetails(ratingEventBus, sessionService, sessionRatingService);

        HorizontalLayout container = new HorizontalLayout();
        container.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.BETWEEN);

        VerticalLayout headerContainer = new VerticalLayout();
        headerContainer.addClassNames(LumoUtility.Padding.Horizontal.NONE);

        H2 headerTitle = new H2("Lineup of speakers");
        headerTitle.getStyle().set("color", "#17222F");
        headerTitle.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.XSMALL, LumoUtility.FontSize.XXXLARGE, LumoUtility.Padding.NONE);

        headerContainer.add(headerTitle);

        Header header = new Header();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.NONE,
                LumoUtility.BoxShadow.NONE);

        initComponents();

        Div headerOption = new Div(searchField, filterTitle, filterCompany, filterCountry, orderBy);
        headerOption.addClassNames(LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.ROW,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Width.FULL,
                LumoUtility.Padding.Horizontal.NONE,
                LumoUtility.Gap.Column.LARGE,
                "flex-wrap-layout");


        header.add(headerContainer, headerOption);
        add(header);

        imageContainer.addClassNames(LumoUtility.Gap.MEDIUM, LumoUtility.Display.GRID, LumoUtility.ListStyleType.NONE, LumoUtility.Margin.NONE, LumoUtility.Padding.NONE);

        refreshAll();


        Section imageContainerSection = new Section(imageContainer);
        imageContainerSection.setWidthFull();
        imageContainerSection.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BorderRadius.NONE, LumoUtility.BoxShadow.NONE);

        Scroller scroller = new Scroller(imageContainerSection);
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        add(scroller);

    }


    private void initComponents() {
        searchField.focus();
        searchField.addClassNames(LumoUtility.Flex.GROW, LumoUtility.MinWidth.NONE);
        searchField.setAriaLabel(SEARCH_PLACEHOLDER);
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.setPlaceholder(SEARCH_PLACEHOLDER);
        searchField.setPrefixComponent(LumoIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

        orderBy.setItems(List.of("Name", "Company", "Title", "Country"));
        orderBy.setValue("Name");

        searchField.addValueChangeListener(e -> refreshAll());
        filterTitle.addValueChangeListener(e -> refreshAll());
        filterCompany.addValueChangeListener(e -> refreshAll());
        filterCountry.addValueChangeListener(e -> refreshAll());
        orderBy.addValueChangeListener(e -> refreshAll());

        searchField.setClearButtonVisible(true);
        filterTitle.setClearButtonVisible(true);
        filterCompany.setClearButtonVisible(true);
        filterCountry.setClearButtonVisible(true);

        filterTitle.addClassNames(LumoUtility.Display.HIDDEN, LumoUtility.Display.Breakpoint.Large.FLEX);
        filterCompany.addClassNames(LumoUtility.Display.HIDDEN, LumoUtility.Display.Breakpoint.Large.FLEX);
        filterCountry.addClassNames(LumoUtility.Display.HIDDEN, LumoUtility.Display.Breakpoint.Large.FLEX);

        List<Speaker> speakers = speakerService.findAll();

        Map<String, List<Speaker>> byTitle = groupByField(speakers, Speaker::getTitle);
        Map<String, List<Speaker>> byCompany = groupByField(speakers, Speaker::getCompany);
        Map<String, List<Speaker>> byCountry = groupByField(speakers, Speaker::getCountry);

        List<Country> listCountries = CountryUtils.getCountryNamesWithCodes()
                .stream().filter(p -> byCountry.keySet().stream().anyMatch(m -> m.equalsIgnoreCase(p.getCode()))).toList();

        filterTitle.setItems(byTitle.keySet());
        filterCountry.setItems(listCountries);
        filterCompany.setItems(byCompany.keySet());

        filterTitle.setPlaceholder("Choose a Title");
        filterCountry.setPlaceholder("Choose a country");
        filterCompany.setPlaceholder("Choose a company");
    }

    private static <K> Map<K, List<Speaker>> groupByField(List<Speaker> speakers, Function<Speaker, K> classifier) {
        return speakers.stream().collect(Collectors.groupingBy(classifier));
    }

    private Specification<Speaker> createFilterSpecification() {
        return (root, query, builder) -> {

            final String search = searchField.getValue().toLowerCase().trim();

            Order order = builder.asc(root.get(orderBy.getValue().toLowerCase()));
            assert query != null;
            query.orderBy(order);
            query.distinct(true);

            Predicate predicateName = predicateUnaccentLike(root, builder, "name", search);
            Predicate predicateTitle = predicateUnaccentLike(root, builder, "title", search);
            Predicate predicateCompany = predicateUnaccentLike(root, builder, "company", search);
            Predicate predicateCountry = predicateUnaccentLike(root, builder, "country", search);
            Predicate predicateBio = predicateUnaccentLike(root, builder, "bio", search);

            final List<Predicate> orPredicates = new ArrayList<>(List.of(predicateName, predicateTitle, predicateCompany, predicateCountry, predicateBio));

            Predicate orPredicate = orPredicates.isEmpty() ? builder.conjunction() : builder.or(orPredicates.toArray(Predicate[]::new));

            Predicate predicateSelectTitle = createPredicateForSelectedItems(Optional.ofNullable(filterTitle.getSelectedItems()), items -> root.get("title").in(items), builder);

            Predicate predicateSelectCompany = createPredicateForSelectedItems(Optional.ofNullable(filterCompany.getSelectedItems()), items -> root.get("company").in(items), builder);

            Predicate predicateSelectCountry = createPredicateForSelectedItems(Optional.of(filterCountry.getSelectedItems().stream().map(Country::getCode).toList()), items -> root.get("country").in(items), builder);

            return builder.and(orPredicate, predicateSelectTitle, predicateSelectCompany, predicateSelectCountry);

        };
    }

    private void refreshAll() {
        imageContainer.removeAll();

        speakerService.findAll(createFilterSpecification())
                .forEach(s -> imageContainer.add(new SpeakersViewCard(s, speakersViewDetails::showSpeaker)));
    }
}
