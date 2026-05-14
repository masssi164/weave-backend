package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.boards.local.LocalPreviewBoardsRepository;
import com.massimotter.weave.backend.boards.port.BoardsPreviewGuard;
import com.massimotter.weave.backend.exception.ApiErrorException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardsFacadeServiceTest {

    @Test
    void previewRuntimeFailsClosedWhenFeatureFlagIsDisabled() {
        BoardsFacadeService service = new BoardsFacadeService(
                new BoardsPreviewGuard(false),
                new LocalPreviewBoardsRepository());

        assertThatThrownBy(service::preview)
                .isInstanceOfSatisfying(ApiErrorException.class, error -> {
                    assertThat(error.status().value()).isEqualTo(503);
                    assertThat(error.code()).isEqualTo("boards-provider_unavailable");
                    assertThat(error.details()).containsEntry("module", "boards");
                    assertThat(error.details()).containsEntry("preview", true);
                    assertThat(error.details()).containsEntry("releaseStatus", "post-release-hidden-preview");
                });
    }
}
