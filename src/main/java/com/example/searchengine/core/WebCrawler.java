package com.example.searchengine.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Web crawler: breadth-first search of the site graph (pages = vertices, hyperlinks = edges).
 * Queue = frontier, hash set = visited, so cycles cannot cause infinite loops.
 * Every non-stop word of every visited page is inserted into the trie.
 */
public class WebCrawler {
    private final Trie trie;

    public WebCrawler(Trie trie) { this.trie = trie; }

    public CrawlReport crawl(Path seed) {
        List<String> pages = new ArrayList<>();
        Map<String, Integer> inLinks = new HashMap<>();
        List<String> log = new ArrayList<>();

        Queue<Path> frontier = new ArrayDeque<>();
        Set<Path> visited = new HashSet<>();
        Path start = seed.toAbsolutePath().normalize();
        frontier.add(start);
        visited.add(start);

        while (!frontier.isEmpty()) {
            Path page = frontier.remove();
            String name = page.getFileName().toString();
            if (!Files.exists(page)) {                       // boundary: broken link
                log.add("broken link, skipped: " + name);
                continue;
            }
            PageParser parser;
            try {
                parser = PageParser.parse(page.toFile());     // jsoup reads and parses the file
            } catch (Exception e) {
                log.add("cannot parse " + name + ": " + e.getMessage());
                continue;
            }
            pages.add(name);

            int indexed = 0;
            for (String w : Tokenizer.tokenize(parser.getText())) {
                if (StopWords.isStopWord(w)) continue;        // excluded index terms
                trie.insert(w, name);
                indexed++;
            }
            log.add(name + ": " + indexed + " index terms, " + parser.getLinks().size() + " links");

            for (String href : parser.getLinks()) {
                if (href.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) continue;   // external (http:, mailto:)
                int hash = href.indexOf('#');                                   // strip fragment
                if (hash >= 0) href = href.substring(0, hash);
                if (href.isEmpty()) continue;
                Path target = page.getParent().resolve(href).normalize();
                inLinks.merge(target.getFileName().toString(), 1, Integer::sum);
                if (visited.add(target)) frontier.add(target);
            }
        }
        return new CrawlReport(pages, inLinks, log);
    }
}
