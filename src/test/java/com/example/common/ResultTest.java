package com.example.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    @DisplayName("序列化：全局 non_null 配置下 data 键仍必须存在（契约 {code, message, data} 三字段恒定）")
    void dataKeyAlwaysPresentInJson() throws Exception {
        // 复现 application.yml 的全局配置 default-property-inclusion: non_null
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        String okJson = mapper.writeValueAsString(Result.ok());
        String failJson = mapper.writeValueAsString(Result.fail(ResultCode.NOT_FOUND));

        assertTrue(okJson.contains("\"data\""), "成功响应缺少 data 键：" + okJson);
        assertTrue(failJson.contains("\"data\""), "失败响应缺少 data 键：" + failJson);
        assertEquals("{\"code\":0,\"message\":\"成功\",\"data\":null}", okJson);
    }
}
