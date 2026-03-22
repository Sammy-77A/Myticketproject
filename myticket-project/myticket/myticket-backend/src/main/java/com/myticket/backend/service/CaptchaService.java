package com.myticket.backend.service;

public interface CaptchaService {
    boolean verify(String token);
}
