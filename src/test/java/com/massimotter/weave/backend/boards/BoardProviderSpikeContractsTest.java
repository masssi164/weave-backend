package com.massimotter.weave.backend.boards;

import com.massimotter.weave.backend.boards.deck.NextcloudDeckBoardsRepository;
import com.massimotter.weave.backend.boards.domain.BoardCapability;
import com.massimotter.weave.backend.boards.domain.ProviderKind;
import com.massimotter.weave.backend.boards.openproject.OpenProjectBoardsRepository;
import com.massimotter.weave.backend.boards.support.BoardsErrorCode;
import com.massimotter.weave.backend.boards.support.BoardsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardProviderSpikeContractsTest {

    @Test
    void openProjectBenchmarkAdapterDeclaresCapabilitiesButStaysDisabled() {
        var repository = new OpenProjectBoardsRepository();

        var capabilities = repository.capabilities();

        assertThat(capabilities.provider()).isEqualTo(ProviderKind.OPEN_PROJECT);
        assertThat(capabilities.enabled()).isFalse();
        assertThat(capabilities.supported()).contains(
                BoardCapability.COMMENTS,
                BoardCapability.ATTACHMENTS,
                BoardCapability.NON_DESTRUCTIVE_ARCHIVE,
                BoardCapability.CUSTOM_FIELDS);
        assertThat(capabilities.unsupported()).contains(
                BoardCapability.WEBHOOK_EVENTS,
                BoardCapability.INCREMENTAL_SYNC,
                BoardCapability.ACCESSIBLE_NON_DRAG_MOVES);
        assertThat(capabilities.supportSafeSummary()).contains("benchmark").contains("Release 1");
        assertThatThrownBy(() -> repository.listProjects(null))
                .isInstanceOf(BoardsException.class)
                .satisfies(error -> assertThat(((BoardsException) error).code())
                        .isEqualTo(BoardsErrorCode.PROVIDER_UNAVAILABLE))
                .hasMessageContaining("benchmark-only")
                .hasMessageContaining("disabled");
    }

    @Test
    void deckBridgeAdapterDeclaresCapabilitiesButStaysDisabled() {
        var repository = new NextcloudDeckBoardsRepository();

        var capabilities = repository.capabilities();

        assertThat(capabilities.provider()).isEqualTo(ProviderKind.NEXTCLOUD_DECK);
        assertThat(capabilities.enabled()).isFalse();
        assertThat(capabilities.supported()).contains(
                BoardCapability.COMMENTS,
                BoardCapability.ATTACHMENTS,
                BoardCapability.NON_DESTRUCTIVE_ARCHIVE);
        assertThat(capabilities.unsupported()).contains(
                BoardCapability.WEBHOOK_EVENTS,
                BoardCapability.INCREMENTAL_SYNC,
                BoardCapability.CUSTOM_FIELDS,
                BoardCapability.ACCESSIBLE_NON_DRAG_MOVES);
        assertThat(capabilities.supportSafeSummary()).contains("bridge").contains("Release 1");
        assertThatThrownBy(() -> repository.listProjects(null))
                .isInstanceOf(BoardsException.class)
                .satisfies(error -> assertThat(((BoardsException) error).code())
                        .isEqualTo(BoardsErrorCode.PROVIDER_UNAVAILABLE))
                .hasMessageContaining("bridge-only")
                .hasMessageContaining("disabled");
    }
}
