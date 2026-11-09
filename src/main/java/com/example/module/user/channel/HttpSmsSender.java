package com.example.module.user.channel;

import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.common.util.MaskUtil;
import com.example.config.AuthProperties;
import com.example.module.user.enums.CodeScene;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP 短信网关通道
 *
 * <p>请求约定：POST {@code auth.code.sms.http-url}，JSON 体 {@code
 * {"phone","code","scene","expireMinutes"}}；配置了 http-token 时放在 {@code Authorization: Bearer
 * <token>}。可对接自建网关或云厂商的 HTTP 接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.code.sms.provider", havingValue = "http")
public class HttpSmsSender implements SmsSender {

    private final AuthProperties authProperties;

    @Override
    public void send(String phone, String code, CodeScene scene, int expireMinutes) {
        AuthProperties.Sms config = authProperties.getCode().getSms();
        if (!StringUtils.hasText(config.getHttpUrl())) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "短信通道未配置，请联系管理员");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", phone);
        body.put("code", code);
        body.put("scene", scene.name());
        body.put("expireMinutes", expireMinutes);

        try {
            RestClient.RequestBodySpec spec =
                    RestClient.create()
                            .post()
                            .uri(config.getHttpUrl())
                            .contentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(config.getHttpToken())) {
                spec = spec.header("Authorization", "Bearer " + config.getHttpToken());
            }
            spec.body(body).retrieve().toBodilessEntity();
            log.info(
                    "短信已发送: phone={}, scene={}, provider=http",
                    MaskUtil.maskPhone(phone),
                    scene.getLabel());
        } catch (RestClientException e) {
            log.error("短信发送失败: phone={}, scene={}", MaskUtil.maskPhone(phone), scene.name(), e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    @Override
    public String providerName() {
        return "http";
    }
}
