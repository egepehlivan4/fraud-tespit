package com.fraud.detector.web;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.KartService;
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
@RequestMapping("/admin/kartlar")
public class AdminKartController {

    private final KartService kartService;

    public AdminKartController(KartService kartService) {
        this.kartService = kartService;
    }

    @GetMapping
    public String listele(@RequestParam(required = false) String durum, Model model) {
        String durumFiltre = (durum == null || durum.isBlank()) ? null : durum;
        model.addAttribute("kartlar", kartService.listele(durumFiltre));
        model.addAttribute("secilenDurum", durum);
        return "admin/kart-liste";
    }

    @GetMapping("/yeni")
    public String yeniForm(Model model) {
        model.addAttribute("hesaplar", kartService.hesapSecenekleri());
        return "admin/kart-form";
    }

    @PostMapping
    public String ekle(@RequestParam int hesapId, @RequestParam String son4,
                        @AuthenticationPrincipal AppUserPrincipal aktifKullanici, RedirectAttributes ra) {
        try {
            kartService.ekle(hesapId, son4, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "Kart tanımlandı.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        } catch (IsKuraliIhlaliException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:/admin/kartlar";
    }

    @PostMapping("/{id}/durum")
    public String durumDegistir(@PathVariable int id, @RequestParam String yeniDurum,
                                 @AuthenticationPrincipal AppUserPrincipal aktifKullanici,
                                 RedirectAttributes ra) {
        try {
            kartService.durumDegistir(id, yeniDurum, aktifKullanici.getId());
            ra.addFlashAttribute("bilgi", "Kart durumu güncellendi.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        } catch (IsKuraliIhlaliException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:/admin/kartlar";
    }
}
