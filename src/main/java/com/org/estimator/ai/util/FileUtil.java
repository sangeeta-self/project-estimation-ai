package com.org.estimator.ai.util;

import org.apache.commons.io.FileUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;


public class FileUtil {

    public static File saveMultipartFile(MultipartFile file, String uploadDir, String docId) throws Exception {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        String filename = docId + "_" + file.getOriginalFilename();
        File saved = Paths.get(uploadDir, filename).toFile();
        FileUtils.copyFile(dir, saved);
        return saved;
    }
}
