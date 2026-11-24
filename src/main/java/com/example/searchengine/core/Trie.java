package com.example.searchengine.core;

import java.util.List;

/**
 * Standard trie (Section 23.6). Alphabet = {a..z, 0..9} in canonical order; each node keeps its
 * children in an array indexed by symbol. A node with a non-null occurrence list is an external
 * (word) node.
 */
public class Trie {
    public static final int SIGMA = 36;

    public static class Node {
        final Node[] child = new Node[SIGMA];
        OccurrenceList occurrences;   // non-null => a word ends here
    }

    private final Node root = new Node();
    private int wordCount = 0;
    private int nodeCount = 1;

    private static int idx(char c) { return c <= '9' ? c - '0' + 26 : c - 'a'; }
    private static char sym(int i) { return i >= 26 ? (char) ('0' + i - 26) : (char) ('a' + i); }

    /** Insert one occurrence of 'word' found in 'page'. O(m) for a word of length m. */
    public void insert(String word, String page) {
        Node v = root;
        for (int i = 0; i < word.length(); i++) {
            int k = idx(word.charAt(i));
            if (v.child[k] == null) { v.child[k] = new Node(); nodeCount++; }
            v = v.child[k];
        }
        if (v.occurrences == null) { v.occurrences = new OccurrenceList(); wordCount++; }
        v.occurrences.add(page);
    }

    /** Follow the path spelled by s; null if it does not exist. O(m). */
    public Node find(String s) {
        Node v = root;
        for (int i = 0; i < s.length(); i++) {
            v = v.child[idx(s.charAt(i))];
            if (v == null) return null;
        }
        return v;
    }

    /** Exact word lookup: occurrence list or null. */
    public OccurrenceList search(String word) {
        Node v = find(word);
        return v == null ? null : v.occurrences;
    }

    /** Prefix lookup: occurrence lists of every word starting with prefix (DFS of the subtree). */
    public void collectWithPrefix(String prefix, List<OccurrenceList> lists) {
        Node v = find(prefix);
        if (v != null) collect(v, lists);
    }

    private void collect(Node v, List<OccurrenceList> lists) {
        if (v.occurrences != null) lists.add(v.occurrences);
        for (int i = 0; i < SIGMA; i++) {
            if (v.child[i] != null) collect(v.child[i], lists);
        }
    }

    // ------------------------------------------------------------------
    // DELETE operations
    // ------------------------------------------------------------------

    /**
     * Delete a word from the trie. The word's node stops being an external node and every node that
     * is no longer needed (no children, not a word) is pruned on the way back up. O(m).
     * Returns true if the word existed.
     */
    public boolean deleteWord(String word) {
        if (word.isEmpty() || search(word) == null) return false;
        deleteWord(root, word, 0);
        wordCount--;
        return true;
    }

    /** Recursive helper. Returns true if node v can be removed from its parent. */
    private boolean deleteWord(Node v, String word, int depth) {
        if (depth == word.length()) {
            v.occurrences = null;                          // no longer a word
        } else {
            int k = idx(word.charAt(depth));
            if (deleteWord(v.child[k], word, depth + 1)) { // child became useless -> prune it
                v.child[k] = null;
                nodeCount--;
            }
        }
        return v != root && v.occurrences == null && !hasChildren(v);
    }

    /**
     * Remove every occurrence of a page from the index (used when a page is deleted).
     * Words that occurred only on that page disappear from the trie. DFS over the whole trie, O(N).
     * Returns the number of words that were removed completely.
     */
    public int removePage(String page) {
        int[] removed = {0};
        removePage(root, page, removed);
        wordCount -= removed[0];
        return removed[0];
    }

    private boolean removePage(Node v, String page, int[] removed) {
        if (v.occurrences != null) {
            v.occurrences.remove(page);
            if (v.occurrences.isEmpty()) { v.occurrences = null; removed[0]++; }
        }
        for (int i = 0; i < SIGMA; i++) {
            if (v.child[i] != null && removePage(v.child[i], page, removed)) {
                v.child[i] = null;
                nodeCount--;
            }
        }
        return v != root && v.occurrences == null && !hasChildren(v);
    }

    private static boolean hasChildren(Node v) {
        for (Node c : v.child) if (c != null) return true;
        return false;
    }

    public int getWordCount() { return wordCount; }
    public int getNodeCount() { return nodeCount; }
}
