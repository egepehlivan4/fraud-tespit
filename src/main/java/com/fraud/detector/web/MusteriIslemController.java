package com.fraud.detector.web;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.common.VeriHatasiCozumleyici;
import com.fraud.detector.security.AppUserPrincipal;
import com.fraud.detector.service.IsyeriService;
import com.fraud.detector.service.MusteriIslemService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/musteri")
public class MusteriIslemController {

    private final MusteriIslemService musteriIslemService;
    private final IsyeriService isyeriService;

    public MusteriIslemController(MusteriIslemService musteriIslemService, IsyeriService isyeriService) {
        this.musteriIslemService = musteriIslemService;
        this.isyeriService = isyeriService;
    }

    @GetMapping("/kartlarim")
    public String kartlarim(Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        model.addAttribute("hesaplar", musteriIslemService.hesaplarim(kullanici.getId()));
        model.addAttribute("kartlar", musteriIslemService.kartlarim(kullanici.getId()));
        return "musteri/kartlarim";
    }

    @GetMapping("/harcama-yap")
    public String harcamaYapForm(Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        model.addAttribute("kartlar", musteriIslemService.kartlarim(kullanici.getId()));
        model.addAttribute("isyeriler", isyeriService.listele());
        return "musteri/harcama-yap";
    }

    @PostMapping("/harcama-yap")
    public String harcamaYap(@RequestParam int kartId, @RequestParam int isyeriId,
                              @RequestParam BigDecimal tutar,
                              @AuthenticationPrincipal AppUserPrincipal kullanici, RedirectAttributes ra) {
        try {
            boolean alarmVarMi = musteriIslemService.harcamaYap(kartId, isyeriId, tutar, kullanici.getId());
            ra.addFlashAttribute("bilgi", alarmVarMi
                    ? "İşleminiz güvenlik kontrolüne alındı."
                    : "İşlem onaylandı.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("hata", VeriHatasiCozumleyici.anlasilirMesaj(e));
        } catch (IsKuraliIhlaliException e) {
            ra.addFlashAttribute("hata", e.getMessage());
        }
        return "redirect:/musteri/harcama-yap";
    }

    @GetMapping("/islemlerim")
    public String islemlerim(@RequestParam(required = false) Integer kartId,
                              @RequestParam(required = false) String bas,
                              @RequestParam(required = false) String bit,
                              @RequestParam(defaultValue = "1") int sayfa,
                              Model model, @AuthenticationPrincipal AppUserPrincipal kullanici) {
        LocalDate basTarih = (bas == null || bas.isBlank()) ? null : LocalDate.parse(bas);
        LocalDate bitTarih = (bit == null || bit.isBlank()) ? null : LocalDate.parse(bit);

        model.addAttribute("islemler",
                musteriIslemService.gecmis(kullanici.getId(), kartId, basTarih, bitTarih, sayfa));
        model.addAttribute("toplamSayfa",
                musteriIslemService.gecmisToplamSayfa(kullanici.getId(), kartId, basTarih, bitTarih));
        model.addAttribute("sayfa", Math.max(sayfa, 1));
        model.addAttribute("kartlar", musteriIslemService.kartlarim(kullanici.getId()));
        model.addAttribute("secilenKart", kartId);
        model.addAttribute("bas", bas);
        model.addAttribute("bit", bit);
        return "musteri/islemlerim";
    }
}
