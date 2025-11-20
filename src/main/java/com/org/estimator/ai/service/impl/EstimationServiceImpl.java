package com.org.estimator.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.dto.request.*;
import com.org.estimator.ai.dto.response.*;
import com.org.estimator.ai.service.EstimationService;
import com.org.estimator.ai.service.LangService;
import com.org.estimator.ai.service.VectorDbService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;


@Service
public class EstimationServiceImpl implements EstimationService{

    private final LangService langService;
    private final DocumentStore documentStore;
    private final EmbeddingService embeddingService;
    private final VectorDbService vectorDbService;
    private final ObjectMapper mapper = new ObjectMapper();

    public EstimationServiceImpl(LangService langService, DocumentStore documentStore, EmbeddingService embeddingService, VectorDbService vectorDbService) {
        this.langService = langService;
        this.documentStore = documentStore;
        this.embeddingService = embeddingService;
        this.vectorDbService = vectorDbService;
    }


    @Override
    public EstimateResponse estimate(EstimateRequest req) throws Exception {

        String description = req.getDescription();
        if ((description == null || description.isBlank()) && req.getUploadDocId() != null) {
            var info = documentStore.get(req.getUploadDocId());
            if (info != null) description = info.extractedSummary;
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Either description or uploadDocId required");
        }

        // 2) break into tasks
        String breakPrompt = langService.breakTasksPrompt(description);
        String breakResp = langService.callChat(breakPrompt);
        List<Map<String,Object>> tasks = parseListField(breakResp, "tasks");
        if (tasks.isEmpty()) {
            tasks = List.of(Map.of("module","General","feature","General","task","Implement features","complexity","Medium","role","Backend"));
        }

        // 3) create embeddings & upsert chunks (optional RAG)
        // Create one large chunk for now (you can chunk and upsert each chunk)
        List<Double> emb = embeddingService.createEmbedding(description);
        String rdocId = req.getUploadDocId() == null ? "doc-inline-"+UUID.randomUUID().toString().substring(0,6) : req.getUploadDocId();
        vectorDbService.upsertVector(rdocId + "-chunk-0", emb, description, rdocId);

        // 4) estimate effort
        String tasksJson = mapper.writeValueAsString(tasks);
        String estPrompt = langService.estimateEffortPrompt(tasksJson, req.getOptions() == null ? "" : mapper.writeValueAsString(req.getOptions()));
        String estResp = langService.callChat(estPrompt);
        List<Map<String,Object>> estimates = parseListField(estResp, "estimates");
        if (estimates.isEmpty()) {
            // fallback: naive hours
            estimates = new ArrayList<>();
            for (Map<String,Object> t : tasks) {
                Map<String,Object> e = new HashMap<>(t);
                e.put("hours", 16);
                e.put("confidence", 0.6);
                estimates.add(e);
            }
        }
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

        // 5) resource plan & timeline
        String estimatesJson = mapper.writeValueAsString(estimates);
        String resourcePrompt = langService.resourcePlanPrompt(estimatesJson,
                req.getConstraints() == null ? "" : String.join(", ", req.getConstraints()),
                req.getDeadline());
        String resourceResp = langService.callChat(resourcePrompt);
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
            var tl = (List<Map<String,Object>>) resourceMap.get("timeline");
            for (Map<String,Object> t : tl) {
                TimelineTask tt = new TimelineTask();
                tt.setId(asString(t.get("id")));
                tt.setTask(asString(t.get("task")));
                tt.setStart(asString(t.get("start")));
                tt.setEnd(asString(t.get("end")));
                Object depends = t.get("dependsOn");
                if (depends instanceof List) tt.setDependsOn((List<String>) depends);
                else tt.setDependsOn(List.of());
                timeline.add(tt);
            }
        }

        String summary = (req.getProjectName() == null ? "Project" : req.getProjectName()) + " - " + (description.length() > 200 ? description.substring(0,200) : description);
        String arPrompt = langService.assumptionsPrompt(summary);
        String arResp = langService.callChat(arPrompt);
        Map<String,Object> arMap = safeMap(arResp);
        List<String> assumptions = (List<String>) arMap.getOrDefault("assumptions", List.of());
        List<Map<String,Object>> risksRaw = (List<Map<String,Object>>) arMap.getOrDefault("risks", List.of());
        List<RiskEntry> risks = new ArrayList<>();
        for (Map<String,Object> r : risksRaw) {
            RiskEntry rr = new RiskEntry();
            rr.setRisk(asString(r.get("risk")));
            rr.setImpact(asString(r.get("impact")));
            rr.setMitigation(asString(r.get("mitigation")));
            risks.add(rr);
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
        // sources: we created one upsert above
        SourceInfo si = new SourceInfo();
        si.setDocId(rdocId);
        si.setSimilarity(1.0);
        resp.setSources(List.of(si));
        resp.setGeneratedAt(OffsetDateTime.now());
        return resp;
    }

    private List<Map<String,Object>> parseListField(String json, String field) {
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

    private Map<String,Object> safeMap(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Map<String,Object>>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String asString(Object o) { return o == null ? null : o.toString(); }
    private Integer asInt(Object o) { return o == null ? 0 : ((Number)o).intValue(); }
    private Double asDouble(Object o) { return o == null ? 0.0 : ((Number)o).doubleValue(); }
}
