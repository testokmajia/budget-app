package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.*;
import com.techmanage.service.AuthService;
import com.techmanage.service.QrLoginService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final QrLoginService qrLoginService;

    public AuthController(AuthService authService, QrLoginService qrLoginService) {
        this.authService = authService;
        this.qrLoginService = qrLoginService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @GetMapping("/qr/init")
    public ApiResponse<QrSessionResponse> initQrLogin() {
        return ApiResponse.ok(qrLoginService.createSession());
    }

    @GetMapping("/qr/status")
    public ApiResponse<QrSessionResponse> pollQrStatus(@RequestParam String sid) {
        return ApiResponse.ok(qrLoginService.pollStatus(sid));
    }

    @PostMapping("/qr/wechat-login")
    public ApiResponse<QrSessionResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(qrLoginService.wechatLogin(request.sessionId(), request.code()));
    }

    @PostMapping("/qr/bind")
    public ApiResponse<QrSessionResponse> bindWechat(@Valid @RequestBody WechatBindRequest request) {
        return ApiResponse.ok(qrLoginService.bindAndLogin(request.sessionId(), request.username(), request.password()));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<LoginResponse> miniappLogin(@RequestParam String code) {
        return ApiResponse.ok(qrLoginService.miniappLogin(code));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                             Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        authService.changePassword(userId, request);
        return ApiResponse.ok(null);
    }
}
