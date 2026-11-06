package com.example.reservation.controller;

import com.example.reservation.dto.SignupForm;
import com.example.reservation.exception.DuplicateLoginIdException;
import com.example.reservation.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/signup")
    public String signupForm(@ModelAttribute("form") SignupForm form) {
        return "member/signup";
    }

    /** 컨트롤러는 입력 검증 결과 처리와 화면 이동만 한다. 가입 규칙은 MemberService에 있다. */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignupForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "member/signup";
        }
        try {
            memberService.signup(form);
        } catch (DuplicateLoginIdException e) {
            bindingResult.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다.");
            return "member/signup";
        }
        // 기본 로그인 화면은 /login?signup 같은 쿼리를 인식하지 못해 404가 나므로 /login 으로 보낸다.
        // ("가입 완료" 안내는 로그인 화면을 직접 만들 때 추가)
        return "redirect:/login";
    }
}
