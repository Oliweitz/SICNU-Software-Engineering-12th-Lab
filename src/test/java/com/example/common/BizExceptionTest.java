package com.example.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link BizException} 单元测试 */
class BizExceptionTest {

    @Test
    @DisplayName("默认错误码为 BUSINESS_ERROR")
    void defaultResultCode() {
        BizException e =
                assertThrows(
                        BizException.class,
                        () -> {
                            throw new BizException("任务不存在");
                        });

        assertEquals(ResultCode.BUSINESS_ERROR, e.getResultCode());
        assertEquals("任务不存在", e.getMessage());
    }

    @Test
    @DisplayName("支持指定错误码")
    void customResultCode() {
        BizException e =
                assertThrows(
                        BizException.class,
                        () -> {
                            throw new BizException(ResultCode.NOT_FOUND, "班级不存在");
                        });

        assertEquals(ResultCode.NOT_FOUND, e.getResultCode());
        assertEquals("班级不存在", e.getMessage());
    }
}
