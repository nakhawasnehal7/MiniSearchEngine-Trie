Search Engine Project – Tries (Textbook Section 23.6, "Search Engine")
A simplified search engine for the pages of a small web site, written in Java 17 + Spring Boot (Maven).
It crawls the site by following hyperlinks, parses every page with jsoup, stores all index terms
(every word except stop words) in a standard trie used as an inverted file, and answers ranked queries.
It can be used from a web page or a REST API.
---
1. How to run
   Requirements: JDK 17 or newer, Maven 3.6+, internet access the first time (Maven downloads Spring Boot and jsoup).
   mvn spring-boot:run          # start the application
   mvn test                     # run the boundary-condition tests
   mvn clean package && java -jar target/searchengine-1.0.0.jar     # alternative

Run the commands from the project root (the folder containing `pom.xml` and `site/`).
Then open http://localhost:8282.
The site to crawl is set in `src/main/resources/application.properties`:
search.seed=site/index.html

Endpoint	Purpose
`GET /api/search?q=heap sort`	search (add `*` to a word for prefix search, e.g. `tre*`)
`POST /api/index` (fields `name`, `text`)	add a page (same name = replace)
`DELETE /api/index/{name}`	delete a page
`DELETE /api/word/{word}`	delete an index term
`GET /api/stats`	pages, number of terms/nodes, crawl log
---
2. Files in the submission
   File / folder	Purpose
   `README.md`	this description
   `pom.xml`	Maven build (Spring Boot, jsoup)
   `src/main/java/.../SearchEngineApplication.java`	Spring Boot entry point
   `src/main/java/.../web/`	`SearchService` (crawls at startup), `SearchController` (REST API)
   `src/main/java/.../core/`	all search-engine logic (no Spring code): `Trie`, `OccurrenceList`, `WebCrawler`, `PageParser`, `Tokenizer`, `StopWords`, `SearchIndex`, `Ranker`, `SearchResult`, `QueryOutcome`, `CrawlReport`
   `src/main/resources/static/index.html`	browser interface (search, add, delete, status)
   `src/test/java/.../SearchIndexTest.java`	boundary-condition tests
   `site/*.html`	input: the sample web site (9 pages with hyperlinks)
   output file / screenshots	sample runs (see section 7)
---
3. Input: the sample site
   `site/` contains 9 small HTML pages about data structures: `index.html` (home), `trees`, `graphs`, `hashing`,
   `sorting`, `heaps`, `about`, `orphan` and `unlinked`. Pages link to each other, forming cycles
   (e.g. home -> trees -> home). Pages were deliberately built to test special cases:
   `graphs.html` links to `missing.html` (a broken link) and to an external URL;
   `orphan.html` contains only stop words and its title;
   `unlinked.html` is not linked from any page, so the crawler can never reach it;
   `index.html` -> `about.html` -> `orphan.html` shows crawling more than one level deep.
---
4. Approach (pipeline)
   Step 1 – Crawl
   `WebCrawler` treats the site as a directed graph (pages = vertices, hyperlinks = edges) and performs a
   breadth-first search from the seed page. A queue holds pages still to visit and a hash set
   holds pages already seen, so every page is processed once and cycles cannot loop forever.
   Relative links are resolved against the current page, `#fragments` are removed, external links
   (`http:`, `mailto:` ...) are ignored, and missing files are reported in the log and skipped.
   The crawler also counts, for every page, how many other pages link to it (in-links).
   Step 2 – Parse and clean
   `PageParser` uses jsoup: `Jsoup.parse(file)` builds the page, the title + body text give the words, and
   `select("a[href]")` gives the links. `Tokenizer` lower-cases the text and splits it on every character that
   is not a letter or digit. `StopWords` (a hash set of articles, prepositions, pronouns, auxiliary verbs, etc.)
   removes words that must not be index terms. Every remaining word of every page is an index term.
   Step 3 – Inverted file stored in a trie
   `Trie` is a standard trie as in Section 23.6: the alphabet is {a-z, 0-9} (36 symbols) in a fixed order;
   each node stores its children in an array indexed by symbol; the node where a word ends is the external
   node of that word. That node stores an occurrence list (`OccurrenceList`): a hash map
   page name -> number of times the word occurs in that page.
   root -> h -> e -> a -> p      { heaps.html: 4, sorting.html: 2, ... }
   \-> a -> s -> h     { hashing.html: 5 }

