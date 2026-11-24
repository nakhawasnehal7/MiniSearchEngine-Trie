package com.example.searchengine.core;

import java.util.ArrayList;
import java.util.List;

/** Splits text into lower-case alphanumeric tokens; any other character is a delimiter. */
public final class Tokenizer {
    private Tokenizer() { }

    public static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            char c = i < text.length() ? Character.toLowerCase(text.charAt(i)) : ' ';
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else if (sb.length() > 0) {
                out.add(sb.toString());
                sb.setLength(0);
            }
        }
        return out;
    }
}
