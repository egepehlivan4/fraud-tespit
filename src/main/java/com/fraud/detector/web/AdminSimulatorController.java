package com.fraud.detector.web;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.AdminSimulatorService;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/islem-simulatoru")
public class AdminSimulatorController {

    private final AdminSimulatorService adminSimulatorService;

    public AdminSimulatorController(AdminSimulatorService adminSimulatorService) {
        this.adminSimulatorService = adminSimulatorService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("kartlar", adminSimulatorService.kartSecenekleri());
        model.addAttribute("isyeriler", adminSimulatorService.isyeriSecenekleri());
        model.addAttribute("cihazlar", adminSimulatorService.cihazSecenekleri());
        model.addAttribute("bugun", LocalDate.now());
        model.addAttribute("suAnSaat", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        return "admin/islem-simulatoru";
    }

    @PostMapping
    public String serbestIslem(@RequestParam int kartId, @RequestParam int isyeriId,
                                @RequestParam BigDecimal tutar,
                                @RequestParam(required = false) String cihazId,
                                @RequestParam(required = false) String saat,
                                @AuthenticationPrincipal AppUserPrincipal admin, RedirectAttributes ra) {
        try {
            Integer cihaz = (cihazId == null || cihazId.isBlank()) ? null : Integer.valueOf(cihazId);
            LocalTime zamanSaat = (saat == null || saat.isBlank()) ? null : LocalTime.parse(saat);
            String mesaj = adminSimulatorService.serbestIslem(kartId, isyeriId, tutar, cihaz, zamanSaat, admin.getId());
            ra.addFlashAttribute("bilgi", mesaj);
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        } catch (IsKuraliIhlaliException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:/admin/islem-simulatoru";
    }

    @PostMapping("/senaryo/{tip}")
    public String senaryo(@PathVariable String tip, @RequestParam int kartId,
                           @AuthenticationPrincipal AppUserPrincipal admin, RedirectAttributes ra) {
        try {
            String mesaj = switch (tip) {
                case "gece" -> adminSimulatorService.geceSenaryosu(kartId, admin.getId());
                case "hiz" -> adminSimulatorService.hizSenaryosu(kartId, admin.getId());
                case "imkansiz-seyahat" -> adminSimulatorService.imkansizSeyahatSenaryosu(kartId, admin.getId());
                case "yuksek-tutar" -> adminSimulatorService.yuksekTutarSenaryosu(kartId, admin.getId());
                default -> "Bilinmeyen senaryo.";
            };
            ra.addFlashAttribute("bilgi", mesaj);
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/admin/islem-simulatoru";
    }
}
