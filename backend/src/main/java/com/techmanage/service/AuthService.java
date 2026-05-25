package com.techmanage.service;

import com.techmanage.dto.ChangePasswordRequest;
import com.techmanage.dto.LoginRequest;
import com.techmanage.dto.LoginResponse;
import com.techmanage.dto.RegisterRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
