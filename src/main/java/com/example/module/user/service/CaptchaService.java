package com.example.module.user.service;

import com.example.module.user.dto.CaptchaVO;

/** 图形验证码服务（登录/发送短信验证码前的人机校验） */
public interface CaptchaService {

    /**
     * 生成图形验证码
     *
     * @return 验证码标识与 Base64 图片
     */
    CaptchaVO generate();

    /**
     * 校验图形验证码（一次性，校验后立即作废，不区分成功失败）
     *
     * @param captchaId 标识
     * @param captchaCode 用户输入
     */
    void verify(String captchaId, String captchaCode);
}
