package com.fraud.detector.web;

import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.MusteriOnayService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/musteri/bekleyen-onay")
public class MusteriOnayController {

    private final MusteriOnayService musteriOnayService;

    public MusteriOnayController(MusteriOnayService musteriOnayService) {
        this.musteriOnayService = musteriOnayService;
    }

    @GetMapping
    public String liste(Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        model.addAttribute("bekleyenler", musteriOnayService.bekleyenler(kullanici.getId()));
        return "musteri/bekleyen-onay";
    }

    @PostMapping("/{id}/onayla")
    public String onayla(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal kullanici,
                          RedirectAttributes ra) {
        try {
            musteriOnayService.onayla(id, kullanici.getId());
            ra.addFlashAttribute("bilgi", "Teşekkürler, işlemi siz yapmışsınız olarak işaretledik.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/musteri/bekleyen-onay";
    }

    @PostMapping("/{id}/reddet")
    public String reddet(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal kullanici,
                          RedirectAttributes ra) {
        try {
            musteriOnayService.reddet(id, kullanici.getId());
            ra.addFlashAttribute("bilgi", "Kartınız bloke edildi, tutar hesabınıza iade edildi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/musteri/bekleyen-onay";
    }
}
