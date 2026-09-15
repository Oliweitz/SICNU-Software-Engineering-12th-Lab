package com.example.common;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    /**
     * 业务数据
     *
     * <p>{@code @JsonInclude(ALWAYS)} 显式覆盖 application.yml 中全局的 {@code default-property-inclusion:
     * non_null}： 全局配置会令 null 字段整个从 JSON 中消失，导致「无数据的成功响应」缺失 {@code data} 键， 与文档约定的 {@code {code,
     * message, data}} 三字段契约不符。此处保证 <b>data 键始终存在</b>（无数据时为 {@code null}）， 前端可安全地统一按 {@code
     * res.data} 取值。
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
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
