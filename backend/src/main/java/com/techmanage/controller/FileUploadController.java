package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.entity.Attachment;
import com.techmanage.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final Path uploadDir;
    private final AttachmentRepository attachmentRepository;

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    public FileUploadController(@Value("${app.upload-dir:uploads}") String uploadDir,
                                 AttachmentRepository attachmentRepository) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.attachmentRepository = attachmentRepository;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory", e);
        }
    }

    @PostMapping("/files/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小不能超过20MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + ext;
        Path target = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return ApiResponse.ok(Map.of(
            "fileName", originalName != null ? originalName : storedName,
            "filePath", "/uploads/" + storedName
        ));
    }

    @PostMapping("/issues/{id}/attachments")
    public ApiResponse<List<Map<String, Object>>> uploadAttachments(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("文件 " + file.getOriginalFilename() + " 大小超过20MB限制");
            }
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            var attachment = new Attachment(id, originalName != null ? originalName : storedName,
                    "/uploads/" + storedName, file.getSize(), file.getContentType());
            attachment = attachmentRepository.save(attachment);
            results.add(Map.of(
                "id", attachment.getId(),
                "fileName", attachment.getFileName(),
                "filePath", attachment.getFilePath(),
                "fileSize", attachment.getFileSize(),
                "contentType", attachment.getContentType()
            ));
        }
        return ApiResponse.ok(results);
    }

    @GetMapping("/issues/{id}/attachments")
    public ApiResponse<List<Map<String, Object>>> getAttachments(@PathVariable Long id) {
        List<Attachment> attachments = attachmentRepository.findByIssueIdOrderByCreatedAtAsc(id);
        var result = attachments.stream().map(a -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", a.getId());
            m.put("fileName", a.getFileName());
            m.put("filePath", a.getFilePath());
            m.put("fileSize", a.getFileSize());
            m.put("contentType", a.getContentType());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).toList();
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/issues/{issueId}/attachments/{attId}")
    public ApiResponse<Void> deleteAttachment(@PathVariable Long issueId, @PathVariable Long attId) {
        attachmentRepository.deleteByIssueIdAndId(issueId, attId);
        return ApiResponse.ok(null);
    }
}
