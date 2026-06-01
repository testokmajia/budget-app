package com.techmanage.controller;

import com.techmanage.common.ApiResponse;
import com.techmanage.dto.ForgotPasswordResetRequest;
import com.techmanage.dto.SendResetCodeRequest;
import com.techmanage.dto.VerifyResetCodeRequest;
import com.techmanage.service.VerificationCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 密码重置控制器 — 忘记密码流程
 */
@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final VerificationCodeService verificationCodeService;

    public PasswordResetController(VerificationCodeService verificationCodeService) {
        this.verificationCodeService = verificationCodeService;
    }

    /** 步骤1：发送验证码到邮箱 */
    @PostMapping("/send-reset-code")
    public ApiResponse<Void> sendResetCode(
            @Valid @RequestBody SendResetCodeRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        verificationCodeService.sendCode(request.email(), clientIp);
        // 无论邮箱是否存在，统一返回成功（防邮箱枚举攻击）
        return ApiResponse.ok();
    }

    /** 步骤2：校验验证码，返回重置令牌 */
    @PostMapping("/verify-reset-code")
    public ApiResponse<String> verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request) {
        String resetToken = verificationCodeService.verifyCode(request.email(), request.code());
        return ApiResponse.ok(resetToken);
    }

    /** 步骤3：使用重置令牌完成密码重置 */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ForgotPasswordResetRequest request) {
        verificationCodeService.resetPassword(
            request.email(), request.resetToken(), request.newPassword());
        return ApiResponse.ok();
    }
}
