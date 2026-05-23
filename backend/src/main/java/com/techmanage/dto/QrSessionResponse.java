package com.techmanage.dto;

public record QrSessionResponse(
    String sessionId,
    String shortCode,
    String status,
    String token,
    LoginResponse.UserInfo user,
    String message,
    boolean bound
) {
    public static QrSessionResponse waiting(String sessionId, String shortCode) {
        return new QrSessionResponse(sessionId, shortCode, "WAITING", null, null, null, false);
    }

    public static QrSessionResponse scanned(String sessionId, String shortCode) {
        return new QrSessionResponse(sessionId, shortCode, "SCANNED", null, null, "已扫描，请在手机上确认登录", false);
    }

    public static QrSessionResponse confirmed(String sessionId, String shortCode, String token, LoginResponse.UserInfo user) {
        return new QrSessionResponse(sessionId, shortCode, "CONFIRMED", token, user, "登录成功", true);
    }

    public static QrSessionResponse expired(String sessionId, String shortCode) {
        return new QrSessionResponse(sessionId, shortCode, "EXPIRED", null, null, "二维码已过期，请刷新", false);
    }
}
