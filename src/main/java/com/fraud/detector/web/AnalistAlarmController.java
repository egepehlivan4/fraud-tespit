package com.fraud.detector.web;

import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.AnalistAlarmService;
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
@RequestMapping("/analist/alarmlar")
public class AnalistAlarmController {

    private final AnalistAlarmService analistAlarmService;

    public AnalistAlarmController(AnalistAlarmService analistAlarmService) {
        this.analistAlarmService = analistAlarmService;
    }

    @GetMapping
    public String listele(@RequestParam(required = false) String durum, Model model) {
        String durumFiltre = (durum == null || durum.isBlank()) ? null : durum;
        model.addAttribute("alarmlar", analistAlarmService.listele(durumFiltre));
        model.addAttribute("secilenDurum", durum);
        return "analist/alarm-liste";
    }

    @GetMapping("/{id}")
    public String detay(@PathVariable int id, Model model, RedirectAttributes ra) {
        return analistAlarmService.detay(id)
                .map(alarm -> {
                    model.addAttribute("alarm", alarm);
                    model.addAttribute("aciklama", analistAlarmService.aciklama(id));
                    model.addAttribute("sonIslemler", analistAlarmService.sonIslemler(alarm.kullaniciId()));
                    model.addAttribute("fraudHalkasi", analistAlarmService.fraudHalkasi(alarm.hesapId()));
                    return "analist/alarm-detay";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("hata", "Alarm bulunamadı.");
                    return "redirect:/analist/alarmlar";
                });
    }

    @PostMapping("/{id}/karar")
    public String karar(@PathVariable int id, @RequestParam String karar,
                         @AuthenticationPrincipal AppUserPrincipal analist, RedirectAttributes ra) {
        try {
            analistAlarmService.karar(id, analist.getId(), karar);
            ra.addFlashAttribute("bilgi", "Karar kaydedildi: " + karar);
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/analist/alarmlar/" + id;
    }
}
