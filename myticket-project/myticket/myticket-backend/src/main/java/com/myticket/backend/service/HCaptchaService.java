package com.myticket.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.hcaptcha.enabled", havingValue = "true")
public class HCaptchaService implements CaptchaService {

    private final String siteSecret;
    private final RestTemplate restTemplate;

    public HCaptchaService(@Value("${app.hcaptcha.secret}") String siteSecret) {
        this.siteSecret = siteSecret;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean verify(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret", siteSecret);
        map.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://hcaptcha.com/siteverify", request, Map.class);
            
            if (response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                return Boolean.TRUE.equals(success);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
