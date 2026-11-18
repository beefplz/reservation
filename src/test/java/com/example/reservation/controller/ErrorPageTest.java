package com.example.reservation.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공통 오류 화면(templates/error.html).
 * MockMvc 는 실제 서블릿 컨테이너처럼 오류를 /error 로 넘기지 않으므로,
 * 컨테이너가 넘길 때 붙이는 요청 속성(상태 코드, 예외)을 직접 넣고 /error 를 호출한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorPageTest {

    @Autowired MockMvc mvc;

    @ParameterizedTest(name = "[{index}] {0} → \"{1}\"")
    @CsvSource({
            "404, 페이지를 찾을 수 없습니다",
            "403, 접근 권한이 없습니다",
            "400, 잘못된 요청입니다",
            "500, 일시적인 오류가 발생했습니다",
            "405, 요청을 처리하지 못했습니다",
    })
    @DisplayName("상태 코드별로 한국어 안내 화면을 보여준다 (Whitelabel 아님)")
    void errorPage_byStatus(int statusCode, String title) throws Exception {
        // 브라우저처럼 HTML 을 요청해야 화면이 나온다 (Accept 가 없으면 BasicErrorController 는 JSON 으로 응답)
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, statusCode))
                .andExpect(status().is(statusCode))
                .andExpect(content().string(containsString(title)))
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    @DisplayName("500 화면에 예외 메시지나 스택 트레이스 같은 내부 정보가 나오지 않는다")
    void errorPage_hidesInternalDetails() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION,
                                new IllegalStateException("Access denied for user 'root'")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("Access denied"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at com.example"))));
    }
}
