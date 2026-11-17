-- =====================================================================
--  FRAUD TESPİT SİSTEMİ — PostgreSQL şeması
--  BMT-311 Veri Tabanı Yönetim Sistemleri dönem projesi
--  Bölümler: 1) Tablolar  2) Yardımcı fonksiyonlar  3) Trigger'lar
--            4) Stored procedure'ler  5) Rapor fonksiyonları  6) Örnek veri
-- =====================================================================

DROP SCHEMA IF EXISTS fraud CASCADE;
CREATE SCHEMA fraud;
SET search_path TO fraud;

-- =====================================================================
-- 1) TABLOLAR (3NF)
-- =====================================================================

CREATE TABLE rol (
    id   SERIAL PRIMARY KEY,
    ad   VARCHAR(20) NOT NULL UNIQUE          -- MUSTERI, ANALIST, ADMIN
);

CREATE TABLE kullanici (
    id          SERIAL PRIMARY KEY,
    ad_soyad    VARCHAR(100) NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    sifre_hash  VARCHAR(100) NOT NULL,
    rol_id      INT NOT NULL REFERENCES rol(id),
    aktif       BOOLEAN NOT NULL DEFAULT TRUE,
    olusturma   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sehir (
    id      SERIAL PRIMARY KEY,
    ad      VARCHAR(50) NOT NULL UNIQUE,
    enlem   NUMERIC(9,6) NOT NULL,
    boylam  NUMERIC(9,6) NOT NULL
);

CREATE TABLE kategori (
    id  SERIAL PRIMARY KEY,
    ad  VARCHAR(50) NOT NULL UNIQUE           -- Market, Elektronik, Kuyumcu...
);

CREATE TABLE isyeri (
    id           SERIAL PRIMARY KEY,
    ad           VARCHAR(100) NOT NULL,
    kategori_id  INT NOT NULL REFERENCES kategori(id),
    sehir_id     INT NOT NULL REFERENCES sehir(id)
);

CREATE TABLE hesap (
    id            SERIAL PRIMARY KEY,
    kullanici_id  INT NOT NULL REFERENCES kullanici(id) ON DELETE CASCADE,
    iban          CHAR(26) NOT NULL UNIQUE,
    bakiye        NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (bakiye >= 0)
);

CREATE TABLE kart (
    id         SERIAL PRIMARY KEY,
    hesap_id   INT NOT NULL REFERENCES hesap(id) ON DELETE CASCADE,
    son4       CHAR(4) NOT NULL,
    durum      VARCHAR(10) NOT NULL DEFAULT 'AKTIF'
               CHECK (durum IN ('AKTIF','BLOKE','IPTAL')),
    olusturma  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE cihaz (
    id           SERIAL PRIMARY KEY,
    parmak_izi   VARCHAR(64) NOT NULL UNIQUE,  -- tarayıcı/cihaz kimliği
    tanim        VARCHAR(100)
);

CREATE TABLE islem (
    id           BIGSERIAL PRIMARY KEY,
    kart_id      INT NOT NULL REFERENCES kart(id),
    isyeri_id    INT NOT NULL REFERENCES isyeri(id),
    cihaz_id     INT REFERENCES cihaz(id),
    ip           INET,
    tutar        NUMERIC(12,2) NOT NULL CHECK (tutar > 0),
    tarih        TIMESTAMP NOT NULL DEFAULT NOW(),
    durum        VARCHAR(12) NOT NULL DEFAULT 'ONAYLANDI'
                 CHECK (durum IN ('ONAYLANDI','REDDEDILDI')),
    risk_puani   INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_islem_kart_tarih ON islem(kart_id, tarih);
CREATE INDEX ix_islem_cihaz      ON islem(cihaz_id);

-- Kurallar: puan ve eşik admin panelinden değiştirilebilir
-- kod: trigger'in tanidigi 5 sabit TIP'ten biri (UNIQUE degil) — ayni tipten
-- birden fazla kademeli kural tanimlanabilir (orn. YUKSEK_TUTAR icin 5x/+40
-- ve 15x/+60). fn_islem_risk her aktif satiri kendi puan/esik'i ile ayri ayri
-- degerlendirdigi icin bu trigger'da hicbir degisiklik gerektirmez.
CREATE TABLE kural (
    id     SERIAL PRIMARY KEY,
    kod    VARCHAR(30) NOT NULL
           CHECK (kod IN ('GECE', 'YUKSEK_TUTAR', 'YENI_ISYERI', 'HIZ', 'IMKANSIZ_SEYAHAT')),
    ad     VARCHAR(100) NOT NULL,
    puan   INT NOT NULL CHECK (puan BETWEEN 0 AND 100),
    esik   NUMERIC(10,2),                      -- kurala göre anlamı değişir
    aktif  BOOLEAN NOT NULL DEFAULT TRUE
);

-- Genel ayarlar (ör. alarm eşiği)
CREATE TABLE ayar (
    anahtar  VARCHAR(30) PRIMARY KEY,
    deger    NUMERIC(10,2) NOT NULL
);

-- Açıklanabilirlik: bir işlemde hangi kural kaç puan verdi
CREATE TABLE kural_ihlal (
    islem_id  BIGINT NOT NULL REFERENCES islem(id) ON DELETE CASCADE,
    kural_id  INT    NOT NULL REFERENCES kural(id),
    puan      INT    NOT NULL,
    aciklama  VARCHAR(200),
    PRIMARY KEY (islem_id, kural_id)
);

CREATE TABLE alarm (
    id             SERIAL PRIMARY KEY,
    islem_id       BIGINT NOT NULL UNIQUE REFERENCES islem(id) ON DELETE CASCADE,
    toplam_puan    INT NOT NULL,
    durum          VARCHAR(10) NOT NULL DEFAULT 'BEKLIYOR'
                   CHECK (durum IN ('BEKLIYOR','FRAUD','TEMIZ')),
    musteri_teyit  BOOLEAN,                    -- NULL: cevap yok
    karar_veren_id INT REFERENCES kullanici(id),
    karar_tarihi   TIMESTAMP,
    olusturma      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE denetim_log (
    id            BIGSERIAL PRIMARY KEY,
    tablo         VARCHAR(30) NOT NULL,
    kayit_id      BIGINT,
    islem_tipi    VARCHAR(6) NOT NULL,         -- INSERT / UPDATE / DELETE
    eski          JSONB,
    yeni          JSONB,
    kullanici_id  INT,                          -- uygulamadan gelen kullanıcı
    zaman         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- Kişisel harcama profili (online öğrenen model) ----------

-- Müşterinin kendi belirlediği tolerans
CREATE TABLE musteri_ayar (
    kullanici_id       INT PRIMARY KEY REFERENCES kullanici(id) ON DELETE CASCADE,
    tolerans_yuzde     NUMERIC(5,2) NOT NULL DEFAULT 20
                       CHECK (tolerans_yuzde BETWEEN 5 AND 200),
    otomatik_tolerans  BOOLEAN NOT NULL DEFAULT FALSE,  -- TRUE: sınır = ort + 2σ
    bildirim_aktif     BOOLEAN NOT NULL DEFAULT TRUE
);

-- Hesap bazında aylık toplam harcama (trigger ile güncel tutulur)
CREATE TABLE aylik_harcama (
    hesap_id      INT  NOT NULL REFERENCES hesap(id) ON DELETE CASCADE,
    ay            DATE NOT NULL,                  -- ayın ilk günü
    toplam        NUMERIC(14,2) NOT NULL DEFAULT 0,
    islem_sayisi  INT NOT NULL DEFAULT 0,
    PRIMARY KEY (hesap_id, ay)
);

-- Modelin kendisi: üstel ağırlıklı hareketli ortalama (EWMA) ve varyans
CREATE TABLE harcama_profili (
    hesap_id            INT PRIMARY KEY REFERENCES hesap(id) ON DELETE CASCADE,
    ortalama            NUMERIC(14,2),            -- öğrenilen aylık beklenti
    varyans             NUMERIC(20,4) NOT NULL DEFAULT 0,
    alfa                NUMERIC(3,2)  NOT NULL DEFAULT 0.30
                        CHECK (alfa > 0 AND alfa < 1),  -- öğrenme hızı
    egitilen_ay_sayisi  INT NOT NULL DEFAULT 0,
    son_ay              DATE,                     -- modele katılan son tam ay
    guncelleme          TIMESTAMP
);

-- Her ay modelin ne beklediği ve gerçekte ne olduğu (grafik için)
CREATE TABLE profil_gecmisi (
    hesap_id   INT  NOT NULL REFERENCES hesap(id) ON DELETE CASCADE,
    ay         DATE NOT NULL,
    gercek     NUMERIC(14,2) NOT NULL,
    beklenen   NUMERIC(14,2),
    ust_sinir  NUMERIC(14,2),
    PRIMARY KEY (hesap_id, ay)
);

CREATE TABLE bildirim (
    id            SERIAL PRIMARY KEY,
    kullanici_id  INT NOT NULL REFERENCES kullanici(id) ON DELETE CASCADE,
    hesap_id      INT REFERENCES hesap(id) ON DELETE CASCADE,
    tip           VARCHAR(20) NOT NULL
                  CHECK (tip IN ('LIMIT_ASIMI','TEMPO','FRAUD_TEYIT')),
    mesaj         VARCHAR(300) NOT NULL,
    ay            DATE,
    okundu        BOOLEAN NOT NULL DEFAULT FALSE,
    olusturma     TIMESTAMP NOT NULL DEFAULT NOW()
);
-- Bütçe bildirimleri ayda bir kez gider
CREATE UNIQUE INDEX ux_bildirim_butce ON bildirim(hesap_id, tip, ay)
    WHERE tip IN ('LIMIT_ASIMI','TEMPO');

-- =====================================================================
-- 2) YARDIMCI FONKSİYONLAR
-- =====================================================================

-- İki koordinat arası mesafe (km) — Haversine
CREATE FUNCTION fn_mesafe_km(lat1 NUMERIC, lon1 NUMERIC, lat2 NUMERIC, lon2 NUMERIC)
RETURNS NUMERIC AS $$
DECLARE
    r CONSTANT NUMERIC := 6371;
    dlat NUMERIC := radians(lat2 - lat1);
    dlon NUMERIC := radians(lon2 - lon1);
    a NUMERIC;
BEGIN
    a := sin(dlat/2)^2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlon/2)^2;
    RETURN round((2 * r * asin(sqrt(a)))::NUMERIC, 1);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Uygulamanın "SET app.kullanici_id = X" ile verdiği kullanıcıyı okur
CREATE FUNCTION fn_aktif_kullanici() RETURNS INT AS $$
    SELECT NULLIF(current_setting('app.kullanici_id', TRUE), '')::INT;
$$ LANGUAGE sql STABLE;

-- =====================================================================
-- 3) TRIGGER'LAR
-- =====================================================================

-- 3.1 BEFORE INSERT: bloke kart ve yetersiz bakiye kontrolü
CREATE FUNCTION fn_islem_on_kontrol() RETURNS trigger AS $$
DECLARE
    v_durum  VARCHAR;
    v_bakiye NUMERIC;
BEGIN
    SELECT k.durum, h.bakiye INTO v_durum, v_bakiye
    FROM kart k JOIN hesap h ON h.id = k.hesap_id
    WHERE k.id = NEW.kart_id;

    IF v_durum <> 'AKTIF' THEN
        RAISE EXCEPTION 'Kart % durumda, işlem reddedildi', v_durum;
    END IF;
    IF v_bakiye < NEW.tutar THEN
        RAISE EXCEPTION 'Yetersiz bakiye (% TL)', v_bakiye;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_islem_on_kontrol
BEFORE INSERT ON islem
FOR EACH ROW EXECUTE FUNCTION fn_islem_on_kontrol();

-- 3.2 AFTER INSERT: bakiyeden düş + risk puanı hesapla + gerekirse alarm aç
CREATE FUNCTION fn_islem_risk() RETURNS trigger AS $$
DECLARE
    r           RECORD;
    v_toplam    INT := 0;
    v_esik      NUMERIC;
    v_sayi      INT;
    v_ort       NUMERIC;
    v_onceki    RECORD;
    v_km        NUMERIC;
    v_saat      NUMERIC;
    v_hiz       NUMERIC;
BEGIN
    -- bakiyeden düş
    UPDATE hesap SET bakiye = bakiye - NEW.tutar
    WHERE id = (SELECT hesap_id FROM kart WHERE id = NEW.kart_id);

    FOR r IN SELECT * FROM kural WHERE aktif LOOP

        -- GECE: 00:00–05:59 arası işlem
        IF r.kod = 'GECE' AND EXTRACT(HOUR FROM NEW.tarih) < 6 THEN
            INSERT INTO kural_ihlal VALUES (NEW.id, r.id, r.puan,
                'Gece saatinde işlem (' || to_char(NEW.tarih,'HH24:MI') || ')');
            v_toplam := v_toplam + r.puan;

        -- YUKSEK_TUTAR: kartın son 90 gün ortalamasının esik katı
        ELSIF r.kod = 'YUKSEK_TUTAR' THEN
            SELECT AVG(tutar), COUNT(*) INTO v_ort, v_sayi
            FROM islem
            WHERE kart_id = NEW.kart_id AND id <> NEW.id
              AND tarih BETWEEN NEW.tarih - INTERVAL '90 days' AND NEW.tarih;
            IF v_sayi >= 3 AND NEW.tutar > v_ort * r.esik THEN
                INSERT INTO kural_ihlal VALUES (NEW.id, r.id, r.puan,
                    'Tutar ortalamanın ' || round(NEW.tutar / v_ort, 1) || ' katı');
                v_toplam := v_toplam + r.puan;
            END IF;

        -- YENI_ISYERI: kart bu işyerinde ilk kez kullanılıyor
        ELSIF r.kod = 'YENI_ISYERI' THEN
            IF NOT EXISTS (SELECT 1 FROM islem
                           WHERE kart_id = NEW.kart_id AND isyeri_id = NEW.isyeri_id
                             AND id <> NEW.id) THEN
                INSERT INTO kural_ihlal VALUES (NEW.id, r.id, r.puan,
                    'Kart bu işyerinde ilk kez kullanıldı');
                v_toplam := v_toplam + r.puan;
            END IF;

        -- HIZ: son 10 dakikada esik kadar veya daha fazla işlem
        ELSIF r.kod = 'HIZ' THEN
            SELECT COUNT(*) INTO v_sayi FROM islem
            WHERE kart_id = NEW.kart_id
              AND tarih BETWEEN NEW.tarih - INTERVAL '10 minutes' AND NEW.tarih;
            IF v_sayi >= r.esik THEN
                INSERT INTO kural_ihlal VALUES (NEW.id, r.id, r.puan,
                    '10 dakikada ' || v_sayi || ' işlem');
                v_toplam := v_toplam + r.puan;
            END IF;

        -- IMKANSIZ_SEYAHAT: önceki işlemle arasındaki hız > esik km/saat
        ELSIF r.kod = 'IMKANSIZ_SEYAHAT' THEN
            SELECT i.tarih, s.ad, s.enlem, s.boylam INTO v_onceki
            FROM islem i
            JOIN isyeri y ON y.id = i.isyeri_id
            JOIN sehir  s ON s.id = y.sehir_id
            WHERE i.kart_id = NEW.kart_id AND i.id <> NEW.id AND i.tarih <= NEW.tarih
            ORDER BY i.tarih DESC LIMIT 1;

            IF FOUND THEN
                SELECT fn_mesafe_km(v_onceki.enlem, v_onceki.boylam, s.enlem, s.boylam)
                INTO v_km
                FROM isyeri y JOIN sehir s ON s.id = y.sehir_id
                WHERE y.id = NEW.isyeri_id;

                v_saat := EXTRACT(EPOCH FROM (NEW.tarih - v_onceki.tarih)) / 3600.0;
                IF v_km > 50 THEN
                    v_hiz := CASE WHEN v_saat = 0 THEN 99999 ELSE v_km / v_saat END;
                    IF v_hiz > r.esik THEN
                        INSERT INTO kural_ihlal VALUES (NEW.id, r.id, r.puan,
                            v_onceki.ad || ' → ' || v_km || ' km, '
                            || round(v_saat * 60) || ' dk içinde');
                        v_toplam := v_toplam + r.puan;
                    END IF;
                END IF;
            END IF;
        END IF;
    END LOOP;

    UPDATE islem SET risk_puani = v_toplam WHERE id = NEW.id;

    SELECT deger INTO v_esik FROM ayar WHERE anahtar = 'ALARM_ESIK';
    IF v_toplam >= v_esik THEN
        INSERT INTO alarm(islem_id, toplam_puan) VALUES (NEW.id, v_toplam);
        INSERT INTO bildirim(kullanici_id, hesap_id, tip, mesaj)
        SELECT h.kullanici_id, h.id, 'FRAUD_TEYIT',
               to_char(NEW.tarih, 'DD.MM HH24:MI') || ' tarihli ' || NEW.tutar
               || ' TL işlemi siz mi yaptınız?'
        FROM kart k JOIN hesap h ON h.id = k.hesap_id WHERE k.id = NEW.kart_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_islem_risk
AFTER INSERT ON islem
FOR EACH ROW EXECUTE FUNCTION fn_islem_risk();

-- 3.3 Denetim log'u (kart, kural, ayar, kullanici tablolarında her değişiklik)
CREATE FUNCTION fn_denetim() RETURNS trigger AS $$
BEGIN
    INSERT INTO denetim_log(tablo, kayit_id, islem_tipi, eski, yeni, kullanici_id)
    VALUES (
        TG_TABLE_NAME,
        CASE WHEN TG_OP = 'DELETE' THEN (to_jsonb(OLD)->>'id')::BIGINT
             ELSE (to_jsonb(NEW)->>'id')::BIGINT END,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) END,
        CASE WHEN TG_OP IN ('UPDATE','INSERT') THEN to_jsonb(NEW) END,
        fn_aktif_kullanici()
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_denetim_kart      AFTER INSERT OR UPDATE OR DELETE ON kart
    FOR EACH ROW EXECUTE FUNCTION fn_denetim();
CREATE TRIGGER trg_denetim_kural     AFTER INSERT OR UPDATE OR DELETE ON kural
    FOR EACH ROW EXECUTE FUNCTION fn_denetim();
CREATE TRIGGER trg_denetim_kullanici AFTER INSERT OR UPDATE OR DELETE ON kullanici
    FOR EACH ROW EXECUTE FUNCTION fn_denetim();

-- 3.4 Kapanmış alarmın kararı değiştirilemez
CREATE FUNCTION fn_alarm_kilit() RETURNS trigger AS $$
BEGIN
    IF OLD.durum <> 'BEKLIYOR' AND NEW.durum <> OLD.durum THEN
        RAISE EXCEPTION 'Alarm % zaten % olarak kapatılmış', OLD.id, OLD.durum;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_alarm_kilit
BEFORE UPDATE ON alarm
FOR EACH ROW EXECUTE FUNCTION fn_alarm_kilit();

-- 3.4b İptal edilen kartın durumu hiçbir şekilde değiştirilemez
CREATE FUNCTION fn_kart_iptal_kilit() RETURNS trigger AS $$
BEGIN
    IF OLD.durum = 'IPTAL' AND NEW.durum <> OLD.durum THEN
        RAISE EXCEPTION 'Kart % iptal edilmiş, durumu değiştirilemez', OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_kart_iptal_kilit
BEFORE UPDATE ON kart
FOR EACH ROW EXECUTE FUNCTION fn_kart_iptal_kilit();

-- ---------------------------------------------------------------------
-- 3.5 KİŞİSEL HARCAMA PROFİLİ — online öğrenme
--  Ay bitince o ayın toplamı modele katılır:
--     fark     = gerçek - ortalama
--     ortalama = ortalama + α·fark
--     varyans  = (1-α)·(varyans + α·fark²)
--  Böylece harcama alışkanlığı değiştikçe beklenti de kendiliğinden kayar.
-- ---------------------------------------------------------------------

-- Müşterinin tercihine göre bu ayki üst sınır
CREATE FUNCTION fn_ust_sinir(p_hesap_id INT) RETURNS NUMERIC AS $$
    SELECT CASE
             WHEN p.ortalama IS NULL THEN NULL
             WHEN COALESCE(m.otomatik_tolerans, FALSE)
                  THEN round(p.ortalama + 2 * sqrt(p.varyans), 2)
             ELSE round(p.ortalama * (1 + COALESCE(m.tolerans_yuzde, 20) / 100), 2)
           END
    FROM harcama_profili p
    JOIN hesap h ON h.id = p.hesap_id
    LEFT JOIN musteri_ayar m ON m.kullanici_id = h.kullanici_id
    WHERE p.hesap_id = p_hesap_id;
$$ LANGUAGE sql STABLE;

-- Tamamlanmış bir ayı modele öğret (tek adım)
CREATE FUNCTION fn_profil_ogren(p_hesap_id INT, p_ay DATE, p_toplam NUMERIC)
RETURNS VOID AS $$
DECLARE
    p     harcama_profili%ROWTYPE;
    fark  NUMERIC;
BEGIN
    SELECT * INTO p FROM harcama_profili WHERE hesap_id = p_hesap_id FOR UPDATE;

    -- tahmini kaydet (model bu ayı görmeden önce ne bekliyordu?)
    INSERT INTO profil_gecmisi(hesap_id, ay, gercek, beklenen, ust_sinir)
    VALUES (p_hesap_id, p_ay, p_toplam, p.ortalama, fn_ust_sinir(p_hesap_id))
    ON CONFLICT (hesap_id, ay) DO UPDATE SET gercek = EXCLUDED.gercek;

    IF p.ortalama IS NULL THEN
        p.ortalama := p_toplam;
        p.varyans  := 0;
    ELSE
        fark       := p_toplam - p.ortalama;
        p.ortalama := p.ortalama + p.alfa * fark;
        p.varyans  := (1 - p.alfa) * (p.varyans + p.alfa * fark * fark);
    END IF;

    UPDATE harcama_profili
    SET ortalama = round(p.ortalama, 2), varyans = p.varyans,
        egitilen_ay_sayisi = egitilen_ay_sayisi + 1,
        son_ay = p_ay, guncelleme = NOW()
    WHERE hesap_id = p_hesap_id;
END;
$$ LANGUAGE plpgsql;

-- Hesap açılınca boş profil oluştur
CREATE FUNCTION fn_hesap_profil_ac() RETURNS trigger AS $$
BEGIN
    INSERT INTO harcama_profili(hesap_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_hesap_profil_ac
AFTER INSERT ON hesap
FOR EACH ROW EXECUTE FUNCTION fn_hesap_profil_ac();

-- Her işlemde: biten ayları öğren → bu ayın toplamını güncelle → bildirim kontrolü
CREATE FUNCTION fn_islem_profil() RETURNS trigger AS $$
DECLARE
    v_hesap    INT;
    v_kul      INT;
    v_ay       DATE := date_trunc('month', NEW.tarih)::DATE;
    v_son_ay   DATE;
    v_toplam   NUMERIC;
    v_sinir    NUMERIC;
    v_gun      INT;
    v_ay_gun   INT;
    v_tahmin   NUMERIC;
    m          RECORD;
BEGIN
    SELECT h.id, h.kullanici_id INTO v_hesap, v_kul
    FROM kart k JOIN hesap h ON h.id = k.hesap_id WHERE k.id = NEW.kart_id;

    -- 1) Yeni aya girildiyse, kapanan ayları sırayla modele öğret
    SELECT son_ay INTO v_son_ay FROM harcama_profili WHERE hesap_id = v_hesap;
    FOR m IN SELECT ay, toplam FROM aylik_harcama
             WHERE hesap_id = v_hesap AND ay < v_ay
               AND (v_son_ay IS NULL OR ay > v_son_ay)
             ORDER BY ay
    LOOP
        PERFORM fn_profil_ogren(v_hesap, m.ay, m.toplam);
    END LOOP;

    -- 2) Bu ayın toplamını güncelle
    INSERT INTO aylik_harcama(hesap_id, ay, toplam, islem_sayisi)
    VALUES (v_hesap, v_ay, NEW.tutar, 1)
    ON CONFLICT (hesap_id, ay) DO UPDATE
       SET toplam = aylik_harcama.toplam + EXCLUDED.toplam,
           islem_sayisi = aylik_harcama.islem_sayisi + 1
    RETURNING toplam INTO v_toplam;

    -- 3) Bildirim kontrolü
    IF NOT COALESCE((SELECT bildirim_aktif FROM musteri_ayar
                     WHERE kullanici_id = v_kul), TRUE) THEN
        RETURN NEW;
    END IF;

    v_sinir := fn_ust_sinir(v_hesap);
    IF v_sinir IS NULL THEN RETURN NEW; END IF;   -- model henüz eğitilmedi

    IF v_toplam > v_sinir THEN
        INSERT INTO bildirim(kullanici_id, hesap_id, tip, mesaj, ay)
        VALUES (v_kul, v_hesap, 'LIMIT_ASIMI',
                'Bu ay ' || v_toplam || ' TL harcadınız. Size göre normal üst sınır '
                || v_sinir || ' TL idi.', v_ay)
        ON CONFLICT DO NOTHING;
    ELSE
        -- Tempo: bu hızla giderse ay sonunda sınırı aşacak mı?
        v_gun    := EXTRACT(DAY FROM NEW.tarih);
        v_ay_gun := EXTRACT(DAY FROM (v_ay + INTERVAL '1 month - 1 day'));
        v_tahmin := round(v_toplam / v_gun * v_ay_gun, 2);
        IF v_gun >= 5 AND v_tahmin > v_sinir THEN
            INSERT INTO bildirim(kullanici_id, hesap_id, tip, mesaj, ay)
            VALUES (v_kul, v_hesap, 'TEMPO',
                    'Bu hızla ay sonunda yaklaşık ' || v_tahmin
                    || ' TL harcayacaksınız (sınırınız ' || v_sinir || ' TL).', v_ay)
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_islem_profil
AFTER INSERT ON islem
FOR EACH ROW EXECUTE FUNCTION fn_islem_profil();

