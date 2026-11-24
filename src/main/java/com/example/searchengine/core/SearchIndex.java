package com.example.searchengine.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade over the trie-based inverted file. Operations:
 *   build(seed)          crawl a site and index it
 *   addPage(name, text)  ADD   : index a page given as text
 *   search(query)        SEARCH: ranked search (exact words, or prefix with '*')
 *   deletePage(name)     DELETE: remove a page and all its index entries
 *   deleteWord(word)     DELETE: remove one index term completely
 */
public class SearchIndex {
    private final Trie trie = new Trie();
    private final Set<String> pages = new LinkedHashSet<>();            // all indexed pages
    private final Map<String, Integer> inLinks = new HashMap<>();       // page -> incoming links
    private List<String> crawlLog = new ArrayList<>();

    // ------------------------------------------------------------------ build / ADD
    public synchronized void build(Path seed) {
        if (!Files.exists(seed)) {
            crawlLog = new ArrayList<>(List.of("seed page not found: " + seed));
            return;
        }
        CrawlReport report = new WebCrawler(trie).crawl(seed);
        pages.addAll(report.pages());
        inLinks.putAll(report.inLinks());
        crawlLog = new ArrayList<>(report.log());
    }

    /**
     * ADD a page from plain text. If a page with this name already exists it is replaced
     * (old entries are removed first). Returns the number of index terms inserted.
     * Throws IllegalArgumentException for a blank name or blank text.
     */
    public synchronized int addPage(String name, String text) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("page name is empty");
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("page text is empty");
        name = name.trim();
        if (pages.contains(name)) trie.removePage(name);       // re-adding = replace
        int n = 0;
        for (String w : Tokenizer.tokenize(text)) {
            if (StopWords.isStopWord(w)) continue;
            trie.insert(w, name);                              // <-- trie insertion
            n++;
        }
        pages.add(name);
        return n;
    }

    // ------------------------------------------------------------------ DELETE
    /** DELETE a page: removes it from every word's occurrence list; words only it used disappear. */
    public synchronized boolean deletePage(String name) {
        if (name == null || !pages.remove(name.trim())) return false;
        trie.removePage(name.trim());
        inLinks.remove(name.trim());
        return true;
    }

    /** DELETE a single index term (e.g. to block a word). Returns true if it existed. */
    public synchronized boolean deleteWord(String word) {
        if (word == null) return false;
        List<String> toks = Tokenizer.tokenize(word);
        if (toks.size() != 1) return false;
        return trie.deleteWord(toks.get(0));
    }

    // ------------------------------------------------------------------ getters
    public synchronized List<String> getPages() { return new ArrayList<>(pages); }
    public synchronized List<String> getCrawlLog() { return new ArrayList<>(crawlLog); }
    public synchronized int getWordCount() { return trie.getWordCount(); }
    public synchronized int getNodeCount() { return trie.getNodeCount(); }

    // ------------------------------------------------------------------ SEARCH
    /** Answer a query. Terms ending in '*' are prefix queries. */
    public synchronized QueryOutcome search(String query) {
        List<String> ignored = new ArrayList<>();
        List<String> terms = new ArrayList<>();

        if (query == null || query.trim().isEmpty())
            return new QueryOutcome(query, "Empty query - please type a word.", ignored, terms, List.of());

        for (String raw : query.trim().split("\\s+")) {
            boolean star = raw.endsWith("*");
            for (String w : Tokenizer.tokenize(raw)) {
                if (StopWords.isStopWord(w)) { ignored.add(w); continue; }
                String term = star ? w + "*" : w;
                if (!terms.contains(term)) terms.add(term);        // duplicates counted once
            }
        }
        if (terms.isEmpty())
            return new QueryOutcome(query, "No searchable terms in the query.", ignored, terms, List.of());

        Map<String, SearchResult> byPage = new HashMap<>();
        for (String term : terms) {
            boolean prefix = term.endsWith("*");
            String t = prefix ? term.substring(0, term.length() - 1) : term;
            List<OccurrenceList> lists = new ArrayList<>();
            if (prefix) {
                trie.collectWithPrefix(t, lists);
            } else {
                OccurrenceList ol = trie.search(t);
                if (ol != null) lists.add(ol);
            }
            HashSet<String> seenForTerm = new HashSet<>();
            for (OccurrenceList ol : lists) {
                for (Map.Entry<String, Integer> e : ol.getCounts().entrySet()) {
                    SearchResult r = byPage.computeIfAbsent(e.getKey(),
                        p -> new SearchResult(p, inLinks.getOrDefault(p, 0)));
                    r.addOccurrences(e.getValue());
                    if (seenForTerm.add(e.getKey())) r.incrementMatchedTerms();
                }
            }
        }
        List<SearchResult> ranked = Ranker.rank(new ArrayList<>(byPage.values()));
        String msg = ranked.isEmpty() ? "No pages found." : ranked.size() + " page(s) found.";
        return new QueryOutcome(query, msg, ignored, terms, ranked);
    }
}
