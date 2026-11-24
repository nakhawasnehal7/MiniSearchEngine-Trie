package com.example.searchengine.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranking criteria, in priority order:
 *  1. number of DISTINCT query terms contained in the page (soft AND),
 *  2. total frequency of the matched words,
 *  3. number of incoming hyperlinks (importance),
 *  4. page name (deterministic tie-break).
 * Sorting uses the textbook merge sort, O(r log r).
 */
public final class Ranker {
    private Ranker() { }

    /** true if a ranks before b. */
    static boolean better(SearchResult a, SearchResult b) {
        if (a.getMatchedTerms() != b.getMatchedTerms()) return a.getMatchedTerms() > b.getMatchedTerms();
        if (a.getFrequency() != b.getFrequency()) return a.getFrequency() > b.getFrequency();
        if (a.getInLinks() != b.getInLinks()) return a.getInLinks() > b.getInLinks();
        return a.getPage().compareTo(b.getPage()) < 0;
    }

    public static List<SearchResult> rank(List<SearchResult> list) {
        if (list.size() <= 1) return list;
        int mid = list.size() / 2;
        List<SearchResult> a = rank(new ArrayList<>(list.subList(0, mid)));
        List<SearchResult> b = rank(new ArrayList<>(list.subList(mid, list.size())));
        List<SearchResult> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) out.add(better(b.get(j), a.get(i)) ? b.get(j++) : a.get(i++));
        while (i < a.size()) out.add(a.get(i++));
        while (j < b.size()) out.add(b.get(j++));
        return out;
    }
}
