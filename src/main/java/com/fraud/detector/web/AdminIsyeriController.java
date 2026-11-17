package com.fraud.detector.web;

import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.IsyeriService;
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

@Controller
@RequestMapping("/admin/isyeriler")
public class AdminIsyeriController {

    private final IsyeriService isyeriService;

    public AdminIsyeriController(IsyeriService isyeriService) {
        this.isyeriService = isyeriService;
    }

    @GetMapping
    public String listele(Model model) {
        model.addAttribute("isyeriler", isyeriService.listele());
        return "admin/isyeri-liste";
    }

    @GetMapping("/yeni")
    public String yeniForm(Model model) {
        model.addAttribute("kategoriler", isyeriService.kategoriler());
        model.addAttribute("sehirler", isyeriService.sehirler());
        return "admin/isyeri-form";
    }

    @PostMapping
    public String ekle(@RequestParam String ad, @RequestParam int kategoriId, @RequestParam int sehirId,
                        @AuthenticationPrincipal AppUserPrincipal aktifKullanici, RedirectAttributes ra) {
        try {
            isyeriService.ekle(ad, kategoriId, sehirId, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "İşyeri eklendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/admin/isyeriler";
    }

    @GetMapping("/{id}/duzenle")
    public String duzenleForm(@PathVariable int id, Model model, RedirectAttributes ra) {
        return isyeriService.bul(id)
                .map(y -> {
                    model.addAttribute("isyeri", y);
                    model.addAttribute("kategoriler", isyeriService.kategoriler());
                    model.addAttribute("sehirler", isyeriService.sehirler());
                    return "admin/isyeri-duzenle";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("hata", "İşyeri bulunamadı.");
                    return "redirect:/admin/isyeriler";
                });
    }

    @PostMapping("/{id}")
    public String guncelle(@PathVariable int id, @RequestParam String ad,
                            @RequestParam int kategoriId, @RequestParam int sehirId,
                            @AuthenticationPrincipal AppUserPrincipal aktifKullanici, RedirectAttributes ra) {
        try {
            isyeriService.guncelle(id, ad, kategoriId, sehirId, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "İşyeri güncellendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/admin/isyeriler";
    }

    @PostMapping("/{id}/sil")
    public String sil(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal aktifKullanici,
                       RedirectAttributes ra) {
        try {
            isyeriService.sil(id, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "İşyeri silindi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/admin/isyeriler";
    }
}
