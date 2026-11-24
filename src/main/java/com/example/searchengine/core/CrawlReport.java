package com.example.searchengine.core;

import java.util.List;
import java.util.Map;

/** Outcome of a crawl: pages in crawl order, in-link counts per page, and a human-readable log. */
public record CrawlReport(List<String> pages, Map<String, Integer> inLinks, List<String> log) { }
