package com.example.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 *
 * <p>所有异常在此统一转换为 {@link Result}，保证前端拿到的响应格式始终一致。 未知异常记录 error 日志（含堆栈），便于排查；业务异常不记堆栈。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：使用异常自带的错误码与提示
     *
     * <p>认证/授权类业务异常同时映射为 HTTP 401/403（与 Security 过滤器链的返回口径一致， 见 docs/技术方案.md §7）；其余业务错误保持 HTTP
     * 200，由前端按 body.code 提示。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getResultCode().getCode(), e.getMessage());
        HttpStatus status =
                switch (e.getResultCode()) {
                    case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
                    case FORBIDDEN -> HttpStatus.FORBIDDEN;
                    default -> HttpStatus.OK;
                };
        return ResponseEntity.status(status).body(Result.fail(e.getResultCode(), e.getMessage()));
    }

    /**
     * @RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message =
                e.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(f -> f.getField() + " " + f.getDefaultMessage())
                        .orElse(ResultCode.BAD_REQUEST.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message =
                e.getFieldErrors().stream()
                        .findFirst()
                        .map(f -> f.getField() + " " + f.getDefaultMessage())
                        .orElse(ResultCode.BAD_REQUEST.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    /** 请求体缺失或格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "请求体缺失或格式错误");
    }

    /** 请求方式不支持（GET 调了 POST 等） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    /** 无权限访问（方法级 @PreAuthorize 拒绝）；HTTP 403 与 docs/技术方案.md §7 一致 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.fail(ResultCode.FORBIDDEN));
    }

    /** 静态资源/接口路径不存在（区分 404 与 500） */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
