package com.example.searchengine.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Words (articles, prepositions, pronouns, auxiliaries...) that are NOT index terms. Hash set: O(1) lookup. */
public final class StopWords {
    private static final Set<String> WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "the", "of", "in", "on", "at", "to", "for", "from", "by", "with", "about",
        "as", "into", "over", "under", "between", "and", "or", "but", "nor", "not", "so",
        "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
        "my", "your", "his", "its", "our", "their", "this", "that", "these", "those",
        "is", "are", "was", "were", "be", "been", "am", "do", "does", "did", "has", "have",
        "had", "will", "would", "can", "could", "also", "such", "too", "than", "then",
        "there", "which", "who", "what", "when", "where", "how", "if", "up", "out"));

    private StopWords() { }

    public static boolean isStopWord(String word) { return WORDS.contains(word); }
}
