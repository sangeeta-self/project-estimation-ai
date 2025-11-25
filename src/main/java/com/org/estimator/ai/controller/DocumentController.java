package com.org.estimator.ai.controller;


import com.org.estimator.ai.config.AppConfig;
import com.org.estimator.ai.dto.request.EstimateRequest;
import com.org.estimator.ai.dto.response.EstimateResponse;
import com.org.estimator.ai.service.EstimationService;
import com.org.estimator.ai.service.impl.DocumentStore;
import com.org.estimator.ai.service.impl.EmbeddingService;
import com.org.estimator.ai.util.FileUtil;
import com.org.estimator.ai.util.ResultFileStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {


    private final EstimationService estimationService;
    private final DocumentStore documentStore;
    private final EmbeddingService embeddingService;
    private final ResultFileStore resultFileStore;
    private final String uploadDir;



    public DocumentController(EstimationService estimationService, DocumentStore documentStore,
                              EmbeddingService embeddingService, ResultFileStore resultFileStore,
                              @Value("${storage.upload-dir}") String uploadDir) {
        this.estimationService = estimationService;
        this.documentStore = documentStore;
        this.embeddingService = embeddingService;
        this.resultFileStore = resultFileStore;

        this.uploadDir = uploadDir;
    }

    @PostMapping(value = "/upload-estimate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EstimateResponse> uploadAndEstimate(@RequestPart("file") MultipartFile file,
                                                              @RequestParam(value = "projectName", required = false) String projectName,
                                                              @RequestParam(value = "deadline", required = false) String deadline) throws Exception {
        String docId = "doc-" + UUID.randomUUID().toString().substring(0,8);
        File saved = FileUtil.saveMultipartFile(file, uploadDir, docId);
        String text = FileUtil.extractText(saved);

        DocumentStore.DocInfo info = new DocumentStore.DocInfo();
        info.docId = docId;
        info.filename = saved.getName();
        info.status = "ready";
        info.extractedSummary = text.length() > 800 ? text.substring(0,800) + "..." : text;
        info.fullText = text;
        documentStore.put(info);

        EstimateRequest req = new EstimateRequest();
        req.setUploadDocId(docId);
        req.setProjectName(projectName);
        req.setDeadline(deadline);

        EstimateResponse resp = estimationService.estimate(req);

        try { resultFileStore.saveResult(resp, uploadDir); } catch (Exception ex) { System.err.println("Failed to save result: " + ex.getMessage()); }

        return ResponseEntity.ok(resp);
    }
}