Words that start the same share nodes. One list exists per word and all pages that contain the word share it.
Operation	Method	Time
insert a word occurrence	`Trie.insert(word, page)`	O(m)
exact search	`Trie.search(word)`	O(m)
prefix search	`Trie.collectWithPrefix(prefix, lists)` – walk to the prefix node, DFS its subtree	O(m + size of subtree)
delete a word	`Trie.deleteWord(word)` – unmark the node, prune useless nodes on the way back	O(m)
delete a page	`Trie.removePage(page)` – DFS removing the page from every list, prune empty branches	O(N)
(m = length of the word, N = number of nodes.) By Theorem 23.6.1 a trie for strings of total length n has at
most n + 1 nodes. For the sample site: 105 distinct index terms stored in 465 nodes.
Step 4 – Answer a query
`SearchIndex.search(query)`:
lower-case, remove punctuation, split into terms; stop words are dropped (and reported); repeated terms count once;
for every term get its occurrence list(s) from the trie (one list for an exact word, many for a prefix term `xyz*`);
merge per page: number of distinct query terms found, total frequency, and in-links;
rank the pages (below).
Step 5 – Ranking
Pages are ordered by, in priority order:
number of distinct query terms contained in the page (more is better – a soft AND);
total frequency of the matched words in the page;
number of incoming hyperlinks (a page that many pages link to is considered more important);
page name (so the order is deterministic).
Sorting is done with a hand-written merge sort (`Ranker`), O(r log r) for r matching pages.
Step 6 – Add and delete
`addPage(name, text)`: tokenize, drop stop words, `Trie.insert` each word. Adding an existing name replaces the old page.
Pages added this way have no in-links and are kept in memory only.
`deletePage(name)` and `deleteWord(word)` as described in the table above.
All public methods of `SearchIndex` are `synchronized` because web requests run on several threads.
---
5. Data structures and algorithms used
   Used for	Data structure / algorithm
   Inverted index	Trie (own implementation) with occurrence lists
   Occurrence list, in-link counts	Hash map
   Visited pages, stop words	Hash set
   Crawl frontier	Queue
   Crawling	Breadth-first search
   Prefix collection, page deletion	Depth-first traversal of the trie
   Ranking	Merge sort
   Result/term lists	Array lists
   No other search engine or search library (e.g. Lucene) is used. jsoup is used only to parse HTML.
---
6. Boundary conditions tested
   Input	Expected behaviour
   empty query / spaces only	message "Empty query", no crash
   only stop words (`the of and`)	"No searchable terms", stop words listed as ignored
   mixed case + punctuation (`THE Trie!`)	stop word ignored, `trie` found
   repeated term (`hash hash HASH`)	counted once
   word in no page (`xyzzy`)	"No pages found"
   word only on an unreachable page (`quantum`)	"No pages found" (the crawler never reaches `unlinked.html`)
   prefix with no match (`zzz*`), bare `*`	no results / no searchable terms, no crash
   partial match (`hashing trees graphs heaps quantum`)	pages ranked by how many of the words they contain
   broken link, external link, link cycles	skipped / ignored / no infinite loop
   page made only of stop words	crawled, almost nothing indexed
   missing seed file	message in the log, empty index, no crash
   add with blank name or text	rejected with an error message
   add a page with an existing name	old entries removed, new ones indexed
   delete a page / word that does not exist	"not found" (HTTP 404)
   delete a word that is a prefix of another (`tree` vs `treehouse`)	longer word is kept
   delete every page	trie is empty again (root only)
   These cases are covered by `SearchIndexTest` (`mvn test`) and can be repeated by hand in the browser.
---
7. Sample run (output)
   Start the application and try these queries in the browser (or `GET /api/search?q=...`):
   hash            heap sort          search engine      tre*          graph*
   (empty)         the of and         THE Trie!          hash hash HASH
   xyzzy           quantum            zzz*               *

The crawl log printed at startup (lines `[crawler] <page>: <n> index terms, <k> links`) and screenshots of
the results table are included as the output of the project.
---
8. Limitations
   No stemming: `hash` does not match `hashing` (use `hash*`).
   Only local HTML files are crawled, not live internet pages.
   A stop word followed by `*` is dropped.
   The index is kept in memory; pages added or deleted through the interface are not saved when the application stops.