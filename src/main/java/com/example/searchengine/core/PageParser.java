package com.example.searchengine.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Parses one HTML page with jsoup into its visible text and its outgoing hyperlinks.
 * jsoup builds a DOM tree of the page; we read the title + body text (script/style
 * content is not part of the text) and collect the href of every <a> tag.
 */
public class PageParser {
    private final String text;
    private final List<String> links = new ArrayList<>();

    /** Read and parse an HTML file. Throws IOException if the file cannot be read. */
    public static PageParser parse(File file) throws IOException {
        return new PageParser(Jsoup.parse(file, "UTF-8"));
    }

    private PageParser(Document doc) {
        this.text = doc.title() + " " + doc.body().text();     // visible words of the page
        for (Element a : doc.select("a[href]")) {              // every <a href="..."> tag
            links.add(a.attr("href"));                         // link target exactly as written
        }
    }

    public String getText() { return text; }
    public List<String> getLinks() { return links; }
}
