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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String signup(@Valid @ModelAttribute("form") SignupForm form, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "member/signup";
        }
        try {
            memberService.signup(form);
        } catch (DuplicateLoginIdException e) {
            bindingResult.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다.");
            return "member/signup";
        }
        // flash 속성: 리다이렉트 직후 한 번만 보이고 사라지는 값 (새로고침하면 안내 문구가 없어짐)
        redirectAttributes.addFlashAttribute("signupSuccess", true);
        return "redirect:/login";
    }
}
