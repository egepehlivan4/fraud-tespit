package com.fraud.detector.web;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.KullaniciService;
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
@RequestMapping("/admin/kullanicilar")
public class AdminKullaniciController {

    private final KullaniciService kullaniciService;

    public AdminKullaniciController(KullaniciService kullaniciService) {
        this.kullaniciService = kullaniciService;
    }

    @GetMapping
    public String listele(@RequestParam(required = false) String rol,
                           @RequestParam(required = false) String aktif,
                           Model model) {
        String rolFiltre = (rol == null || rol.isBlank()) ? null : rol;
        Boolean aktifFiltre = (aktif == null || aktif.isBlank()) ? null : Boolean.valueOf(aktif);

        model.addAttribute("kullanicilar", kullaniciService.listele(rolFiltre, aktifFiltre));
        model.addAttribute("roller", kullaniciService.roller());
        model.addAttribute("secilenRol", rolFiltre);
        model.addAttribute("secilenAktif", aktif);
        return "admin/kullanici-liste";
    }

    @GetMapping("/yeni")
    public String yeniForm(Model model) {
        model.addAttribute("roller", kullaniciService.roller());
        return "admin/kullanici-form";
    }

    @PostMapping
    public String ekle(@RequestParam String adSoyad, @RequestParam String email,
                        @RequestParam String sifre, @RequestParam int rolId,
                        @AuthenticationPrincipal AppUserPrincipal aktifKullanici,
                        RedirectAttributes ra) {
        try {
            kullaniciService.ekle(adSoyad, email, sifre, rolId, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "Kullanıcı eklendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        } catch (IsKuraliIhlaliException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:/admin/kullanicilar";
    }

    @GetMapping("/{id}/duzenle")
    public String duzenleForm(@PathVariable int id, Model model, RedirectAttributes ra) {
        return kullaniciService.bul(id)
                .map(k -> {
                    model.addAttribute("kullanici", k);
                    model.addAttribute("roller", kullaniciService.roller());
                    return "admin/kullanici-duzenle";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("hata", "Kullanıcı bulunamadı.");
                    return "redirect:/admin/kullanicilar";
                });
    }

    @PostMapping("/{id}")
    public String guncelle(@PathVariable int id, @RequestParam String adSoyad,
                            @RequestParam String email, @RequestParam int rolId,
                            @RequestParam(required = false) Boolean aktif,
                            @RequestParam(required = false) String sifre,
                            @AuthenticationPrincipal AppUserPrincipal aktifKullanici,
                            RedirectAttributes ra) {
        try {
            kullaniciService.guncelle(id, adSoyad, email, rolId, Boolean.TRUE.equals(aktif),
                    sifre, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "Kullanıcı güncellendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        } catch (IsKuraliIhlaliException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:/admin/kullanicilar";
    }

    @PostMapping("/{id}/sil")
    public String sil(@PathVariable int id,
                       @AuthenticationPrincipal AppUserPrincipal aktifKullanici,
                       RedirectAttributes ra) {
        try {
            boolean tamamenSilindi = kullaniciService.sil(id, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", tamamenSilindi
                    ? "Kullanıcı tamamen silindi."
                    : "Bu kullanıcının geçmiş kaydı (işlem/alarm) olduğu için tamamen silinemedi; pasif duruma alındı.");
        } catch (IsKuraliIhlaliException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:/admin/kullanicilar";
    }
}
