package com.fraud.detector.web;

import com.fraud.detector.model.SehirKategoriRaporSatiri;
import com.fraud.detector.service.RaporService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/analist/raporlar")
public class AnalistRaporController {

    private final RaporService raporService;

    public AnalistRaporController(RaporService raporService) {
        this.raporService = raporService;
    }

    @GetMapping
    public String sehirKategori(@RequestParam(required = false) String bas,
                                 @RequestParam(required = false) String bit,
                                 Model model) {
        LocalDate basTarih = (bas == null || bas.isBlank()) ? LocalDate.now().minusDays(90) : LocalDate.parse(bas);
        LocalDate bitTarih = (bit == null || bit.isBlank()) ? LocalDate.now() : LocalDate.parse(bit);

        List<SehirKategoriRaporSatiri> satirlar = raporService.sehirKategoriRaporu(basTarih, bitTarih);
        List<String> etiketler = new ArrayList<>();
        List<BigDecimal> oranlar = new ArrayList<>();
        for (SehirKategoriRaporSatiri s : satirlar) {
            etiketler.add(s.sehir() + " — " + s.kategori());
            oranlar.add(s.alarmOrani());
        }

        model.addAttribute("satirlar", satirlar);
        model.addAttribute("etiketler", etiketler);
        model.addAttribute("oranlar", oranlar);
        model.addAttribute("bas", basTarih);
        model.addAttribute("bit", bitTarih);
        return "analist/raporlar";
    }

    @GetMapping("/fraud-halkasi")
    public String fraudHalkasi(@RequestParam(required = false) Integer hesapId, Model model) {
        if (hesapId != null) {
            model.addAttribute("sonuclar", raporService.fraudHalkasi(hesapId));
        }
        model.addAttribute("secilenHesapId", hesapId);
        return "analist/fraud-halkasi";
    }
}
