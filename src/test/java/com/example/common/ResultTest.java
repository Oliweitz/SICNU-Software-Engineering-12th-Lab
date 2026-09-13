package com.example.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link Result} 统一响应体单元测试 */
class ResultTest {

    @Test
    @DisplayName("成功响应：无数据时 code=0 且 data 为 null")
    void okWithoutData() {
        Result<Void> result = Result.ok();

        assertEquals(0, result.getCode());
        assertEquals("成功", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("成功响应：携带业务数据")
    void okWithData() {
        Result<String> result = Result.ok("hello");

        assertEquals(0, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    @DisplayName("失败响应：使用预设错误码与提示")
    void failWithResultCode() {
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);

        assertEquals(ResultCode.UNAUTHORIZED.getCode(), result.getCode());
        assertEquals("未登录或登录已过期", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("失败响应：可覆盖提示信息")
    void failWithCustomMessage() {
        Result<Void> result = Result.fail(ResultCode.BAD_REQUEST, "用户名不能为空");

        assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
        assertEquals("用户名不能为空", result.getMessage());
    }
}