-- Fraud nedeniyle reddedilen işlem aylık harcamadan düşülür
CREATE FUNCTION fn_islem_iade() RETURNS trigger AS $$
BEGIN
    UPDATE aylik_harcama
    SET toplam = toplam - NEW.tutar, islem_sayisi = islem_sayisi - 1
    WHERE ay = date_trunc('month', NEW.tarih)::DATE
      AND hesap_id = (SELECT hesap_id FROM kart WHERE id = NEW.kart_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_islem_iade
AFTER UPDATE OF durum ON islem
FOR EACH ROW
WHEN (OLD.durum = 'ONAYLANDI' AND NEW.durum = 'REDDEDILDI')
EXECUTE FUNCTION fn_islem_iade();

-- =====================================================================
-- 4) STORED PROCEDURE'LER
-- =====================================================================

-- 4.0 Modeli geçmiş veriden sıfırdan eğit (ilk kurulum / yeniden eğitim)
CREATE PROCEDURE sp_profil_yeniden_egit(p_hesap_id INT)
LANGUAGE plpgsql AS $$
DECLARE
    m     RECORD;
    v_ay  DATE := date_trunc('month', NOW())::DATE;
BEGIN
    DELETE FROM aylik_harcama  WHERE hesap_id = p_hesap_id;
    DELETE FROM profil_gecmisi WHERE hesap_id = p_hesap_id;
    UPDATE harcama_profili
    SET ortalama = NULL, varyans = 0, egitilen_ay_sayisi = 0, son_ay = NULL
    WHERE hesap_id = p_hesap_id;

    INSERT INTO aylik_harcama(hesap_id, ay, toplam, islem_sayisi)
    SELECT k.hesap_id, date_trunc('month', i.tarih)::DATE, SUM(i.tutar), COUNT(*)
    FROM islem i JOIN kart k ON k.id = i.kart_id
    WHERE k.hesap_id = p_hesap_id AND i.durum = 'ONAYLANDI'
    GROUP BY 1, 2;

    FOR m IN SELECT ay, toplam FROM aylik_harcama
             WHERE hesap_id = p_hesap_id AND ay < v_ay ORDER BY ay
    LOOP
        PERFORM fn_profil_ogren(p_hesap_id, m.ay, m.toplam);
    END LOOP;
