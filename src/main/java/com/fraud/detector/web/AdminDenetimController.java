package com.fraud.detector.web;

import com.fraud.detector.service.DenetimLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/denetim-log")
public class AdminDenetimController {

    private final DenetimLogService denetimLogService;

    public AdminDenetimController(DenetimLogService denetimLogService) {
        this.denetimLogService = denetimLogService;
    }

    @GetMapping
    public String listele(@RequestParam(required = false) String tablo,
                           @RequestParam(defaultValue = "1") int sayfa, Model model) {
        String tabloFiltre = (tablo == null || tablo.isBlank()) ? null : tablo;
        model.addAttribute("satirlar", denetimLogService.listele(tabloFiltre, sayfa));
        model.addAttribute("toplamSayfa", denetimLogService.toplamSayfa(tabloFiltre));
        model.addAttribute("sayfa", Math.max(sayfa, 1));
        model.addAttribute("secilenTablo", tablo);
        return "admin/denetim-log";
    }
}
