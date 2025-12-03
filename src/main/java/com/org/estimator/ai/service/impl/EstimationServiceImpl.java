package com.org.estimator.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.dto.request.*;
import com.org.estimator.ai.dto.response.*;
import com.org.estimator.ai.service.EmbeddingService;
import com.org.estimator.ai.service.EstimationService;
import com.org.estimator.ai.service.LangService;
import com.org.estimator.ai.service.VectorDbService;
import com.org.estimator.ai.util.FileUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class EstimationServiceImpl implements EstimationService{

    // chunking, indexing
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;
    private static final int BATCH_UPSERT = 40;

    private final LangService langService;
    private final EmbeddingService embeddingService;
    private final VectorDbService vectorDbService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DocumentStore documentStore; // interface to map docId -> filePath, saved by upload controller


    public EstimationServiceImpl(LangService langService,
                                 EmbeddingService embeddingService,
                                 VectorDbService vectorDbService,
                                 DocumentStore documentStore) {
        this.langService = langService;
        this.embeddingService = embeddingService;
        this.vectorDbService = vectorDbService;
        this.documentStore = documentStore;
    }

    @Override
    public EstimateResponse estimate(EstimateRequest req) throws Exception {

        String description = req.getDescription();
        if (description == null || description.isBlank()) {
            File sourceFile = null;
            if (req.getUploadDocId() != null && !req.getUploadDocId().isBlank()) {
                var info = documentStore.get(req.getUploadDocId());
                if (info != null && info.filePath != null && !info.filePath.isBlank()) {
                    sourceFile = new File(info.filePath);
                }
            }
            if (sourceFile == null && req.getFilePath() != null && !req.getFilePath().isBlank()) {
                sourceFile = new File(req.getFilePath());
            }
            if (sourceFile != null && sourceFile.exists() && sourceFile.isFile()) {
                description = FileUtil.extractText(sourceFile);
            } else {
                throw new IllegalArgumentException("Either description, uploadDocId or filePath is required and file must exist");
            }
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Document contains no extractable text. Upload a text-based PDF/DOCX or provide description.");
        }

        // 2. Break into tasks
        String breakPrompt = langService.breakTasksPrompt(description);
        String breakResp = safeCallChat(breakPrompt);
        List<Map<String,Object>> tasks = parseListField(breakResp, "tasks");
        if (tasks.isEmpty()) {
            tasks = List.of(Map.of("module","General","feature","General","task","Implement features","complexity","Medium","role","Backend"));
        }

        // 3. Index document into vector DB (chunk + embed + upsert)
        String rdocId = req.getUploadDocId() == null ? "doc-inline-" + UUID.randomUUID().toString().substring(0,6) : req.getUploadDocId();
        String sourceUri = determineSourceUri(req, rdocId);
        indexDocumentIntoVectorDb(rdocId, description, sourceUri);

        // 4. Estimate effort
        String tasksJson = mapper.writeValueAsString(tasks);
        String estPrompt = langService.estimateEffortPrompt(tasksJson, req.getOptions() == null ? "" : mapper.writeValueAsString(req.getOptions()));
        String estResp = safeCallChat(estPrompt);
        List<Map<String,Object>> estimates = parseListField(estResp, "estimates");
        if (estimates.isEmpty()) {
            estimates = tasks.stream().map(t -> {
                Map<String,Object> e = new HashMap<>(t);
                e.put("hours", 16);
                e.put("confidence", 0.6);
                return e;
            }).collect(Collectors.toList());
        }

        // map to EffortEstimate
        List<EffortEstimate> effortList = new ArrayList<>();
        for (Map<String,Object> m : estimates) {
            EffortEstimate ee = new EffortEstimate();
            ee.setModule(asString(m.get("module")));
            ee.setFeature(asString(m.get("feature")));
            ee.setTask(asString(m.get("task")));
            ee.setRole(asString(m.get("role")));
            ee.setHours(asInt(m.get("hours")));
            ee.setConfidence(asDouble(m.get("confidence")));
            effortList.add(ee);
        }

        // 5. Resource plan & timeline
        String estimatesJson = mapper.writeValueAsString(estimates);
        String resourcePrompt = langService.resourcePlanPrompt(estimatesJson,
                req.getConstraints() == null ? "" : String.join(", ", req.getConstraints()),
                req.getDeadline());
        String resourceResp = safeCallChat(resourcePrompt);
        Map<String,Object> resourceMap = safeMap(resourceResp);

        Map<String, ResourcePlanEntry> resourcePlan = new HashMap<>();
        if (resourceMap.containsKey("resourcePlan")) {
            Map<String, Map<String,Object>> rp = (Map<String, Map<String,Object>>) resourceMap.get("resourcePlan");
            for (var en : rp.entrySet()) {
                var val = en.getValue();
                ResourcePlanEntry rpe = new ResourcePlanEntry();
                rpe.setEffortHours(asInt(val.get("effortHours")));
                rpe.setResourcesNeeded(asInt(val.get("resourcesNeeded")));
                rpe.setDurationWeeks(asInt(val.get("durationWeeks")));
                resourcePlan.put(en.getKey(), rpe);
            }
        }

        List<TimelineTask> timeline = new ArrayList<>();
        if (resourceMap.containsKey("timeline")) {
            var timeLine = (List<Map<String,Object>>) resourceMap.get("timeline");
            for (Map<String,Object> t : timeLine) {
                TimelineTask task = new TimelineTask();
                task.setId(asString(t.get("id")));
                task.setTask(asString(t.get("task")));
                task.setStart(asString(t.get("start")));
                task.setEnd(asString(t.get("end")));
                Object depends = t.get("dependsOn");
                if (depends instanceof List) task.setDependsOn((List<String>) depends);
                else task.setDependsOn(List.of());
                timeline.add(task);
            }
        }

        // 6. Assumptions & risks
        String summary = (req.getProjectName() == null ? "Project" : req.getProjectName()) + " - " +
                (description.length() > 200 ? description.substring(0,200) : description);
        String arPrompt = langService.assumptionsPrompt(summary);
        String arResp = safeCallChat(arPrompt);
        Map<String,Object> arMap = safeMap(arResp);
        List<String> assumptions = (List<String>) arMap.getOrDefault("assumptions", List.of());
        List<Map<String,Object>> risksRaw = (List<Map<String,Object>>) arMap.getOrDefault("risks", List.of());
        List<RiskEntry> risks = new ArrayList<>();
        for (Map<String,Object> risk : risksRaw) {
            RiskEntry riskEntry = new RiskEntry();
            riskEntry.setRisk(asString(risk.get("risk")));
            riskEntry.setImpact(asString(risk.get("impact")));
            riskEntry.setMitigation(asString(risk.get("mitigation")));
            risks.add(riskEntry);
        }

        EstimateResponse resp = new EstimateResponse();
        resp.setProjectId("proj-" + UUID.randomUUID().toString().substring(0,8));
        resp.setProjectName(req.getProjectName());
        resp.setSummary("Auto-generated estimate (review required)");
        resp.setEffortEstimates(effortList);
        resp.setResourcePlan(resourcePlan);
        resp.setTimeline(timeline);
        resp.setAssumptions(assumptions);
        resp.setRisks(risks);

        SourceInfo si = new SourceInfo();
        si.setDocId(rdocId);
        si.setSimilarity(1.0);
        resp.setSources(List.of(si));
        resp.setGeneratedAt(OffsetDateTime.now());
        return resp;
    }


    private String determineSourceUri(EstimateRequest req, String rdocId) {
        try {
            if (req.getUploadDocId() != null && !req.getUploadDocId().isBlank()) {
                var info = documentStore.get(req.getUploadDocId());
                if (info != null && info.filePath != null) {
                    return new File(info.filePath).toURI().toString();
                }
            } else if (req.getFilePath() != null && !req.getFilePath().isBlank()) {
                return new File(req.getFilePath()).toURI().toString();
            }
        } catch (Exception ignored) {}
        return "inline:" + rdocId;
    }

    private void indexDocumentIntoVectorDb(String rdocId, String text, String sourceUri) {
        if (text == null || text.isBlank()) return;
        List<String> chunks = chunkText(text, CHUNK_SIZE, CHUNK_OVERLAP);
        List<Map<String,Object>> batch = new ArrayList<>();
        int idx = 0;
        for (String chunk : chunks) {
            Map<String,Object> meta = new HashMap<>();
            meta.put("docId", rdocId);
            meta.put("chunkIndex", idx);
            meta.put("source", sourceUri);
            meta.put("snippet", chunk.length() > 200 ? chunk.substring(0,200) : chunk);
            meta.put("length", chunk.length());
            List<Double> emb;
            try {
                emb = embeddingService.createEmbedding(chunk);
            } catch (Exception ex) {
                // skip this chunk on embedding error
                idx++; continue;
            }
            Map<String,Object> item = new HashMap<>();
            item.put("id", rdocId + "-chunk-" + idx);
            item.put("values", emb);
            item.put("metadata", meta);
            batch.add(item);
            if (batch.size() >= BATCH_UPSERT) {
                try { vectorDbService.upsertBatch(batch, rdocId); } catch (Exception ignored) {}
                batch.clear();
            }
            idx++;
        }
        if (!batch.isEmpty()) {
            try { vectorDbService.upsertBatch(batch, rdocId); } catch (Exception ignored) {}
        }
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) chunks.add(chunk);
            start = end - overlap;
            if (start < 0) start = 0;
            if (start >= text.length()) break;
        }
        return chunks;
    }

    // safe call with retries
    private String safeCallChat(String prompt) throws Exception {
        int tries = 0;
        for (int i = 0; i <= tries; i++) {
            try {
                return langService.callChat(prompt);
            } catch (Exception ex) {
                if (i == tries) throw ex;
                Thread.sleep(1000L * (i+1));
            }
        }
        return "";
    }

    // robust JSON extraction/parsing (strip code fences and extract JSON block)
    private String extractJsonBlock(String content) {
        if (content == null) return null;
        String s = content.trim();
        // remove leading/trailing code fences
        if (s.startsWith("```")) {
            int last = s.lastIndexOf("```");
            if (last > 3) s = s.substring(3, last).trim();
            else s = s.replaceFirst("^```[\\w\\s]*\\n?", "");
        }
        int objStart = s.indexOf('{');
        int arrStart = s.indexOf('[');
        int start = -1;
        if (arrStart >=0 && (arrStart < objStart || objStart < 0)) start = arrStart;
        else start = objStart;
        if (start >= 0) {
            char open = s.charAt(start);
            char close = open == '{' ? '}' : ']';
            int depth = 0;
            for (int i = start; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) return s.substring(start, i+1);
                }
            }
        }
        return s;
    }

    private List<Map<String,Object>> parseListField(String content, String field) {
        String json = extractJsonBlock(content);
        if (json == null) return List.of();
        try {
            Map<String,Object> obj = mapper.readValue(json, new TypeReference<Map<String,Object>>() {});
            if (obj.containsKey(field) && obj.get(field) instanceof List) {
                return (List<Map<String,Object>>) obj.get(field);
            }
            return mapper.readValue(json, new TypeReference<List<Map<String,Object>>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String,Object> safeMap(String content) {
        String json = extractJsonBlock(content);
        if (json == null) return Map.of();
        try {
            return mapper.readValue(json, new TypeReference<Map<String,Object>>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String asString(Object o) { return o == null ? null : o.toString(); }
    private Integer asInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number)o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
    private Double asDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number)o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }
}
