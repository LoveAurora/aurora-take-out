package org.aurora.service;

import org.aurora.result.Result;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    Result<String> uploadImg(MultipartFile img);
}
