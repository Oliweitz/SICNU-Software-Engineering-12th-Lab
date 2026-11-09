package com.example.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.common.util.MaskUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 敏感信息脱敏单元测试 */
class MaskUtilTest {

    @Test
    @DisplayName("手机号保留前三位与后四位")
    void maskPhone() {
        assertEquals("138****0000", MaskUtil.maskPhone("13800000000"));
        assertEquals("***", MaskUtil.maskPhone(null));
        assertEquals("***", MaskUtil.maskPhone("123"));
    }

    @Test
    @DisplayName("邮箱保留首字符与域名")
    void maskEmail() {
        assertEquals("s***@example.com", MaskUtil.maskEmail("student@example.com"));
        assertEquals("***", MaskUtil.maskEmail(null));
        assertEquals("***", MaskUtil.maskEmail(""));
    }

    @Test
    @DisplayName("按联系方式类型自动选择脱敏规则")
    void maskContact() {
        assertEquals("139****0002", MaskUtil.maskContact("13900000002"));
        assertEquals("a***@example.com", MaskUtil.maskContact("admin@example.com"));
    }
}
