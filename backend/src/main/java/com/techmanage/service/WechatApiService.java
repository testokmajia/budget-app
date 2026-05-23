package com.techmanage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class WechatApiService {

    private final String appId;
    private final String secret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WechatApiService(
            @Value("${wechat.miniapp.app-id:}") String appId,
            @Value("${wechat.miniapp.secret:}") String secret,
            ObjectMapper objectMapper) {
        this.appId = appId;
        this.secret = secret;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public record WechatSession(String openId, String sessionKey, String unionId) {}

    public WechatSession code2session(String code) {
        if (appId == null || appId.isBlank() || secret == null || secret.isBlank()) {
            throw new RuntimeException("微信小程序未配置 (app-id/secret)");
        }

        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appId, secret, code
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                throw new RuntimeException("微信API错误: " + json.path("errmsg").asText());
            }

            return new WechatSession(
                json.get("openid").asText(),
                json.path("session_key").asText(),
                json.path("unionid").asText()
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("调用微信API失败: " + e.getMessage(), e);
        }
    }
}