END;
$$;

-- =====================================================================

-- 4.1 Analist kararı: FRAUD ise kart bloke + işlem iade
CREATE PROCEDURE sp_alarm_karar(p_alarm_id INT, p_analist_id INT, p_karar VARCHAR)
LANGUAGE plpgsql AS $$
DECLARE
    v_islem RECORD;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM kullanici k JOIN rol r ON r.id = k.rol_id
                   WHERE k.id = p_analist_id AND r.ad IN ('ANALIST','ADMIN')) THEN
        RAISE EXCEPTION 'Bu işlem için analist yetkisi gerekli';
    END IF;
    IF p_karar NOT IN ('FRAUD','TEMIZ') THEN
        RAISE EXCEPTION 'Geçersiz karar: %', p_karar;
    END IF;

    PERFORM set_config('app.kullanici_id', p_analist_id::TEXT, TRUE);

    UPDATE alarm
    SET durum = p_karar, karar_veren_id = p_analist_id, karar_tarihi = NOW()
    WHERE id = p_alarm_id;

    IF p_karar = 'FRAUD' THEN
        SELECT i.* INTO v_islem FROM islem i JOIN alarm a ON a.islem_id = i.id
        WHERE a.id = p_alarm_id;

        UPDATE kart SET durum = 'BLOKE' WHERE id = v_islem.kart_id;
        UPDATE islem SET durum = 'REDDEDILDI' WHERE id = v_islem.id;
        UPDATE hesap SET bakiye = bakiye + v_islem.tutar
        WHERE id = (SELECT hesap_id FROM kart WHERE id = v_islem.kart_id);
    END IF;
