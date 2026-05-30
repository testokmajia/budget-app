package com.techmanage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmanage.common.BusinessException;
import com.techmanage.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class WechatApiService {

    private final String appId;
    private final String secret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SystemConfigRepository systemConfigRepository;

    public WechatApiService(
            @Value("${wechat.miniapp.app-id:}") String appId,
            @Value("${wechat.miniapp.secret:}") String secret,
            ObjectMapper objectMapper,
            SystemConfigRepository systemConfigRepository) {
        this.appId = appId;
        this.secret = secret;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.systemConfigRepository = systemConfigRepository;
    }

    private String resolveAppId() {
        var dbConfig = systemConfigRepository.findByConfigKey("wechat.miniapp.app-id");
        if (dbConfig.isPresent() && dbConfig.get().getConfigValue() != null
            && !dbConfig.get().getConfigValue().isBlank()) {
            return dbConfig.get().getConfigValue();
        }
        return appId;
    }

    private String resolveSecret() {
        var dbConfig = systemConfigRepository.findByConfigKey("wechat.miniapp.secret");
        if (dbConfig.isPresent() && dbConfig.get().getConfigValue() != null
            && !dbConfig.get().getConfigValue().isBlank()) {
            return dbConfig.get().getConfigValue();
        }
        return secret;
    }

    public record WechatSession(String openId, String sessionKey, String unionId) {}

    public WechatSession code2session(String code) {
        String effectiveAppId = resolveAppId();
        String effectiveSecret = resolveSecret();
        if (effectiveAppId == null || effectiveAppId.isBlank() || effectiveSecret == null || effectiveSecret.isBlank()) {
            throw new BusinessException("微信小程序未配置 (app-id/secret)，请在系统管理中设置");
        }

        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            effectiveAppId, effectiveSecret, URLEncoder.encode(code, StandardCharsets.UTF_8)
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                throw new BusinessException("微信API错误: " + json.path("errmsg").asText());
            }

            return new WechatSession(
                json.get("openid").asText(),
                json.path("session_key").asText(),
                json.path("unionid").asText()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("调用微信API失败: " + e.getMessage(), e);
        }
    }
}
