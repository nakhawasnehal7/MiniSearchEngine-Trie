package com.example.searchengine.core;

import java.util.List;

/** Everything the UI/REST layer needs to show for one query. */
public record QueryOutcome(String query, String message, List<String> ignoredStopWords,
                           List<String> terms, List<SearchResult> results) { }
