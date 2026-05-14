package com.massimotter.weave.backend.service;

import com.massimotter.weave.backend.boards.domain.TaskItem;
import com.massimotter.weave.backend.boards.port.BoardQuery;
import com.massimotter.weave.backend.boards.port.BoardsPreviewGuard;
import com.massimotter.weave.backend.boards.port.BoardsRepository;
import com.massimotter.weave.backend.boards.port.CreateTaskCommand;
import com.massimotter.weave.backend.boards.port.MoveTaskCommand;
import com.massimotter.weave.backend.boards.port.TaskQuery;
import com.massimotter.weave.backend.boards.support.BoardsErrorCode;
import com.massimotter.weave.backend.boards.support.BoardsException;
import com.massimotter.weave.backend.exception.ApiErrorException;
import com.massimotter.weave.backend.model.boards.BoardsCreateTaskRequest;
import com.massimotter.weave.backend.model.boards.BoardsMoveTaskRequest;
import com.massimotter.weave.backend.model.boards.BoardsPreviewResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BoardsFacadeService {

    private final BoardsPreviewGuard previewGuard;
    private final BoardsRepository boardsRepository;

    public BoardsFacadeService(BoardsPreviewGuard previewGuard, BoardsRepository boardsRepository) {
        this.previewGuard = previewGuard;
        this.boardsRepository = boardsRepository;
    }

    public BoardsPreviewResponse preview() {
        requireEnabled();
        try {
            var projects = boardsRepository.listProjects(BoardQuery.firstPage()).items();
            var boards = projects.stream()
                    .flatMap(project -> boardsRepository.listBoards(project.id(), BoardQuery.firstPage()).items().stream())
                    .toList();
            var tasks = boards.stream()
                    .flatMap(board -> boardsRepository.listTasks(board.id(), TaskQuery.all()).items().stream())
                    .toList();
            return new BoardsPreviewResponse(
                    true,
                    "post-release-hidden-preview",
                    "local-preview-backend-facade",
                    boardsRepository.capabilities(),
                    projects,
                    boards,
                    tasks);
        } catch (BoardsException exception) {
            throw apiError(exception);
        }
    }

    public TaskItem createTask(String boardId, BoardsCreateTaskRequest request) {
        requireEnabled();
        try {
            return boardsRepository.createTask(new CreateTaskCommand(
                    boardId,
                    request.columnId(),
                    request.title(),
                    request.description(),
                    request.assigneeRefs(),
                    request.labelRefs(),
                    request.dueAt()));
        } catch (BoardsException exception) {
            throw apiError(exception);
        }
    }

    public TaskItem moveTask(String taskId, BoardsMoveTaskRequest request) {
        requireEnabled();
        try {
            return boardsRepository.moveTask(new MoveTaskCommand(
                    taskId,
                    request.targetColumnId(),
                    request.targetPosition()));
        } catch (BoardsException exception) {
            throw apiError(exception);
        }
    }

    public TaskItem completeTask(String taskId) {
        requireEnabled();
        try {
            return boardsRepository.completeTask(taskId);
        } catch (BoardsException exception) {
            throw apiError(exception);
        }
    }

    private void requireEnabled() {
        try {
            previewGuard.requireEnabled();
        } catch (BoardsException exception) {
            throw apiError(exception);
        }
    }

    private ApiErrorException apiError(BoardsException exception) {
        HttpStatus status = switch (exception.code()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case PROVIDER_UNAVAILABLE, OFFLINE, UNSUPPORTED_CAPABILITY -> HttpStatus.SERVICE_UNAVAILABLE;
            case UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("module", "boards");
        details.put("preview", true);
        details.put("releaseStatus", "post-release-hidden-preview");
        details.putAll(exception.details());
        return new ApiErrorException(status, "boards-" + exception.code().contractName(), exception.getMessage(), details);
    }
}
