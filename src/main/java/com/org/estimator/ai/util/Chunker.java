package com.org.estimator.ai.util;

import java.util.ArrayList;
import java.util.List;

public class Chunker {

    public static List<String> chunkText(String text, int wordsPerChunk) {
        if (text == null) return List.of();
        String[] tokens = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String t : tokens) {
            sb.append(t).append(" ");
            count++;
            if (count >= wordsPerChunk) {
                chunks.add(sb.toString().trim());
                sb = new StringBuilder();
                count = 0;
            }
        }
        if (sb.length() > 10) chunks.add(sb.toString().trim());
        return chunks;
    }
}
