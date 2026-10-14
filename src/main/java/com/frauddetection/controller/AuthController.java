package com.frauddetection.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) Boolean error,
            @RequestParam(required = false) Boolean logout,
            Model model) {

        if (Boolean.TRUE.equals(error)) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (Boolean.TRUE.equals(logout)) {
            model.addAttribute("logout", "You have been signed out.");
        }

        return "auth/login";
    }
}
