package com.smichelotti.mtg.scraper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsoupTest {

    private static final Logger log = LoggerFactory.getLogger(JsoupTest.class);

    public static void main(String[] args) throws Exception {

        String url = "https://www.cardkingdom.com/catalog/search?search=header&filter[name]=lightning+bolt";

        Connection connection = Jsoup.connect(url)
                .userAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/136.0.0.0 Safari/537.36")
                .header("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(30000)
                .followRedirects(true);

        Document document = connection.get();

        log.info("PAGE TITLE: {}", document.title());
    }
}
