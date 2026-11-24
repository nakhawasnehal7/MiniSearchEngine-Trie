package com.example.searchengine.web;

import com.example.searchengine.core.QueryOutcome;
import com.example.searchengine.core.SearchIndex;
import jakarta.annotation.PostConstruct;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Spring bean that crawls the site once at startup and serves queries from the in-memory trie. */
@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    @Value("${search.seed}")
    private String seed;

    private final SearchIndex index = new SearchIndex();

    @PostConstruct
    public void init() {
        log.info("Crawling from {}", seed);
        index.build(Paths.get(seed));
        index.getCrawlLog().forEach(line -> log.info("  [crawler] {}", line));
        log.info("Crawl finished: {} pages, {} distinct index terms, {} trie nodes",
                 index.getPages().size(), index.getWordCount(), index.getNodeCount());
    }

    public QueryOutcome search(String q) { return index.search(q); }

    public SearchIndex getIndex() { return index; }
}
