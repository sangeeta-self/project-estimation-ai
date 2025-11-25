package com.org.estimator.ai.util;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.*;
import java.nio.file.Path;
import java.util.Locale;


public class FileUtil {

    public static File saveMultipartFile(MultipartFile file, String uploadDir, String docId) throws Exception {
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create upload directory: " + uploadDir);
        }

        String original = file.getOriginalFilename();
        String fileNme = getFileName(original == null ? "file" : original);
        File saved = Path.of(uploadDir, docId + "_" + fileNme).toFile();

        FileUtils.copyInputStreamToFile(file.getInputStream(), saved);
        return saved;
    }

    private static String getFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }

    public static String extractText(File file) throws Exception {
        String name = file.getName().toLowerCase(Locale.ROOT);

        System.out.println("[FileUtil] Extracting text from: " + file.getAbsolutePath());

        if (name.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (name.endsWith(".docx") || name.endsWith(".doc")) {
            return extractFromWord(file);
        }  else {
            return "";
        }
    }

    private static String extractFromPdf(File file) throws Exception {
        try (PDDocument doc = PDDocument.load(file)) {
            if (doc.isEncrypted()) {
                throw new RuntimeException("Encrypted PDF not supported.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            if (text == null || text.trim().isEmpty()) {
                System.out.println("[FileUtil] PDF has no extractable text (maybe scanned).");
                return ""; // NO OCR fallback
            }

            return text.trim();
        }
    }

    private static String extractFromWord(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument docx = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {

            String text = extractor.getText();
            return text == null ? "" : text.trim();
        } catch (Exception exDocx) {
            System.out.println("[FileUtil] DOCX parse failed: " + exDocx.getMessage() + " — attempting .doc parser.");
            try (FileInputStream fis2 = new FileInputStream(file);
                 HWPFDocument doc = new HWPFDocument(fis2);
                 WordExtractor extractor = new WordExtractor(doc)) {

                String text = extractor.getText();
                return text == null ? "" : text.trim();
            } catch (Exception exDoc) {
                System.err.println("[FileUtil] Both DOCX and DOC parsing failed: " + exDoc.getMessage());
                throw new RuntimeException("Unsupported or corrupt Word file: " + file.getName());
            }
        }
    }

    private static String extractFromRtf(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            RTFEditorKit rtfEditorKit = new RTFEditorKit();
            Document doc = rtfEditorKit.createDefaultDocument();
            rtfEditorKit.read(fis, doc, 0);
            return safeString(doc.getText(0, doc.getLength()));
        }
    }

    private static String extractFromText(File file) throws Exception {
        return safeString(FileUtils.readFileToString(file, "UTF-8"));
    }

    private static String safeString(String s) {
        return s == null ? "" : s.trim();
    }

}
