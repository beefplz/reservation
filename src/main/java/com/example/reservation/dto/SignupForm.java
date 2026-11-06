package com.example.reservation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 회원가입 화면의 입력값. 컨트롤러에서 @Valid로 검증한다. */
@Getter
@Setter
public class SignupForm {

    @NotBlank(message = "아이디를 입력하세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문/숫자 4~20자입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordConfirm;

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 50, message = "이름은 50자 이내입니다.")
    private String name;

    /** 비밀번호와 비밀번호 확인이 같은지. 오류는 passwordMatching 필드에 붙는다. */
    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordMatching() {
        if (password == null || passwordConfirm == null) {
            return true; // 빈 값은 @NotBlank가 따로 잡는다
        }
        return password.equals(passwordConfirm);
    }
}
