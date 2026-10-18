package com.group15.lab1.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一返回结构化错误响应。
 * <p>
 * 遵循"早抛出，晚捕获"原则：
 * - 业务异常在 Service 层发现条件时立即抛出（早抛出）
 * - 所有异常在此处统一捕获、记录日志、包装为规范响应（晚捕获）
 * - 服务器内部异常对外隐藏技术细节，防止信息泄露
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        log.warn("资源冲突: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), "请求的资源已存在");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), "认证失败");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("参数不合法: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), "参数不合法");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        String message = "请求参数校验失败";
        FieldError firstError = ex.getBindingResult().getFieldError();
        if (firstError != null && firstError.getDefaultMessage() != null) {
            message = firstError.getDefaultMessage();
        }
        log.warn("参数校验失败: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, message, "参数校验失败");
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException ex) {
        log.warn("任务不存在: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), "资源不存在");
    }

    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<ApiError> handleTeamNotFound(TeamNotFoundException ex) {
        log.warn("团队不存在: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), "团队不存在");
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiError> handleMemberNotFound(MemberNotFoundException ex) {
        log.warn("团队成员不存在: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), "团队成员不存在");
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiError> handleForbiddenOperation(ForbiddenOperationException ex) {
        log.warn("权限不足: {}", ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), "权限不足");
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        log.warn("操作冲突: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), "操作冲突");
    }

    /**
     * 兜底处理：捕获所有未预期的服务器内部异常。
     * - 使用 log.error 记录完整堆栈，方便排查问题
     * - 对外只返回通用提示，隐藏内部技术细节，防止信息泄露
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception ex) {
        log.error("服务器内部异常，类型: {}，原因: {}", ex.getClass().getName(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请联系管理员", "Internal Server Error");
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status, String message, String details) {
        ApiError apiError = new ApiError(status.value(), status.getReasonPhrase(), message, details);
        return ResponseEntity.status(status).body(apiError);
    }
}