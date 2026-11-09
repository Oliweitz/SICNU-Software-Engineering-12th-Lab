package com.example.common.util;

/**
 * 敏感信息脱敏工具
 *
 * <p>手机号、邮箱不得以明文进入日志与接口响应（见 docs/init.sql 第 7.3 节）， 统一通过本类输出。
 */
public final class MaskUtil {

    private MaskUtil() {}

    /** 手机号脱敏：13800000000 → 138****0000 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 邮箱脱敏：student@example.com → s***@example.com */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** 按联系方式类型自动脱敏 */
    public static String maskContact(String contact) {
        if (contact == null) {
            return "***";
        }
        return contact.contains("@") ? maskEmail(contact) : maskPhone(contact);
    }
}
