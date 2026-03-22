package com.myticket.backend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.hcaptcha.enabled", havingValue = "false", matchIfMissing = true)
public class MockCaptchaService implements CaptchaService {

    @Override
    public boolean verify(String token) {
        // In offline mode, assume captcha is always valid
        System.out.println("MOCK CAPTCHA: Verified token " + token);
        return true;
    }
}