END;
$$;

-- 4.2 Müşteri teyidi: "Bu işlemi siz mi yaptınız?"
CREATE PROCEDURE sp_musteri_teyit(p_alarm_id INT, p_kullanici_id INT, p_onay BOOLEAN)
LANGUAGE plpgsql AS $$
DECLARE
    v_islem RECORD;
BEGIN
    SELECT i.* INTO v_islem
    FROM alarm a
    JOIN islem i ON i.id = a.islem_id
    JOIN kart  k ON k.id = i.kart_id
    JOIN hesap h ON h.id = k.hesap_id
    WHERE a.id = p_alarm_id AND h.kullanici_id = p_kullanici_id AND a.durum = 'BEKLIYOR';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Bu alarm size ait değil veya zaten kapatılmış';
    END IF;

    PERFORM set_config('app.kullanici_id', p_kullanici_id::TEXT, TRUE);

    IF p_onay THEN
        UPDATE alarm SET musteri_teyit = TRUE, durum = 'TEMIZ',
               karar_veren_id = p_kullanici_id, karar_tarihi = NOW()
        WHERE id = p_alarm_id;
    ELSE
        UPDATE alarm SET musteri_teyit = FALSE, durum = 'FRAUD',
               karar_veren_id = p_kullanici_id, karar_tarihi = NOW()
        WHERE id = p_alarm_id;
        UPDATE kart  SET durum = 'BLOKE'     WHERE id = v_islem.kart_id;
        UPDATE islem SET durum = 'REDDEDILDI' WHERE id = v_islem.id;
        UPDATE hesap SET bakiye = bakiye + v_islem.tutar
        WHERE id = (SELECT hesap_id FROM kart WHERE id = v_islem.kart_id);
    END IF;
