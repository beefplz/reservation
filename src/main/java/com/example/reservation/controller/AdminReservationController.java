package com.example.reservation.controller;

import com.example.reservation.domain.ReservationStatus;
import com.example.reservation.exception.ReservationException;
import com.example.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 관리자 예약 승인/거절. /admin/** 는 SecurityConfig 에서 ADMIN 만 접근 가능. 화면은 JSP. */
@Controller
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private static final String REDIRECT_PENDING = "redirect:/admin/reservations?status=PENDING";

    private final ReservationService reservationService;

    /** status 파라미터가 없으면 전체, 있으면 그 상태만. 잘못된 값(enum 에 없는 이름)은 400. */
    @GetMapping
    public String list(@RequestParam(required = false) ReservationStatus status, Model model) {
        model.addAttribute("reservations", reservationService.findForAdmin(status));
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", ReservationStatus.values());
        return "admin/reservation/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.approve(id);
            redirectAttributes.addFlashAttribute("successMessage", "예약을 승인했습니다.");
        } catch (ReservationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return REDIRECT_PENDING;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.reject(id);
            redirectAttributes.addFlashAttribute("successMessage", "예약을 거절했습니다.");
        } catch (ReservationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return REDIRECT_PENDING;
    }
}
