package com.fraud.detector.service;

import com.fraud.detector.common.IsKuraliIhlaliException;
import com.fraud.detector.model.Kullanici;
import com.fraud.detector.model.Rol;
import com.fraud.detector.repository.KullaniciRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class KullaniciService {

    private static final String MUSTERI_ROL_ADI = "MUSTERI";

    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder passwordEncoder;

    public KullaniciService(KullaniciRepository kullaniciRepository, PasswordEncoder passwordEncoder) {
        this.kullaniciRepository = kullaniciRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Kullanici> listele(String rolFiltre, Boolean aktifFiltre) {
        return kullaniciRepository.listele(rolFiltre, aktifFiltre);
    }

    public List<Rol> roller() {
        return kullaniciRepository.roller();
    }

    public Optional<Kullanici> bul(int id) {
        return kullaniciRepository.bul(id);
    }

    /**
     * @Transactional: kullanici INSERT'i ile (MUSTERI ise) hesap INSERT'i tek
     * transaction'da birlesir — hesap acma basarisiz olursa kullanici da geri alinir.
     */
    @Transactional
    public void ekle(String adSoyad, String email, String sifreDuz, int rolId, int aktifKullaniciId) {
        String sifreHash = passwordEncoder.encode(sifreDuz);
        int yeniId = kullaniciRepository.ekle(adSoyad, email, sifreHash, rolId, aktifKullaniciId);

        Rol rol = roller().stream().filter(r -> r.id() == rolId).findFirst()
                .orElseThrow(() -> new IsKuraliIhlaliException("Geçersiz rol seçildi."));
        if (MUSTERI_ROL_ADI.equals(rol.ad())) {
            kullaniciRepository.hesapAc(yeniId, aktifKullaniciId);
        }
    }

    public void guncelle(int id, String adSoyad, String email, int rolId, boolean aktif,
                          String yeniSifreDuzYaDaBos, int aktifKullaniciId) {
        if (id == aktifKullaniciId && !aktif) {
            throw new IsKuraliIhlaliException("Kendi hesabınızı pasif yapamazsınız.");
        }
        String yeniSifreHash = (yeniSifreDuzYaDaBos == null || yeniSifreDuzYaDaBos.isBlank())
                ? null
                : passwordEncoder.encode(yeniSifreDuzYaDaBos);
        kullaniciRepository.guncelle(id, adSoyad, email, rolId, aktif, yeniSifreHash, aktifKullaniciId);
    }

    /**
     * Once kalici silme denenir. FK ihlali (iliskili islem/alarm kaydi) varsa
     * kalicaSil kendi transaction'inda rollback olur (bu metod KASTEN
     * @Transactional degil, boylece asagidaki pasifYap cagrisi TAMAMEN YENI
     * ve temiz bir transaction'da calisir).
     *
     * @return true: tamamen silindi, false: FK nedeniyle pasife alindi
     */
    public boolean sil(int id, int aktifKullaniciId) {
        if (id == aktifKullaniciId) {
            throw new IsKuraliIhlaliException("Kendi hesabınızı silemezsiniz.");
        }
        try {
            kullaniciRepository.kalicaSil(id, aktifKullaniciId);
            return true;
        } catch (DataIntegrityViolationException e) {
            kullaniciRepository.pasifYap(id, aktifKullaniciId);
            return false;
        }
    }
}
