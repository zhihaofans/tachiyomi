package eu.kanade.mangafeed.data.source.online.english;

import android.content.Context;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.kanade.mangafeed.data.database.models.Chapter;
import eu.kanade.mangafeed.data.database.models.Manga;
import eu.kanade.mangafeed.data.source.SourceManager;
import eu.kanade.mangafeed.data.source.base.Source;
import eu.kanade.mangafeed.data.source.model.MangasPage;

public class Mangafox extends Source {

    public static final String NAME = "Mangafox (EN)";
    public static final String BASE_URL = "http://mangafox.me";
    public static final String INITIAL_POPULAR_MANGAS_URL = "http://mangafox.me/directory/";
    public static final String INITIAL_SEARCH_URL =
            "http://mangafox.me/search.php?name_method=cw&advopts=1&order=az&sort=name";

    public Mangafox(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getSourceId() {
        return SourceManager.MANGAFOX;
    }

    @Override
    public boolean isLoginRequired() {
        return false;
    }

    @Override
    protected String getInitialPopularMangasUrl() {
        return INITIAL_POPULAR_MANGAS_URL;
    }

    @Override
    protected String getInitialSearchUrl(String query) {
        return INITIAL_SEARCH_URL + "&name=" + query + "&page=1";
    }

    @Override
    protected List<Manga> parsePopularMangasFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        Elements mangaHtmlBlocks = parsedHtml.select("div#mangalist > ul.list > li");
        for (Element currentHtmlBlock : mangaHtmlBlocks) {
            Manga currentManga = constructPopularMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }

        return mangaList;
    }

    private Manga constructPopularMangaFromHtmlBlock(Element htmlBlock) {
        Manga mangaFromHtmlBlock = new Manga();
        mangaFromHtmlBlock.source = getSourceId();

        Element urlElement = htmlBlock.select("a.title").first();

        if (urlElement != null) {
            mangaFromHtmlBlock.url = urlElement.attr("href");
            mangaFromHtmlBlock.title = urlElement.text();
        }

        return mangaFromHtmlBlock;
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        Element next = parsedHtml.select("a:has(span.next)").first();
        if (next == null)
            return null;

        return INITIAL_POPULAR_MANGAS_URL + next.attr("href");
    }

    @Override
    protected List<Manga> parseSearchFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        Elements mangaHtmlBlocks = parsedHtml.select("table#listing > tbody > tr:gt(0)");
        for (Element currentHtmlBlock : mangaHtmlBlocks) {
            Manga currentManga = constructSearchMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }

