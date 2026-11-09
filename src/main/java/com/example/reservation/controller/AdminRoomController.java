package com.example.reservation.controller;

import com.example.reservation.dto.RoomForm;
import com.example.reservation.exception.DuplicateRoomNameException;
import com.example.reservation.exception.RoomHasUpcomingReservationsException;
import com.example.reservation.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 관리자용 회의실 관리. /admin/** 는 SecurityConfig 에서 ADMIN 만 접근하도록 막고 있다. */
@Controller
@RequestMapping("/admin/rooms")
@RequiredArgsConstructor
public class AdminRoomController {

    private static final String FORM_VIEW = "admin/room/form";
    private static final String REDIRECT_LIST = "redirect:/admin/rooms";

    private final RoomService roomService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", roomService.findAllRooms());
        return "admin/room/list";
    }

    @GetMapping("/new")
    public String createForm(@ModelAttribute("form") RoomForm form) {
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") RoomForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            roomService.create(form);
        } catch (DuplicateRoomNameException e) {
            bindingResult.rejectValue("name", "duplicate", "이미 사용 중인 회의실 이름입니다.");
            return FORM_VIEW;
        }
        return REDIRECT_LIST;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", RoomForm.from(roomService.getRoom(id)));
        model.addAttribute("roomId", id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") RoomForm form,
                         BindingResult bindingResult, Model model) {
        model.addAttribute("roomId", id); // 오류로 화면에 돌아갈 때 수정 주소를 유지
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            roomService.update(id, form);
        } catch (DuplicateRoomNameException e) {
            bindingResult.rejectValue("name", "duplicate", "이미 사용 중인 회의실 이름입니다.");
            return FORM_VIEW;
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deactivate(id);
        } catch (RoomHasUpcomingReservationsException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id) {
        roomService.activate(id);
        return REDIRECT_LIST;
    }
}
