package com.myticket.backend.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketTestController {

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, String> ping(String message) {
        return Map.of("response", "pong", "originalMessage", message != null ? message : "");
    }
}
