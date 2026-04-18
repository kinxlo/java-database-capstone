package com.project.back_end.mvc;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DashboardController {

    private final com.project.back_end.services.Service service;

    public DashboardController(com.project.back_end.services.Service service) {
        this.service = service;
    }

    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        Map<String, String> errors = service.validateToken(token, "admin");
        if (errors.isEmpty()) {
            return "admin/adminDashboard";
        }
        return "redirect:/";
    }

    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        Map<String, String> errors = service.validateToken(token, "doctor");
        if (errors.isEmpty()) {
            return "doctor/doctorDashboard";
        }
        return "redirect:/";
    }
}
