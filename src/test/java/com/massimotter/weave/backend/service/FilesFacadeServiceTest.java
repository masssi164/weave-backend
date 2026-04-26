package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.files.CreateFolderRequest;
import com.massimotter.weave.backend.model.files.FileItemResponse;
import com.massimotter.weave.backend.model.files.FileListResponse;
import com.massimotter.weave.backend.model.files.FileUploadResponse;
import com.massimotter.weave.backend.service.files.DownloadedFile;
import com.massimotter.weave.backend.service.files.FilesStorageAdapter;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesFacadeServiceTest {

    @Test
    void failsClosedWhenAdapterIsMissingOrUnconfigured() {
        FilesFacadeService missing = new FilesFacadeService(provider(null));
        FilesFacadeService unconfigured = new FilesFacadeService(provider(new StubAdapter(false)));

        assertThatThrownBy(() -> missing.list("/"))
                .isInstanceOfSatisfying(ApiErrorException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(exception.code()).isEqualTo("nextcloud-adapter-not-configured");
                    assertThat(exception.details()).containsEntry("operation", "list-files");
                });
        assertThatThrownBy(() -> unconfigured.upload("/", null))
                .isInstanceOfSatisfying(ApiErrorException.class, exception ->
                        assertThat(exception.details()).containsEntry("operation", "upload-file"));
    }

    @Test
    void delegatesToConfiguredStorageAdapter() {
        FilesFacadeService service = new FilesFacadeService(provider(new StubAdapter(true)));

        FileListResponse response = service.list("/Team");

        assertThat(response.path()).isEqualTo("/Team");
        assertThat(response.items()).extracting(FileItemResponse::name).containsExactly("readme.md");
    }

    private ObjectProvider<FilesStorageAdapter> provider(FilesStorageAdapter adapter) {
        return new ObjectProvider<>() {
            @Override
            public FilesStorageAdapter getObject(Object... args) {
                return adapter;
            }

            @Override
            public FilesStorageAdapter getIfAvailable() {
                return adapter;
            }

            @Override
            public FilesStorageAdapter getIfUnique() {
                return adapter;
            }

            @Override
            public FilesStorageAdapter getObject() {
                return adapter;
            }

            @Override
            public Iterator<FilesStorageAdapter> iterator() {
                return adapter == null ? List.<FilesStorageAdapter>of().iterator() : List.of(adapter).iterator();
            }
        };
    }

    private static final class StubAdapter implements FilesStorageAdapter {

        private final boolean configured;

        private StubAdapter(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public FileListResponse list(String path) {
            return new FileListResponse(path, List.of(new FileItemResponse(
                    "files:test",
                    "readme.md",
                    path + "/readme.md",
                    "file",
                    "text/markdown",
                    12L,
                    OffsetDateTime.parse("2026-04-26T08:00:00Z"),
                    true)), null);
        }

        @Override
        public FileItemResponse createFolder(CreateFolderRequest request) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public FileUploadResponse upload(String parentPath, MultipartFile file) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public DownloadedFile download(String id) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public void delete(String id) {
            throw new UnsupportedOperationException("not needed");
        }
    }
}
