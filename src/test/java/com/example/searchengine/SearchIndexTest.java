package com.example.searchengine;

import static org.junit.jupiter.api.Assertions.*;

import com.example.searchengine.core.QueryOutcome;
import com.example.searchengine.core.SearchIndex;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Boundary-condition tests; run with:  mvn test */
class SearchIndexTest {
    static final SearchIndex index = new SearchIndex();

    @BeforeAll
    static void crawl() { index.build(Paths.get("site/index.html")); }

    @Test void crawlsReachablePagesOnly() {
        assertEquals(8, index.getPages().size());
        assertTrue(index.search("quantum").results().isEmpty());     // unlinked.html unreachable
    }
    @Test void normalQuery() {
        assertEquals("hashing.html", index.search("hash").results().get(0).getPage());
    }
    @Test void emptyQuery() { assertTrue(index.search("   ").results().isEmpty()); }
    @Test void nullQuery() { assertTrue(index.search(null).results().isEmpty()); }
    @Test void onlyStopWords() {
        QueryOutcome o = index.search("the of and");
        assertTrue(o.terms().isEmpty());
        assertEquals(3, o.ignoredStopWords().size());
    }
    @Test void caseAndPunctuation() { assertEquals(1, index.search("THE Trie!").results().size()); }
    @Test void duplicateTermsCountedOnce() { assertEquals(1, index.search("hash hash HASH").terms().size()); }
    @Test void unknownWord() { assertTrue(index.search("xyzzy").results().isEmpty()); }
    @Test void prefixQueries() {
        assertEquals(5, index.search("tre*").results().size());
        assertTrue(index.search("zzz*").results().isEmpty());
        assertTrue(index.search("*").results().isEmpty());
    }
    @Test void missingSeed() {
        SearchIndex empty = new SearchIndex();
        empty.build(Paths.get("nope/none.html"));
        assertTrue(empty.search("tree").results().isEmpty());
    }

    // ---------- ADD / DELETE (each test uses its own index so tests stay independent) ----------
    private static SearchIndex fresh() { SearchIndex i = new SearchIndex(); i.build(Paths.get("site/index.html")); return i; }

    @Test void addPageThenSearch() {
        SearchIndex i = fresh();
        assertEquals(4, i.addPage("stacks.html", "A stack is last in first out lifo"));
        assertEquals("stacks.html", i.search("lifo").results().get(0).getPage());
    }
    @Test void addBlankRejected() {
        SearchIndex i = fresh();
        assertThrows(IllegalArgumentException.class, () -> i.addPage("", "text"));
        assertThrows(IllegalArgumentException.class, () -> i.addPage("x.html", "  "));
    }
    @Test void addSamePageReplaces() {
        SearchIndex i = fresh();
        i.addPage("p.html", "zebra");
        i.addPage("p.html", "giraffe");
        assertTrue(i.search("zebra").results().isEmpty());
        assertEquals(1, i.search("giraffe").results().size());
    }
    @Test void deletePageRemovesItsWords() {
        SearchIndex i = fresh();
        assertTrue(i.deletePage("hashing.html"));
        assertTrue(i.search("hash").results().isEmpty());                 // only hashing.html had "hash"
        assertFalse(i.search("trees").results().stream().anyMatch(r -> r.getPage().equals("hashing.html")));
        assertFalse(i.deletePage("hashing.html"));                        // already gone
    }
    @Test void deleteUnknownPage() { assertFalse(fresh().deletePage("nope.html")); }
    @Test void deleteWordPrunesTrie() {
        SearchIndex i = fresh();
        int words = i.getWordCount(), nodes = i.getNodeCount();
        assertTrue(i.deleteWord("trie"));
        assertTrue(i.search("trie").results().isEmpty());
        assertEquals(words - 1, i.getWordCount());
        assertTrue(i.getNodeCount() < nodes);
        assertFalse(i.deleteWord("trie"));
        assertFalse(i.deleteWord(""));
    }
    @Test void deleteWordKeepsLongerWordsWithSamePrefix() {
        SearchIndex i = fresh();
        i.addPage("t.html", "tree treehouse");
        assertTrue(i.deleteWord("tree"));
        assertEquals(1, i.search("treehouse").results().size());          // 'treehouse' still intact
    }
}
