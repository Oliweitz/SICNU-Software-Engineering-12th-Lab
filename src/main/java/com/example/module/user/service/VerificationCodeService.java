package com.example.module.user.service;

import com.example.module.user.enums.CodeChannel;
import com.example.module.user.enums.CodeScene;

/** 短信/邮箱验证码服务（生成、限频、校验） */
public interface VerificationCodeService {

    /**
     * 生成并发送验证码
     *
     * @return 生成的验证码（仅供开发环境回显，生产不得外泄）
     */
    String send(CodeScene scene, CodeChannel channel, String target);

    /**
     * 校验验证码（一次性；超过最大尝试次数即作废）
     *
     * @return 是否通过
     */
    boolean verify(CodeScene scene, String target, String code);

    /** 根据输入识别通道：含 @ 视为邮箱，否则按手机号校验 */
    CodeChannel detectChannel(String target);

    /** 归一化联系方式：去空格、邮箱转小写 */
    String normalizeTarget(String target);

    /** 是否为合法的手机号或邮箱 */
    boolean isContact(String target);
}
