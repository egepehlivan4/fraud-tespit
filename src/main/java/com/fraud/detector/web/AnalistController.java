package com.fraud.detector.web;

import com.fraud.detector.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalistController {

    @GetMapping("/analist")
    public String anaSayfa(Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        model.addAttribute("adSoyad", kullanici.getAdSoyad());
        return "analist/home";
    }
}
