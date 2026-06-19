package com.alphnology.views.news;

import com.alphnology.data.News;
import com.alphnology.infrastructure.storage.ObjectStorageService;
import com.alphnology.services.NewsService;
import com.alphnology.utils.DateTimeFormatterUtils;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.List;

@PageTitle("Noticias")
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
        header.addClassNames("public-page-header", LumoUtility.Margin.Bottom.LARGE);

        Div newsContainer = new Div();
        newsContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.LARGE,
                LumoUtility.MaxWidth.SCREEN_XLARGE
        );

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
        card.addClassName("news-card");

        // Body: flex row (image left + text right), collapses to column on mobile
        Div body = new Div();
        body.addClassNames(
                "news-card-body",
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.FlexDirection.Breakpoint.Medium.ROW
        );

        if (StringUtils.hasText(news.getPhotoKey())) {
            Div imageWrap = new Div();
            imageWrap.addClassName("news-image-wrap");
            Image image = new Image(storageService.getSignedUrl(news.getPhotoKey()), news.getTitle());
            image.addClassName("news-image");
            imageWrap.add(image);
            body.add(imageWrap);
        }

        // Text section
        H2 title = new H2(news.getTitle());
        title.addClassName("news-card-title");

        Span author = new Span("By " + news.getAuthor().getName());
        author.addClassName("news-meta-author");

        Div dot = new Div();
        dot.addClassName("news-meta-dot");

        Span date = new Span(news.getPublishedAt().format(DateTimeFormatterUtils.dateTimeFormatter));
        date.addClassName("news-meta-date");

        Div meta = new Div(author, dot, date);
        meta.addClassName("news-card-meta");

        Div content = new Div();
        content.getElement().setProperty("innerHTML", news.getContent().replace("\n", "<br>"));
        content.addClassName("news-card-content");

        Div textSection = new Div(title, meta, content);
        textSection.addClassName("news-text-section");

        body.add(textSection);
        card.add(body);
        return card;
    }
}
