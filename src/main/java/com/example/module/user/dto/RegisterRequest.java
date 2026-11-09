package com.example.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求（POST /api/v1/auth/register） */
@Data
public class RegisterRequest {

    /** 登录名（学号/工号） */
    @NotBlank(message = "登录名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,50}$", message = "登录名只能包含字母、数字、下划线，长度 4-50")
    private String username;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名不能超过 50 个字符")
    private String realName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 手机号（必填，注册时通过短信验证码验证归属） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    /** 短信验证码 */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;

    /** 邮箱（选填；填写时必须通过邮箱验证码验证归属） */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

    /** 邮箱验证码（填写邮箱时必填） */
    private String emailCode;

    /** 专业（字典 major 的字典码，选填） */
    @Size(max = 20, message = "专业编码不能超过 20 个字符")
    private String major;

    /** 年级（字典 grade 的字典码，选填） */
    @Size(max = 20, message = "年级编码不能超过 20 个字符")
    private String grade;

    /** 图形验证码标识 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaId;

    /** 图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