END;
$$;

-- 4.3 Bloke kartı yeniden aktif etme (yalnızca admin)
CREATE PROCEDURE sp_kart_aktif_et(p_kart_id INT, p_admin_id INT)
LANGUAGE plpgsql AS $$
DECLARE
    v_durum VARCHAR;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM kullanici k JOIN rol r ON r.id = k.rol_id
                   WHERE k.id = p_admin_id AND r.ad = 'ADMIN') THEN
        RAISE EXCEPTION 'Bu işlem için admin yetkisi gerekli';
    END IF;

    SELECT durum INTO v_durum FROM kart WHERE id = p_kart_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Kart % bulunamadı', p_kart_id;
    END IF;
    IF v_durum <> 'BLOKE' THEN
        RAISE EXCEPTION 'Kart % durumda, sadece BLOKE kart AKTIF yapılabilir', v_durum;
    END IF;

    PERFORM set_config('app.kullanici_id', p_admin_id::TEXT, TRUE);
    UPDATE kart SET durum = 'AKTIF' WHERE id = p_kart_id;
END;
$$;

-- =====================================================================
-- 5) RAPOR FONKSİYONLARI (sonuç tablosu döndürdükleri için FUNCTION)
-- =====================================================================

-- 5.1 Alarmın nedeni (analist ekranı)
CREATE FUNCTION fn_alarm_aciklama(p_alarm_id INT)
RETURNS TABLE(kural VARCHAR, puan INT, aciklama VARCHAR) AS $$
    SELECT k.ad, ki.puan, ki.aciklama
    FROM alarm a
    JOIN kural_ihlal ki ON ki.islem_id = a.islem_id
    JOIN kural k        ON k.id = ki.kural_id
    WHERE a.id = p_alarm_id
    ORDER BY ki.puan DESC;
