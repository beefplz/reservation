package com.example.reservation.controller;

import com.example.reservation.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 로그인한 사용자용 회의실 목록. 활성 회의실만 보여준다. */
@Controller
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/rooms")
    public String list(Model model) {
        model.addAttribute("rooms", roomService.findActiveRooms());
        return "room/list";
    }
}
