package com.techmanage.service;

import com.techmanage.dto.*;
import com.techmanage.entity.QrLoginSession;
import com.techmanage.entity.Role;
import com.techmanage.entity.User;
import com.techmanage.repository.QrLoginSessionRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.security.JwtTokenProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QrLoginService {

    private final QrLoginSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final WechatApiService wechatApiService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public QrLoginService(QrLoginSessionRepository sessionRepository,
                          UserRepository userRepository,
                          WechatApiService wechatApiService,
                          JwtTokenProvider jwtTokenProvider,
                          AuthenticationManager authenticationManager) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.wechatApiService = wechatApiService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public QrSessionResponse createSession() {
        String sessionId = UUID.randomUUID().toString();
        String shortCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        QrLoginSession session = new QrLoginSession();
        session.setSessionId(sessionId);
        session.setShortCode(shortCode);
        session.setStatus("WAITING");
        session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        sessionRepository.save(session);

        return QrSessionResponse.waiting(sessionId, shortCode);
    }

    public QrSessionResponse pollStatus(String sessionId) {
        var session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus("EXPIRED");
            sessionRepository.save(session);
            return QrSessionResponse.expired(sessionId, session.getShortCode());
        }

        if ("CONFIRMED".equals(session.getStatus())) {
            User user = userRepository.findById(session.getUserId()).orElse(null);
            if (user == null) {
                return QrSessionResponse.waiting(sessionId, session.getShortCode());
            }
            var roles = user.getRoles().stream().map(Role::getCode).toList();
            var userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getName(),
                user.getDepartment(), user.getPosition(), roles
            );
            return QrSessionResponse.confirmed(sessionId, session.getShortCode(), session.getToken(), userInfo);
        }

        if ("SCANNED".equals(session.getStatus())) {
            return QrSessionResponse.scanned(sessionId, session.getShortCode());
        }

        return QrSessionResponse.waiting(sessionId, session.getShortCode());
    }

    @Transactional
    public QrSessionResponse wechatLogin(String sessionId, String code) {
        var session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在或已过期"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus("EXPIRED");
            sessionRepository.save(session);
            return QrSessionResponse.expired(sessionId, session.getShortCode());
        }

        var wxSession = wechatApiService.code2session(code);
        String openId = wxSession.openId();

        var existingUser = userRepository.findByOpenId(openId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.isEnabled()) {
                throw new RuntimeException("账号已被禁用");
            }

            var roles = user.getRoles().stream().map(Role::getCode).toList();
            var token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roles);

            session.setStatus("CONFIRMED");
            session.setUserId(user.getId());
            session.setToken(token);
            sessionRepository.save(session);

            var userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getName(),
                user.getDepartment(), user.getPosition(), roles
            );
            return QrSessionResponse.confirmed(sessionId, session.getShortCode(), token, userInfo);
        }

        // OpenId not yet bound — mark as scanned, wait for bind
        session.setStatus("SCANNED");
        session.setToken(openId); // temporarily store openId in token field
        sessionRepository.save(session);
        return QrSessionResponse.scanned(sessionId, session.getShortCode());
    }

    @Transactional
    public QrSessionResponse bindAndLogin(String sessionId, String username, String password) {
        var session = sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("会话不存在或已过期"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus("EXPIRED");
            sessionRepository.save(session);
            return QrSessionResponse.expired(sessionId, session.getShortCode());
        }

        String openId = session.getToken(); // openId was stored here during scan
        if (openId == null || openId.isBlank()) {
            throw new RuntimeException("请先扫描二维码");
        }

        // Verify username/password
        var auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));
        User user = (User) auth.getPrincipal();

        if (!user.isEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }

        // Bind openId to user
        user.setOpenId(openId);
        userRepository.save(user);

        // Generate JWT
        var roles = user.getRoles().stream().map(Role::getCode).toList();
        var token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roles);

        session.setStatus("CONFIRMED");
        session.setUserId(user.getId());
        session.setToken(token);
        sessionRepository.save(session);

        var userInfo = new LoginResponse.UserInfo(
            user.getId(), user.getUsername(), user.getName(),
            user.getDepartment(), user.getPosition(), roles
        );
        return QrSessionResponse.confirmed(sessionId, session.getShortCode(), token, userInfo);
    }

    @Transactional
    public LoginResponse miniappLogin(String code) {
        var wxSession = wechatApiService.code2session(code);
        String openId = wxSession.openId();

        var existingUser = userRepository.findByOpenId(openId)
            .orElseThrow(() -> new RuntimeException("未绑定账号，请先在网页端扫码绑定"));

        if (!existingUser.isEnabled()) {
            throw new BadCredentialsException("账号已被禁用");
        }

        var roles = existingUser.getRoles().stream().map(Role::getCode).toList();
        var token = jwtTokenProvider.generateToken(existingUser.getId(), existingUser.getUsername(), roles);

        var userInfo = new LoginResponse.UserInfo(
            existingUser.getId(), existingUser.getUsername(), existingUser.getName(),
            existingUser.getDepartment(), existingUser.getPosition(), roles
        );
        return new LoginResponse(token, userInfo);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
