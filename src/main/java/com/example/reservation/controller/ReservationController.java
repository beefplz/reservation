package com.example.reservation.controller;

import com.example.reservation.dto.ReservationForm;
import com.example.reservation.exception.ReservationException;
import com.example.reservation.security.LoginMember;
import com.example.reservation.service.ReservationService;
import com.example.reservation.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** 예약 신청과 내 예약 목록. 화면은 JSP (/WEB-INF/views/reservation/*.jsp). */
@Controller
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private static final String FORM_VIEW = "reservation/form";

    private final ReservationService reservationService;
    private final RoomService roomService;

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long roomId, Model model) {
        ReservationForm form = new ReservationForm();
        form.setRoomId(roomId);
        form.setDate(LocalDate.now());
        model.addAttribute("form", form);
        model.addAttribute("rooms", roomService.findActiveRooms());
        return FORM_VIEW;
    }

    /**
     * 신청자는 폼이 아니라 로그인 정보(@AuthenticationPrincipal)에서 꺼낸다.
     * memberId 를 폼으로 받으면 남의 이름으로 예약할 수 있기 때문이다.
     */
    @PostMapping
    public String create(@AuthenticationPrincipal LoginMember loginMember,
                         @Valid @ModelAttribute("form") ReservationForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", roomService.findActiveRooms());
            return FORM_VIEW;
        }
        try {
            reservationService.create(loginMember.getId(), form);
        } catch (ReservationException e) {
            // 겹침, 비활성 회의실, 시간 규칙 위반 → 같은 화면에 이유를 보여준다
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("rooms", roomService.findActiveRooms());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "예약을 신청했습니다. 관리자 승인 후 확정됩니다.");
        return "redirect:/reservations/my";
    }

    @GetMapping("/my")
    public String myReservations(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("reservations", reservationService.findMyReservations(loginMember.getId()));
        return "reservation/my";
    }
}
