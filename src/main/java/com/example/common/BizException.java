package com.example.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * <p>Service 层遇到可预期的业务错误（数据不存在、状态不允许等）时抛出，由 {@link GlobalExceptionHandler} 统一转换为 {@link Result}
 * 返回；禁止在 Controller 层到处 try-catch。
 */
@Getter
public class BizException extends RuntimeException {

    /** 错误码（默认业务处理失败） */
    private final ResultCode resultCode;

    public BizException(String message) {
        super(message);
        this.resultCode = ResultCode.BUSINESS_ERROR;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
