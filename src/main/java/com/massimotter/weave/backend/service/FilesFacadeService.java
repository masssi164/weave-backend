package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.files.CreateFolderRequest;
import com.massimotter.weave.backend.model.files.FileItemResponse;
import com.massimotter.weave.backend.model.files.FileListResponse;
import com.massimotter.weave.backend.model.files.FileUploadResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FilesFacadeService {

    public FileListResponse list(String path) {
        throw adapterNotConfigured("list-files");
    }

    public FileItemResponse createFolder(CreateFolderRequest request) {
        throw adapterNotConfigured("create-folder");
    }

    public FileUploadResponse upload(String parentPath, MultipartFile file) {
        throw adapterNotConfigured("upload-file");
    }

    public void prepareDownload(String id) {
        throw adapterNotConfigured("download-file");
    }

    public void delete(String id) {
        throw adapterNotConfigured("delete-file");
    }

    private ApiErrorException adapterNotConfigured(String operation) {
        return new ApiErrorException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "nextcloud-adapter-not-configured",
                "Files facade is available, but the downstream Nextcloud adapter is not configured yet.",
                Map.of("module", "files", "operation", operation));
    }
}
