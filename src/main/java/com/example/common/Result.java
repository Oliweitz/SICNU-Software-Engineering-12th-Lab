package com.example.common;

import lombok.Data;

/**
 * 统一响应体：{@code {code, message, data}}（见 docs/技术方案.md 第 7 节）
 *
 * <p>Controller 层一律返回 {@code Result<T>}，禁止直接返回裸数据；分页查询请使用 {@link PageResult} 作为 data。
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> {

    /** 响应码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }
}
