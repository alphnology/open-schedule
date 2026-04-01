package com.alphnology.views.news;

import com.alphnology.data.News;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import com.alphnology.services.NewsService;
import com.alphnology.utils.DateTimeFormatterUtils;
import org.springframework.util.StringUtils;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.data.jpa.domain.Specification;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.List;

/**
 * @author me@fredpena.dev
 * @created 19/10/2025  - 08:56
 */
@PageTitle("Noticias")
//@PageTitle("News")
@Route("news")
@Menu(order = 2, icon = LineAwesomeIconUrl.NEWSPAPER)
@AnonymousAllowed
public class NewsView extends VerticalLayout {

    private final transient ObjectStorageService storageService;

    public NewsView(NewsService newsService, ObjectStorageService storageService) {
        this.storageService = storageService;
        setSizeFull();
        addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BoxSizing.BORDER);
        setAlignItems(Alignment.CENTER);

        H1 header = new H1("Latest News");
        header.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.Margin.Bottom.LARGE);

        Div newsContainer = new Div();
        newsContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.LARGE, LumoUtility.MaxWidth.SCREEN_XLARGE);
        Specification<News> spec = (root, query, cb) -> {
            assert query != null;
            query.orderBy(cb.desc(root.get("publishedAt")));
            return cb.conjunction();
        };
        List<News> newsList = newsService.findAll(spec);

        if (newsList.isEmpty()) {
            add(header, new Span("No news yet. Stay tuned!"));
        } else {
            newsList.forEach(news -> newsContainer.add(createNewsCard(news)));
            add(header, newsContainer);
        }
    }

    private Div createNewsCard(News news) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE, LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.MEDIUM);
        card.getStyle().set("overflow", "auto");

        H2 title = new H2(news.getTitle());
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);

        Image image = new Image();
        if (StringUtils.hasText(news.getPhotoKey())) {
            image.setSrc(storageService.getSignedUrl(news.getPhotoKey()));
            image.setAlt(news.getTitle());
            image.getStyle().set("float", "left");
            image.addClassNames(LumoUtility.Margin.Right.AUTO,
                    LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.AlignSelf.CENTER
            );
            image.setWidth("300px");
            image.getStyle().set("object-fit", "cover");
            image.addClassNames(LumoUtility.BorderRadius.MEDIUM);
        } else {
            image.setVisible(false);
        }

        Span author = new Span("By " + news.getAuthor().getName());
        Span date = new Span(news.getPublishedAt().format(DateTimeFormatterUtils.dateTimeFormatter));
        Div metaInfo = new Div(author, date);
        metaInfo.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Gap.LARGE);

        Div content = new Div();
        content.getElement().setProperty("innerHTML", news.getContent().replace("\n", "<br>"));
        content.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.LineHeight.MEDIUM);

        card.add(title, metaInfo, image, content);
        return card;
    }
}