$$ LANGUAGE sql;

-- 5.2 Şehir ve kategoriye göre alarm raporu
CREATE FUNCTION fn_rapor_sehir_kategori(p_bas DATE, p_bit DATE)
RETURNS TABLE(sehir VARCHAR, kategori VARCHAR, islem_sayisi BIGINT,
              alarm_sayisi BIGINT, fraud_sayisi BIGINT, alarm_orani NUMERIC) AS $$
    SELECT s.ad, c.ad,
           COUNT(i.id),
           COUNT(a.id),
           COUNT(a.id) FILTER (WHERE a.durum = 'FRAUD'),
           round(100.0 * COUNT(a.id) / NULLIF(COUNT(i.id), 0), 2)
    FROM islem i
    JOIN isyeri   y ON y.id = i.isyeri_id
    JOIN sehir    s ON s.id = y.sehir_id
    JOIN kategori c ON c.id = y.kategori_id
    LEFT JOIN alarm a ON a.islem_id = i.id
    WHERE i.tarih::DATE BETWEEN p_bas AND p_bit
    GROUP BY s.ad, c.ad
    ORDER BY COUNT(a.id) DESC, s.ad;
$$ LANGUAGE sql;

-- 5.3 Harcama grafiği: geçmiş aylar + bu ay (gerçek / beklenen / üst sınır)
CREATE FUNCTION fn_harcama_grafik(p_hesap_id INT)
RETURNS TABLE(ay DATE, gercek NUMERIC, beklenen NUMERIC, ust_sinir NUMERIC) AS $$
    SELECT ay, gercek, beklenen, ust_sinir
    FROM profil_gecmisi WHERE hesap_id = p_hesap_id
    UNION ALL
    SELECT a.ay, a.toplam, p.ortalama, fn_ust_sinir(p_hesap_id)
    FROM aylik_harcama a JOIN harcama_profili p ON p.hesap_id = a.hesap_id
    WHERE a.hesap_id = p_hesap_id AND a.ay = date_trunc('month', NOW())::DATE
    ORDER BY ay;
