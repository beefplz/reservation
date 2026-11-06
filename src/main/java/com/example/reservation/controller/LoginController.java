package com.example.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인 "화면"만 보여준다.
 * 실제 로그인 처리(POST /login)는 Spring Security 필터가 컨트롤러보다 먼저 가로채서 한다.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginForm() {
        return "member/login";
    }
}
