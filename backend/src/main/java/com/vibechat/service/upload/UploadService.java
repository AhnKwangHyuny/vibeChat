package com.vibechat.service.upload;

import com.vibechat.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    UploadResponse storeFile(MultipartFile file);
}