$$ LANGUAGE sql;

-- 5.4 Fraud halkası: aynı cihazı paylaşan hesapları zincirleme bul (WITH RECURSIVE)
CREATE FUNCTION fn_fraud_halkasi(p_hesap_id INT)
RETURNS TABLE(hesap_id INT, sahibi VARCHAR, derinlik INT) AS $$
    WITH RECURSIVE
    hesap_cihaz AS (                          -- hangi hesap hangi cihazı kullanmış
        SELECT DISTINCT k.hesap_id, i.cihaz_id
        FROM islem i JOIN kart k ON k.id = i.kart_id
        WHERE i.cihaz_id IS NOT NULL
    ),
    halka(hesap_id, derinlik, yol) AS (
        SELECT p_hesap_id, 0, ARRAY[p_hesap_id]
        UNION ALL
        SELECT hc2.hesap_id, h.derinlik + 1, h.yol || hc2.hesap_id
        FROM halka h
        JOIN hesap_cihaz hc1 ON hc1.hesap_id = h.hesap_id
        JOIN hesap_cihaz hc2 ON hc2.cihaz_id = hc1.cihaz_id
        WHERE hc2.hesap_id <> ALL(h.yol)       -- döngüye girme
    )
    SELECT h.hesap_id, u.ad_soyad::VARCHAR, MIN(h.derinlik)
    FROM halka h
    JOIN hesap hs    ON hs.id = h.hesap_id
    JOIN kullanici u ON u.id = hs.kullanici_id
    WHERE h.hesap_id <> p_hesap_id
    GROUP BY h.hesap_id, u.ad_soyad
    ORDER BY MIN(h.derinlik), h.hesap_id;
$$ LANGUAGE sql;

-- =====================================================================
-- 6) ÖRNEK VERİ
-- =====================================================================

INSERT INTO rol(ad) VALUES ('MUSTERI'), ('ANALIST'), ('ADMIN');

-- {noop} öneki: Spring Security'de düz şifre (demo için). Gerçekte BCrypt kullan.
INSERT INTO kullanici(ad_soyad, email, sifre_hash, rol_id) VALUES
 ('Sistem Yöneticisi', 'admin@demo.com',   '{noop}1234', 3),
 ('Fraud Analisti',    'analist@demo.com', '{noop}1234', 2),
 ('Ayşe Yılmaz',       'ayse@demo.com',    '{noop}1234', 1),
 ('Mehmet Kaya',       'mehmet@demo.com',  '{noop}1234', 1),
 ('Zeynep Demir',      'zeynep@demo.com',  '{noop}1234', 1),
 ('Can Öztürk',        'can@demo.com',     '{noop}1234', 1),
 ('Elif Şahin',        'elif@demo.com',    '{noop}1234', 1);