        return mangaList;
    }

    private Manga constructSearchMangaFromHtmlBlock(Element htmlBlock) {
        Manga mangaFromHtmlBlock = new Manga();
        mangaFromHtmlBlock.source = getSourceId();

        Element urlElement = htmlBlock.select("a.series_preview").first();

        if (urlElement != null) {
            mangaFromHtmlBlock.url = urlElement.attr("href");
            mangaFromHtmlBlock.title = urlElement.text();
        }

        return mangaFromHtmlBlock;
    }

    @Override
    protected String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query) {
        Element next = parsedHtml.select("a:has(span.next)").first();
        if (next == null)
            return null;

        return BASE_URL + next.attr("href");
    }

    @Override
    protected Manga parseHtmlToManga(String mangaUrl, String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        Element infoElement = parsedDocument.select("div#title").first();
        Element titleElement = infoElement.select("h2 > a").first();
        Element rowElement = infoElement.select("table > tbody > tr:eq(1)").first();
        Element authorElement = rowElement.select("td:eq(1)").first();
        Element artistElement = rowElement.select("td:eq(2)").first();
        Element genreElement = rowElement.select("td:eq(3)").first();
        Element descriptionElement = infoElement.select("p.summary").first();
        Element thumbnailUrlElement = parsedDocument.select("div.cover > img").first();

        Manga newManga = new Manga();
        newManga.url = mangaUrl;

        if (titleElement != null) {
            String title = titleElement.text();
            // Strip the last word
            title = title.substring(0, title.lastIndexOf(" "));
            newManga.title = title;
        }
        if (artistElement != null) {
            String fieldArtist = artistElement.text();
            newManga.artist = fieldArtist;
        }
        if (authorElement != null) {
            String fieldAuthor = authorElement.text();
            newManga.author = fieldAuthor;
        }
        if (descriptionElement != null) {
            String fieldDescription = descriptionElement.text();
            newManga.description = fieldDescription;
        }
        if (genreElement != null) {
            String fieldGenre = genreElement.text();
            newManga.genre = fieldGenre;
        }
        if (thumbnailUrlElement != null) {
            String fieldThumbnailUrl = thumbnailUrlElement.attr("src");
            newManga.thumbnail_url = fieldThumbnailUrl;
        }
//        if (statusElement != null) {
//            boolean fieldCompleted = statusElement.text().contains("Completed");
//            newManga.status = fieldCompleted + "";
//        }

        newManga.initialized = true;

        return newManga;
    }

    @Override
    protected List<Chapter> parseHtmlToChapters(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<Chapter> chapterList = new ArrayList<Chapter>();

        Elements chapterElements = parsedDocument.select("div#chapters li div");
        for (Element chapterElement : chapterElements) {
            Chapter currentChapter = constructChapterFromHtmlBlock(chapterElement);

            chapterList.add(currentChapter);
        }

        return chapterList;
    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter newChapter = Chapter.create();

        Element urlElement = chapterElement.select("a.tips").first();
        Element nameElement = chapterElement.select("a.tips").first();
        Element dateElement = chapterElement.select("span.date").first();

        if (urlElement != null) {
            newChapter.url = urlElement.attr("href");
        }
        if (nameElement != null) {
            newChapter.name = nameElement.text();
        }
        if (dateElement != null) {
            newChapter.date_upload = parseUpdateFromElement(dateElement);
        }

        newChapter.date_fetch = new Date().getTime();

        return newChapter;
    }

    private long parseUpdateFromElement(Element updateElement) {
        String updatedDateAsString = updateElement.text();

        if (updatedDateAsString.contains("Today")) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            try {
                Date withoutDay = new SimpleDateFormat("h:mm a", Locale.ENGLISH).parse(updatedDateAsString.replace("Today", ""));
                return today.getTimeInMillis() + withoutDay.getTime();
            } catch (ParseException e) {
                return today.getTimeInMillis();
            }
        } else if (updatedDateAsString.contains("Yesterday")) {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            yesterday.set(Calendar.HOUR_OF_DAY, 0);
            yesterday.set(Calendar.MINUTE, 0);
            yesterday.set(Calendar.SECOND, 0);
            yesterday.set(Calendar.MILLISECOND, 0);

            try {
                Date withoutDay = new SimpleDateFormat("h:mm a", Locale.ENGLISH).parse(updatedDateAsString.replace("Yesterday", ""));
                return yesterday.getTimeInMillis() + withoutDay.getTime();
            } catch (ParseException e) {
                return yesterday.getTimeInMillis();
            }
        } else {
            try {
                Date specificDate = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(updatedDateAsString);

                return specificDate.getTime();
            } catch (ParseException e) {
                // Do Nothing.
            }
        }

        return 0;
    }

    @Override
    protected List<String> parseHtmlToPageUrls(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<String> pageUrlList = new ArrayList<>();

        Elements pageUrlElements = parsedDocument.select("select.m").first().select("option:not([value=0])");
        String baseUrl = parsedDocument.select("div#series a").first().attr("href").replace("1.html", "");
        int counter = 1;
        for (Element pageUrlElement : pageUrlElements) {
            if(counter < pageUrlElements.size()) {
                pageUrlList.add(baseUrl + pageUrlElement.attr("value") + ".html");
            }
            counter++;
        }

        return pageUrlList;
    }

    @Override
    protected String parseHtmlToImageUrl(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        Element imageElement = parsedDocument.getElementById("image");
        return imageElement.attr("src");
    }
}
