package com.fraud.detector.web;

import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.KuralService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/kurallar")
public class AdminKuralController {

    private final KuralService kuralService;

    public AdminKuralController(KuralService kuralService) {
        this.kuralService = kuralService;
    }

    @GetMapping
    public String listele(Model model) {
        model.addAttribute("kurallar", kuralService.listele());
        model.addAttribute("alarmEsik", kuralService.alarmEsikGetir());
        return "admin/kural-liste";
    }

    @GetMapping("/yeni")
    public String yeniForm(Model model) {
        model.addAttribute("tipler", kuralService.tipler());
        return "admin/kural-form";
    }

    @PostMapping
    public String ekle(@RequestParam String kod, @RequestParam String ad, @RequestParam int puan,
                        @RequestParam(required = false) BigDecimal esik,
                        @AuthenticationPrincipal AppUserPrincipal aktifKullanici, RedirectAttributes ra) {
        try {
            kuralService.ekle(kod, ad, puan, esik, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "Kural eklendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/admin/kurallar";
    }

    @GetMapping("/{id}/duzenle")
    public String duzenleForm(@PathVariable int id, Model model, RedirectAttributes ra) {
        return kuralService.bul(id)
                .map(k -> {
                    model.addAttribute("kural", k);
                    return "admin/kural-duzenle";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("hata", "Kural bulunamadı.");
                    return "redirect:/admin/kurallar";
                });
    }

    @PostMapping("/{id}")
    public String guncelle(@PathVariable int id, @RequestParam String ad, @RequestParam int puan,
                            @RequestParam(required = false) BigDecimal esik,
                            @RequestParam(required = false) Boolean aktif,
                            @AuthenticationPrincipal AppUserPrincipal aktifKullanici, RedirectAttributes ra) {
        try {
            kuralService.guncelle(id, ad, puan, esik, Boolean.TRUE.equals(aktif), aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "Kural güncellendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/admin/kurallar";
    }

    @PostMapping("/{id}/sil")
    public String sil(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal aktifKullanici,
                       RedirectAttributes ra) {
        boolean tamamenSilindi = kuralService.sil(id, aktifKullanici.getId());
        ra.addFlashAttribute("bilgi", tamamenSilindi
                ? "Kural tamamen silindi."
                : "Bu kuralın geçmiş alarm/tespit kaydı olduğu için tamamen silinemedi; pasif duruma alındı.");
        return "redirect:/admin/kurallar";
    }

    @PostMapping("/alarm-esik")
    public String alarmEsikGuncelle(@RequestParam BigDecimal deger,
                                     @AuthenticationPrincipal AppUserPrincipal aktifKullanici,
                                     RedirectAttributes ra) {
        try {
            kuralService.alarmEsikGuncelle(deger, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "Alarm eşiği güncellendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/admin/kurallar";
    }
}
