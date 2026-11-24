package com.example.searchengine.core;

import java.util.HashMap;
import java.util.Map;

/** Stored at the external node of a word in the trie: page name -> number of occurrences of the word. */
public class OccurrenceList {
    private final Map<String, Integer> counts = new HashMap<>();

    public void add(String page) { counts.merge(page, 1, Integer::sum); }

    /** Remove a page from this word's list. Returns true if the page was present. */
    public boolean remove(String page) { return counts.remove(page) != null; }

    public boolean isEmpty() { return counts.isEmpty(); }

    public Map<String, Integer> getCounts() { return counts; }
}
