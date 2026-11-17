package com.fraud.detector.web;

import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.BildirimService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/musteri/bildirimlerim")
public class MusteriBildirimController {

    private final BildirimService bildirimService;

    public MusteriBildirimController(BildirimService bildirimService) {
        this.bildirimService = bildirimService;
    }

    @GetMapping
    public String liste(Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        model.addAttribute("bildirimler", bildirimService.listele(kullanici.getId()));
        return "musteri/bildirimlerim";
    }

    @PostMapping("/{id}/okundu")
    public String okunduIsaretle(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        bildirimService.okunduIsaretle(id, kullanici.getId());
        return "redirect:/musteri/bildirimlerim";
    }
}
