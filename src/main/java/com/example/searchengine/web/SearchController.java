package com.example.searchengine.web;

import com.example.searchengine.core.QueryOutcome;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API
 *   GET    /api/search?q=...        SEARCH
 *   POST   /api/index  name,text    ADD a page (replaces a page with the same name)
 *   DELETE /api/index/{name}        DELETE a page
 *   DELETE /api/word/{word}         DELETE an index term
 *   GET    /api/stats               pages, term/node counts, crawl log
 */
@RestController
@RequestMapping("/api")
public class SearchController {
    private final SearchService service;

    public SearchController(SearchService service) { this.service = service; }

    @GetMapping("/search")
    public QueryOutcome search(@RequestParam(name = "q", defaultValue = "") String q) {
        return service.search(q);
    }

    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> add(@RequestParam String name, @RequestParam String text) {
        try {
            int n = service.getIndex().addPage(name, text);
            return ResponseEntity.ok(Map.of("message", "Page '" + name.trim() + "' indexed (" + n + " terms)."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/index/{name}")
    public ResponseEntity<Map<String, Object>> deletePage(@PathVariable String name) {
        boolean ok = service.getIndex().deletePage(name);
        return ok ? ResponseEntity.ok(Map.of("message", "Page '" + name + "' deleted."))
                  : ResponseEntity.status(404).body(Map.of("message", "Page '" + name + "' not found."));
    }

    @DeleteMapping("/word/{word}")
    public ResponseEntity<Map<String, Object>> deleteWord(@PathVariable String word) {
        boolean ok = service.getIndex().deleteWord(word);
        return ok ? ResponseEntity.ok(Map.of("message", "Word '" + word + "' deleted."))
                  : ResponseEntity.status(404).body(Map.of("message", "Word '" + word + "' not found."));
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pages", service.getIndex().getPages());
        m.put("distinctTerms", service.getIndex().getWordCount());
        m.put("trieNodes", service.getIndex().getNodeCount());
        m.put("crawlLog", service.getIndex().getCrawlLog());
        return m;
    }
}
