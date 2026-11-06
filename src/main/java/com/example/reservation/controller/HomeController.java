package com.example.reservation.controller;

import com.example.reservation.security.LoginMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /** @AuthenticationPrincipal: 세션에 저장된 로그인 회원(LoginMember)을 바로 받는다. */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("loginMember", loginMember);
        return "home";
    }
}
