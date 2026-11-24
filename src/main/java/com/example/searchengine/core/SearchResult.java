package com.example.searchengine.core;

/** One ranked page for a query. Getters are used by Jackson for the JSON response. */
public class SearchResult {
    private final String page;
    private int matchedTerms;     // distinct query terms found in the page
    private int frequency;        // total occurrences of the matched words
    private final int inLinks;    // number of pages that link to this page

    public SearchResult(String page, int inLinks) { this.page = page; this.inLinks = inLinks; }

    void addOccurrences(int n) { frequency += n; }
    void incrementMatchedTerms() { matchedTerms++; }

    public String getPage() { return page; }
    public int getMatchedTerms() { return matchedTerms; }
    public int getFrequency() { return frequency; }
    public int getInLinks() { return inLinks; }
}
