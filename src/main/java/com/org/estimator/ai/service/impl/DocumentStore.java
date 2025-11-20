package com.org.estimator.ai.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentStore {
    public static class DocInfo {
        public String docId;
        public String filename;
        public String status; // uploaded | processing | ready
        public String extractedSummary;
    }

    private final Map<String,DocInfo> map = new ConcurrentHashMap<>();
    public void put(DocInfo d) { map.put(d.docId, d); }
    public DocInfo get(String docId) { return map.get(docId); }

}
