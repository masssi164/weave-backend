package com.massimotter.weave.backend.config;

import com.massimotter.weave.backend.boards.local.LocalPreviewBoardsRepository;
import com.massimotter.weave.backend.boards.port.BoardsPreviewGuard;
import com.massimotter.weave.backend.boards.port.BoardsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BoardsRuntimeConfiguration {

    @Bean
    BoardsPreviewGuard boardsPreviewGuard(
            @Value("${weave.boards.preview.runtime-enabled:false}") boolean enabled) {
        return new BoardsPreviewGuard(enabled);
    }

    @Bean
    BoardsRepository boardsRepository() {
        return new LocalPreviewBoardsRepository();
    }
}
