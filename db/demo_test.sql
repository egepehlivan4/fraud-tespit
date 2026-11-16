-- =====================================================================
--  DEMO SENARYOSU — fraud_db.sql çalıştırıldıktan sonra
-- =====================================================================
SET search_path TO fraud;

-- 1) Ayşe (kart 1, Ankara) normal bir alışveriş yapıyor → alarm yok
INSERT INTO islem(kart_id, isyeri_id, cihaz_id, tutar, tarih)
VALUES (1, 1, 1, 250, DATE_TRUNC('day', NOW()) + INTERVAL '14 hours');

-- 2) 10 dk sonra İzmir'de elektronik + yüksek tutar → İMKÂNSIZ SEYAHAT
INSERT INTO islem(kart_id, isyeri_id, cihaz_id, tutar, tarih)
VALUES (1, 7, 4, 9000, DATE_TRUNC('day', NOW()) + INTERVAL '14 hours 10 minutes');

SELECT id, kart_id, tutar, risk_puani FROM islem ORDER BY id DESC LIMIT 2;
SELECT * FROM alarm;

-- 3) Analist alarmın nedenini görüyor
SELECT * FROM fn_alarm_aciklama(1);

-- 4) Müşteri "bu işlemi ben yapmadım" diyor → kart bloke, para iade
CALL sp_musteri_teyit(1, 3, FALSE);
SELECT k.id, k.durum, h.bakiye FROM kart k JOIN hesap h ON h.id = k.hesap_id WHERE k.id = 1;

-- 5) Bloke kartla yeni işlem → trigger reddeder
DO $$ BEGIN
  INSERT INTO islem(kart_id, isyeri_id, tutar) VALUES (1, 1, 50);
EXCEPTION WHEN OTHERS THEN RAISE NOTICE 'Beklenen hata: %', SQLERRM;
END $$;

-- 6) Kapanmış alarmın kararı değiştirilemez
DO $$ BEGIN
  CALL sp_alarm_karar(1, 2, 'TEMIZ');
EXCEPTION WHEN OTHERS THEN RAISE NOTICE 'Beklenen hata: %', SQLERRM;
END $$;

-- 7) HIZ kuralı: Mehmet gece 3 dakikada 3 işlem, sonuncusu yüklü → alarm, analist FRAUD der
INSERT INTO islem(kart_id, isyeri_id, tutar, tarih) VALUES
 (2, 5, 100, DATE_TRUNC('day', NOW()) + INTERVAL '3 hours'),
 (2, 5, 120, DATE_TRUNC('day', NOW()) + INTERVAL '3 hours 1 minute'),
 (2, 6, 2500, DATE_TRUNC('day', NOW()) + INTERVAL '3 hours 2 minutes');
SELECT a.id, a.islem_id, a.toplam_puan, a.durum FROM alarm a ORDER BY a.id;
SELECT * FROM fn_alarm_aciklama(2);
CALL sp_alarm_karar(2, 2, 'FRAUD');

-- 8) Admin kuralı canlı değiştiriyor (denetim log'a düşer)
SELECT set_config('app.kullanici_id', '1', FALSE);
UPDATE kural SET puan = 50 WHERE kod = 'GECE';

-- 9) Admin kartı tekrar açıyor
CALL sp_kart_aktif_et(1, 1);

-- 10) Denetim log'u
SELECT tablo, kayit_id, islem_tipi, kullanici_id,
       eski->>'durum' AS eski_durum, yeni->>'durum' AS yeni_durum,
       eski->>'puan'  AS eski_puan,  yeni->>'puan'  AS yeni_puan
FROM denetim_log WHERE islem_tipi = 'UPDATE' ORDER BY id;

-- 11) Rapor
SELECT * FROM fn_rapor_sehir_kategori(CURRENT_DATE - 90, CURRENT_DATE)
WHERE alarm_sayisi > 0;

-- 12) Fraud halkası: Can'ın (hesap 4) hesabıyla bağlantılı hesaplar
SELECT * FROM fn_fraud_halkasi(4);

-- =====================================================================
--  KİŞİSEL HARCAMA PROFİLİ
-- =====================================================================

-- 13) Öğrenilen profiller
SELECT p.hesap_id, u.ad_soyad, p.ortalama, round(sqrt(p.varyans),2) AS std_sapma,
       p.egitilen_ay_sayisi, fn_ust_sinir(p.hesap_id) AS ust_sinir
FROM harcama_profili p JOIN hesap h ON h.id = p.hesap_id
JOIN kullanici u ON u.id = h.kullanici_id ORDER BY p.hesap_id;

-- 14) Zeynep'in (hesap 3) grafiği: model her ay yukarı kayıyor
SELECT * FROM fn_harcama_grafik(3);

-- 15) Zeynep gündüz büyük bir alışveriş yapıyor → fraud alarmı yok ama "bu hızla sınırı aşacaksın" bildirimi var
INSERT INTO islem(kart_id, isyeri_id, cihaz_id, tutar, tarih)
VALUES (3, 7, 3, 6000, DATE_TRUNC('day', NOW()) + INTERVAL '13 hours');
SELECT id, risk_puani FROM islem ORDER BY id DESC LIMIT 1;
SELECT kullanici_id, tip, mesaj FROM bildirim WHERE hesap_id = 3;

-- 16) Zeynep toleransını %50'ye çıkarıyor → sınır anında değişir
UPDATE musteri_ayar SET tolerans_yuzde = 50 WHERE kullanici_id = 5;
SELECT fn_ust_sinir(3) AS yeni_sinir;

-- 17) Tüm bildirimler
SELECT b.id, u.ad_soyad, b.tip, b.mesaj FROM bildirim b
JOIN kullanici u ON u.id = b.kullanici_id ORDER BY b.id;
