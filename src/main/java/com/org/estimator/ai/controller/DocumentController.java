package com.org.estimator.ai.controller;

import com.org.estimator.ai.dto.request.EstimateRequest;
import com.org.estimator.ai.dto.response.EstimateResponse;
import com.org.estimator.ai.service.EstimationService;
import com.org.estimator.ai.service.impl.DocumentStore;
import com.org.estimator.ai.util.FileUtil;
import com.org.estimator.ai.util.ResultFileStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Estimation", description = "Generate AI-driven project estimates")
public class DocumentController {

    private final EstimationService estimationService;
    private final DocumentStore documentStore;
    private final ResultFileStore resultFileStore;
    private final String uploadDir;


    public DocumentController(EstimationService estimationService, DocumentStore documentStore,
                              ResultFileStore resultFileStore,
                              @Value("${storage.upload-dir}") String uploadDir) {
        this.estimationService = estimationService;
        this.documentStore = documentStore;
        this.resultFileStore = resultFileStore;
        this.uploadDir = uploadDir;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>>  upload(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "filename", required = false) String filename) throws Exception {
        String docId = "doc-" + java.time.LocalDate.now().toString().replaceAll("-", "") + "-" + UUID.randomUUID().toString().substring(0,4);
        String finalName = (filename == null || filename.isBlank()) ? file.getOriginalFilename() : filename;
        File saved = FileUtil.saveMultipartFile(file, uploadDir, docId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("docId", docId);
        resp.put("fileName", saved.getName());
        resp.put("filePath", saved.getAbsolutePath());
        resp.put("message", "File uploaded successfully");
        return ResponseEntity.ok(resp);
    }
}
