package com.example.module.user.enums;

/**
 * 验证码使用场景
 *
 * <p>同一联系方式在不同场景下的验证码互相隔离，登录验证码不能用于重置密码。
 */
public enum CodeScene {
    /** 验证码登录 */
    LOGIN("登录"),
    /** 注册时验证手机号/邮箱归属 */
    REGISTER("注册"),
    /** 忘记密码 */
    RESET_PASSWORD("找回密码");

    private final String label;

    CodeScene(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
