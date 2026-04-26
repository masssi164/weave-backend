package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.files.CreateFolderRequest;
import com.massimotter.weave.backend.model.files.FileItemResponse;
import com.massimotter.weave.backend.model.files.FileListResponse;
import com.massimotter.weave.backend.model.files.FileUploadResponse;
import com.massimotter.weave.backend.service.files.DownloadedFile;
import com.massimotter.weave.backend.service.files.FilesStorageAdapter;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FilesFacadeService {

    private final FilesStorageAdapter filesStorageAdapter;

    public FilesFacadeService(ObjectProvider<FilesStorageAdapter> filesStorageAdapterProvider) {
        this.filesStorageAdapter = filesStorageAdapterProvider.getIfAvailable();
    }

    public FileListResponse list(String path) {
        return configuredAdapter("list-files").list(path);
    }

    public FileItemResponse createFolder(CreateFolderRequest request) {
        return configuredAdapter("create-folder").createFolder(request);
    }

    public FileUploadResponse upload(String parentPath, MultipartFile file) {
        return configuredAdapter("upload-file").upload(parentPath, file);
    }

    public DownloadedFile download(String id) {
        return configuredAdapter("download-file").download(id);
    }

    public void delete(String id) {
        configuredAdapter("delete-file").delete(id);
    }

    private FilesStorageAdapter configuredAdapter(String operation) {
        if (filesStorageAdapter == null || !filesStorageAdapter.isConfigured()) {
            throw adapterNotConfigured(operation);
        }
        return filesStorageAdapter;
    }

    private ApiErrorException adapterNotConfigured(String operation) {
        return new ApiErrorException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "nextcloud-adapter-not-configured",
                "Files facade is available, but the downstream Nextcloud adapter is not configured yet.",
                Map.of("module", "files", "operation", operation));
    }
}