INSERT INTO sehir(ad, enlem, boylam) VALUES
 ('Ankara',   39.9334, 32.8597),
 ('İstanbul', 41.0082, 28.9784),
 ('İzmir',    38.4237, 27.1428),
 ('Antalya',  36.8969, 30.7133),
 ('Trabzon',  41.0027, 39.7168);

INSERT INTO kategori(ad) VALUES
 ('Market'), ('Elektronik'), ('Kuyumcu'), ('Restoran'), ('Akaryakıt'), ('Online');

INSERT INTO isyeri(ad, kategori_id, sehir_id) VALUES
 ('Kızılay Market',        1, 1),
 ('Ankara Teknoloji',      2, 1),
 ('Çankaya Döner',         4, 1),
 ('Eskişehir Yolu Petrol', 5, 1),
 ('Kadıköy Market',        1, 2),
 ('Kapalıçarşı Kuyumcu',   3, 2),
 ('Alsancak Elektronik',   2, 3),
 ('Konyaaltı Restoran',    4, 4),
 ('Trabzon Kuyumcu',       3, 5),
 ('Online Mağaza',         6, 2);

INSERT INTO hesap(kullanici_id, iban, bakiye) VALUES
 (3, 'TR000000000000000000000003', 50000),
 (4, 'TR000000000000000000000004', 30000),
 (5, 'TR000000000000000000000005', 20000),
 (6, 'TR000000000000000000000006', 15000),
 (7, 'TR000000000000000000000007', 25000);

INSERT INTO kart(hesap_id, son4) VALUES
 (1, '1111'), (2, '2222'), (3, '3333'), (4, '4444'), (5, '5555');

INSERT INTO cihaz(parmak_izi, tanim) VALUES
 ('dev-ayse-iphone',   'Ayşe - iPhone'),
 ('dev-mehmet-laptop', 'Mehmet - Laptop'),
 ('dev-zeynep-tel',    'Zeynep - Telefon'),
 ('dev-ortak-1',       'Şüpheli ortak cihaz 1'),
 ('dev-ortak-2',       'Şüpheli ortak cihaz 2');

INSERT INTO kural(kod, ad, puan, esik) VALUES
 ('GECE',             'Gece saatinde işlem',         20, NULL),
 ('YUKSEK_TUTAR',     'Ortalamanın üzerinde tutar',  40, 5),     -- 5 kat
 ('YENI_ISYERI',      'İlk kez kullanılan işyeri',   15, NULL),
 ('HIZ',              'Kısa sürede çok işlem',       30, 3),     -- 10 dk'da 3
 ('IMKANSIZ_SEYAHAT', 'İmkânsız seyahat',            60, 900);   -- km/saat

INSERT INTO ayar VALUES ('ALARM_ESIK', 60);

-- Geçmiş "normal" işlemler: risk trigger'ı kapalıyken yüklenir
ALTER TABLE islem DISABLE TRIGGER trg_islem_risk;
ALTER TABLE islem DISABLE TRIGGER trg_islem_profil;

INSERT INTO islem(kart_id, isyeri_id, cihaz_id, ip, tutar, tarih)
SELECT
    k.id,
    CASE k.id WHEN 1 THEN (ARRAY[1,2,3,4])[1 + g % 4]     -- Ayşe: Ankara
              WHEN 2 THEN (ARRAY[5,6,10])[1 + g % 3]      -- Mehmet: İstanbul
              WHEN 3 THEN (ARRAY[7,10])[1 + g % 2]        -- Zeynep: İzmir
              WHEN 4 THEN (ARRAY[8,10])[1 + g % 2]        -- Can: Antalya
              ELSE        (ARRAY[9,10])[1 + g % 2] END,   -- Elif: Trabzon
    CASE k.id WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 3 ELSE NULL END,
    ('10.0.' || k.id || '.' || (g % 200))::INET,
    -- harcamalar zamanla artıyor: 6 ay önce ~1x, bugün ~1.5x
    round(((50 + random() * 450) * (1 + (200 - g) / 400.0))::NUMERIC, 2),
    DATE_TRUNC('day', NOW()) - (g || ' days')::INTERVAL
        + ((9 + g % 12) || ' hours')::INTERVAL           -- hep gündüz
FROM kart k, generate_series(1,                                    -- son 6 tam ay + bu ay
        (CURRENT_DATE - (date_trunc('month', NOW()) - INTERVAL '6 months')::DATE)) g;

-- Fraud halkası için: Can, Elif ve Mehmet ortak cihazlar kullanmış
INSERT INTO islem(kart_id, isyeri_id, cihaz_id, ip, tutar, tarih) VALUES
 (4, 10, 4, '185.1.1.1', 120, NOW() - INTERVAL '20 days'),
 (5, 10, 4, '185.1.1.1',  90, NOW() - INTERVAL '19 days'),
 (5, 10, 5, '185.1.1.2', 200, NOW() - INTERVAL '18 days'),
 (2, 10, 5, '185.1.1.2', 150, NOW() - INTERVAL '17 days');

ALTER TABLE islem ENABLE TRIGGER trg_islem_risk;
ALTER TABLE islem ENABLE TRIGGER trg_islem_profil;

-- Müşteri tercihleri (Elif otomatik toleransı seçmiş)
INSERT INTO musteri_ayar(kullanici_id, tolerans_yuzde, otomatik_tolerans) VALUES
 (3, 20, FALSE), (4, 30, FALSE), (5, 20, FALSE), (6, 15, FALSE), (7, 20, TRUE);

-- Modelleri geçmiş veriden eğit
DO $$
DECLARE h INT;
BEGIN
    FOR h IN SELECT id FROM hesap LOOP
        CALL sp_profil_yeniden_egit(h);
    END LOOP;
END $$;
