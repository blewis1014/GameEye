package edu.odu.cs411yellow.gameeyebackend.mainbackend.webscraping;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.odu.cs411yellow.gameeyebackend.mainbackend.models.NewsWebsite;
import edu.odu.cs411yellow.gameeyebackend.mainbackend.models.resources.Article;
import edu.odu.cs411yellow.gameeyebackend.mainbackend.repositories.NewsWebsiteRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UniversalScraper implements WebScraper {

    NewsWebsiteRepository newsWebsites; //Repository containing information for news websites to be used

    @Autowired
    public UniversalScraper(NewsWebsiteRepository newsWebsites) {
        this.newsWebsites = newsWebsites;
    }

    /**
     * Initiate the scrape
     *
     * @param newsOutlet    Name of news outlet
     * @return      List of Articles
     */
    @Override
    public List<Article> scrape(String newsOutlet) {
        List<Article> articles = new ArrayList<>();

        try {
            NewsWebsite newsSite = newsWebsites.findByName(newsOutlet);

            String url = newsSite.getRssFeedUrl();
            Document rssFeed = Jsoup.connect(url).get();    //Attempts to connect to URL and retrieve HTML file

            Elements items = rssFeed.select("item");    //Isolates each article as represented in HTML

            // Iterates through each HTML element, creates an Article object, then stores it 
            for (var i : items) {
                Article toAdd = createArticle(i, newsSite.getName());
                articles.add(toAdd);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return articles;
    }

    /*
    * Parses news article HTML into an Article object
    *
    * @param i      HTML element pulled from the RSS feed
    * @param websiteName    Website where the article was pulled
    * @return Article
    * @throws ParseException    If article could not be parsed
    */
    @Override
    public Article createArticle(Element i, String websiteName) throws ParseException {
        DateFormat format = new SimpleDateFormat("E, d MMMM yyyy kk:mm:ss z");
        String title = i.select("title").text();

        String url = i.select("link").text();

        String snippet;

        //parse article publication date
        String pubDate = i.select("pubDate").text();
        Date publicationDate;
        if (!pubDate.isEmpty()) {
            publicationDate = format.parse(pubDate);
        } else {
            publicationDate = new Date();
        }

        //parse text from article body
        if (websiteName.contentEquals("IGN")) {
            snippet = i.select("description").text();
        } else if (websiteName.contentEquals("PC Gamer")) {
            Document body = Jsoup.parse(i.selectFirst("title").nextElementSibling().text());
            Elements paragraph = body.select("p");
            snippet = paragraph.text();
        } else {
            Document body = Jsoup.parse(i.select("description").text());
            Elements paragraph = body.select("p");
            snippet = paragraph.text();
        }

        // Condenses text body to 255 character snippet for article preview
        if (snippet.length() > 255) {
            snippet = snippet.substring(0, 255);
        }

        Article article = new Article();
        article.setTitle(title);
        article.setUrl(url);
        article.setNewsWebsiteName(websiteName);
        article.setSnippet(snippet);
        article.setPublicationDate(publicationDate);
        article.setLastUpdated(publicationDate);
        article.setIsImportant(false);

        return article;
    }


    /**
     * Output to JSON format
     *
     * @return JSON
     */
    //@Override
    public String toString(String name) {
        ObjectMapper obj = new ObjectMapper();
        String articlesStr = "";
        List<Article> articles = scrape(name);

        for (Article a : articles) {
            try {
                String temp;
                temp = obj.writerWithDefaultPrettyPrinter().writeValueAsString(a);
                articlesStr = String.format("%1$s\n%2$s", articlesStr, temp);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return articlesStr;
    }
}
