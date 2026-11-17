package com.fraud.detector.web;

import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.model.HarcamaGrafikNokta;
import com.fraud.detector.model.HesapOzet;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.MusteriProfilService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class MusteriProfilController {

    private final MusteriProfilService musteriProfilService;

    public MusteriProfilController(MusteriProfilService musteriProfilService) {
        this.musteriProfilService = musteriProfilService;
    }

    @GetMapping("/musteri/harcama-profilim")
    public String grafik(@RequestParam(required = false) Integer hesapId, Model model,
                          @AuthenticationPrincipal AppUserPrincipal kullanici) {
        List<HesapOzet> hesaplar = musteriProfilService.hesaplarim(kullanici.getId());
        if (hesaplar.isEmpty()) {
            model.addAttribute("hesaplar", hesaplar);
            return "musteri/harcama-profilim";
        }
        int secilenHesapId = (hesapId != null) ? hesapId : hesaplar.get(0).id();

        List<HarcamaGrafikNokta> noktalar = musteriProfilService.grafik(secilenHesapId);
        LocalDate buAy = LocalDate.now().withDayOfMonth(1);

        List<String> etiketler = new ArrayList<>();
        List<BigDecimal> gercekDizi = new ArrayList<>();
        List<BigDecimal> beklenenDizi = new ArrayList<>();
        List<BigDecimal> ustSinirDizi = new ArrayList<>();
        for (int i = 0; i < noktalar.size(); i++) {
            HarcamaGrafikNokta n = noktalar.get(i);
            String etiket = n.ay().getMonth().getDisplayName(TextStyle.SHORT, new Locale("tr")) + " " + n.ay().getYear();
            boolean devamEdiyor = (i == noktalar.size() - 1) && n.ay().equals(buAy);
            etiketler.add(devamEdiyor ? etiket + " (devam ediyor)" : etiket);
            gercekDizi.add(n.gercek());
            beklenenDizi.add(n.beklenen());
            ustSinirDizi.add(n.ustSinir());
        }

        model.addAttribute("hesaplar", hesaplar);
        model.addAttribute("secilenHesapId", secilenHesapId);
        model.addAttribute("etiketler", etiketler);
        model.addAttribute("gercekDizi", gercekDizi);
        model.addAttribute("beklenenDizi", beklenenDizi);
        model.addAttribute("ustSinirDizi", ustSinirDizi);
        model.addAttribute("sonIndex", noktalar.size() - 1);
        model.addAttribute("sonAyDevamEdiyor", !noktalar.isEmpty()
                && noktalar.get(noktalar.size() - 1).ay().equals(buAy));
        return "musteri/harcama-profilim";
    }

    @GetMapping("/musteri/ayarlarim")
    public String ayarlarimForm(Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        model.addAttribute("ayar", musteriProfilService.ayarGetir(kullanici.getId()));
        model.addAttribute("hesaplar", musteriProfilService.hesaplarim(kullanici.getId()));
        return "musteri/ayarlarim";
    }

    @PostMapping("/musteri/ayarlarim")
    public String ayarlarimGuncelle(@RequestParam BigDecimal toleransYuzde,
                                     @RequestParam(required = false) Boolean otomatikTolerans,
                                     @RequestParam(required = false) Boolean bildirimAktif,
                                     @AuthenticationPrincipal AppUserPrincipal kullanici,
                                     RedirectAttributes ra) {
        try {
            musteriProfilService.ayarGuncelle(kullanici.getId(), toleransYuzde,
                    Boolean.TRUE.equals(otomatikTolerans), Boolean.TRUE.equals(bildirimAktif));

            StringBuilder mesaj = new StringBuilder("Ayarlar güncellendi. Yeni üst sınırınız: ");
            List<HesapOzet> hesaplar = musteriProfilService.hesaplarim(kullanici.getId());
            for (int i = 0; i < hesaplar.size(); i++) {
                HesapOzet h = hesaplar.get(i);
                BigDecimal sinir = musteriProfilService.ustSinir(h.id());
                if (i > 0) {
                    mesaj.append(", ");
                }
                mesaj.append(sinir != null ? sinir + " TL" : "henüz hesaplanamadı");
            }
            ra.addFlashAttribute("bilgi", mesaj.toString());
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        }
        return "redirect:/musteri/ayarlarim";
    }
}
