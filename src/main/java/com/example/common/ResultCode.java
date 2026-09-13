package com.example.common;

/**
 * 统一响应码
 *
 * <p>0 表示成功；业务错误码分段：
 *
 * <ul>
 *   <li>1xxx 通用错误（参数、未登录、无权限等）
 *   <li>2xxx 业务错误（数据不存在、状态不允许等，各模块按需扩展）
 *   <li>5xxx 系统错误（外部依赖、未知异常）
 * </ul>
 */
public enum ResultCode {
    SUCCESS(0, "成功"),
    BAD_REQUEST(1001, "参数错误"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无权限访问"),
    NOT_FOUND(1004, "资源不存在"),
    METHOD_NOT_ALLOWED(1005, "请求方式不支持"),
    CONFLICT(1006, "资源冲突"),

    BUSINESS_ERROR(2000, "业务处理失败"),

    INTERNAL_ERROR(5000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
