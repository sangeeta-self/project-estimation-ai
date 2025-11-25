package com.org.estimator.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.estimator.ai.dto.response.EstimateResponse;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

@Component
public class ResultFileStore {
    private final ObjectMapper mapper = new ObjectMapper();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public File saveResult(EstimateResponse resp, String uploadDir) throws Exception {
        String ts = resp.getGeneratedAt() == null ?
                java.time.OffsetDateTime.now().format(fmt) :
                resp.getGeneratedAt().format(fmt);
        String filename = resp.getProjectId() + "_" + ts + ".json";
        File dir = Path.of(uploadDir, "estimates").toFile();
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, filename);
        mapper.writerWithDefaultPrettyPrinter().writeValue(out, resp);
        return out;
    }
}
