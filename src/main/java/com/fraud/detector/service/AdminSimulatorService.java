package com.fraud.detector.service;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.model.AlarmAciklama;
import com.fraud.detector.model.CihazSecenek;
import com.fraud.detector.model.Isyeri;
import com.fraud.detector.model.Kart;
import com.fraud.detector.repository.IslemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Analist islem olusturmaz; bu simulator sadece ADMIN'e acik (AdminSimulatorController
 * /admin/** altinda). Hazir senaryo butonlari secili kart icin kendi isyeri/tutar/tarih
 * degerlerini kullanir, boylece sunumda tek tikla GECE/HIZ/IMKANSIZ_SEYAHAT/YUKSEK_TUTAR
 * tetiklenebilir.
 */
@Service
public class AdminSimulatorService {

    private static final BigDecimal YUKSEK_TUTAR_DEMO = new BigDecimal("5000");
    private static final int ANKARA_ISYERI_ID = 1;    // Kızılay Market
    private static final int IZMIR_ISYERI_ID = 7;     // Alsancak Elektronik

    private final IslemRepository islemRepository;
    private final KartService kartService;
    private final IsyeriService isyeriService;

    public AdminSimulatorService(IslemRepository islemRepository, KartService kartService,
                                  IsyeriService isyeriService) {
        this.islemRepository = islemRepository;
        this.kartService = kartService;
        this.isyeriService = isyeriService;
    }

    public List<Kart> kartSecenekleri() {
        return kartService.listele(null);
    }

    public List<Isyeri> isyeriSecenekleri() {
        return isyeriService.listele();
    }

    public List<CihazSecenek> cihazSecenekleri() {
        return islemRepository.cihazSecenekleri();
    }

    /**
     * Tarih kasten yok — sadece BUGUN icin saat secilebilir. Gerekce: baska bir aya
     * tarihlenmis bir islem fn_islem_profil'i ya henuz bitmemis bir ayi modele kalici
     * ogretmeye ya da zaten ogrenilmis bir ayin toplamini degistirmeye zorlar.
     */
    public String serbestIslem(int kartId, int isyeriId, BigDecimal tutar, Integer cihazId,
                                LocalTime saat, int adminId) {
        LocalDateTime zaman = bugunSaatOlustur(saat);
        long islemId = islemRepository.ekleSimulasyon(kartId, isyeriId, cihazId, tutar, zaman, adminId);
        return sonucMesaji(islemId);
    }

    public String geceSenaryosu(int kartId, int adminId) {
        int isyeriId = varsayilanIsyeri(kartId);
        LocalDateTime hedef = LocalDate.now().atTime(3, 0);
        LocalDateTime simdi = LocalDateTime.now();
        LocalDateTime gece = hedef.isAfter(simdi) ? simdi : hedef;
        long islemId = islemRepository.ekleSimulasyon(kartId, isyeriId, null, new BigDecimal("150"), gece, adminId);
        return sonucMesaji(islemId);
    }

    /** Sunucu tarafi kontrol: tarih her zaman bugun, saat su andan ileri olamaz. */
    private LocalDateTime bugunSaatOlustur(LocalTime saat) {
        LocalDateTime simdi = LocalDateTime.now();
        if (saat == null) {
            return simdi;
        }
        LocalDateTime zaman = LocalDate.now().atTime(saat);
        if (zaman.isAfter(simdi)) {
            throw new IsKuraliIhlaliException("Seçilen saat şu andan ileri olamaz.");
        }
        return zaman;
    }

    /** Zaman geriye dogru kaydirilir: ilk islem(ler) gecmiste, SON islem = su an. */
    @Transactional
    public String hizSenaryosu(int kartId, int adminId) {
        int isyeriId = varsayilanIsyeri(kartId);
        LocalDateTime simdi = LocalDateTime.now();
        islemRepository.ekleSimulasyon(kartId, isyeriId, null, new BigDecimal("50"), simdi.minusMinutes(4), adminId);
        islemRepository.ekleSimulasyon(kartId, isyeriId, null, new BigDecimal("60"), simdi.minusMinutes(2), adminId);
        long sonIslemId = islemRepository.ekleSimulasyon(
                kartId, isyeriId, null, new BigDecimal("70"), simdi, adminId);
        return sonucMesaji(sonIslemId);
    }

    /** Zaman geriye dogru kaydirilir: Ankara gecmiste, Izmir (son islem) = su an. */
    @Transactional
    public String imkansizSeyahatSenaryosu(int kartId, int adminId) {
        LocalDateTime simdi = LocalDateTime.now();
        islemRepository.ekleSimulasyon(
                kartId, ANKARA_ISYERI_ID, null, new BigDecimal("80"), simdi.minusMinutes(10), adminId);
        long sonIslemId = islemRepository.ekleSimulasyon(
                kartId, IZMIR_ISYERI_ID, null, new BigDecimal("90"), simdi, adminId);
        return sonucMesaji(sonIslemId);
    }

    public String yuksekTutarSenaryosu(int kartId, int adminId) {
        int isyeriId = varsayilanIsyeri(kartId);
        long islemId = islemRepository.ekleSimulasyon(
                kartId, isyeriId, null, YUKSEK_TUTAR_DEMO, LocalDateTime.now(), adminId);
        return sonucMesaji(islemId);
    }

    private int varsayilanIsyeri(int kartId) {
        return islemRepository.sonIsyeri(kartId).orElse(ANKARA_ISYERI_ID);
    }

    private String sonucMesaji(long islemId) {
        int risk = islemRepository.riskPuani(islemId);
        List<AlarmAciklama> kurallar = islemRepository.kuralIhlalleri(islemId);
        boolean alarm = islemRepository.alarmVarMi(islemId);

        StringBuilder mesaj = new StringBuilder("İşlem eklendi (id=" + islemId + "). Risk puanı: " + risk + ".");
        if (!kurallar.isEmpty()) {
            mesaj.append(" Tetiklenen kurallar: ");
            for (int i = 0; i < kurallar.size(); i++) {
                if (i > 0) {
                    mesaj.append(", ");
                }
                mesaj.append(kurallar.get(i).kural()).append(" (+").append(kurallar.get(i).puan()).append(")");
            }
            mesaj.append(".");
        }
        mesaj.append(alarm ? " Alarm açıldı." : " Alarm açılmadı.");
        return mesaj.toString();
    }
}
