package com.vibechat.controller;

import com.vibechat.dto.UploadResponse;
import com.vibechat.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/media")
    public ResponseEntity<UploadResponse> uploadMedia(@RequestParam("file") MultipartFile file) {
        UploadResponse response = uploadService.storeFile(file);
        return ResponseEntity.ok(response);
    }
}
