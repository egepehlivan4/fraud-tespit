# Demo Senaryosu (~5 dakika)

Hocaya göstermeden önce veriyi sıfırlayın:

```bash
scripts/demo-reset.sh
mvn spring-boot:run
```

Tarayıcıda `http://localhost:8080/login`. Üç farklı tarayıcı profili (ya da
gizli pencere) açarsanız aynı anda admin/analist/müşteri olarak giriş
yapabilirsiniz. Tüm şifreler **1234**.

| Kullanıcı | Rol |
|---|---|
| ayse@demo.com | MUSTERI (kart •••• 1111) |
| can@demo.com | MUSTERI (kart •••• 4444 — fraud halkasında) |
| analist@demo.com | ANALIST |
| admin@demo.com | ADMIN |

---

## 1) Müşteri — normal işlem (Ayşe)

- Giriş: **ayse@demo.com**
- **Harcama Yap** → Kart •••• 1111, herhangi bir işyeri, tutar **35**, "Harcama Yap"
- Beklenen: **"İşlem onaylandı."** — risk puanı gösterilmez.

## 2) Admin — İmkânsız Seyahat simülasyonu (Ayşe'nin kartına)

- Giriş: **admin@demo.com**
- **İşlem Simülatörü** → "İmkânsız Seyahat" kutusunda kart olarak **Ayşe Yılmaz •••• 1111**'i seç → "İmkânsız Seyahat"
- Beklenen: *"İşlem eklendi. Risk puanı: 75. Tetiklenen kurallar: İmkânsız seyahat (+60), İlk kez kullanılan işyeri (+15). Alarm açıldı."*

## 3) Müşteri — bildirim + "Ben yapmadım" (Ayşe)

- Ayşe'nin sekmesine dön, sayfayı yenile
- Ana sayfada **Bildirimlerim (1)** rozetini göster
- **Bekleyen Onay** → işlemi gör → **"Ben yapmadım"** → onayı geçir
- Beklenen: *"Kartınız bloke edildi, tutar hesabınıza iade edildi."*

## 4) Bloke kartla işlem reddi (Ayşe)

- **Harcama Yap** → aynı kartla (•••• 1111) tekrar dene
- Beklenen: **"Kart BLOKE durumda, işlem reddedildi"** — trigger'dan gelen anlaşılır hata.
- **Kartlarım**'da kartın **BLOKE** göründüğünü de gösterebilirsiniz.

## 5) Admin — ikinci bir alarm (Can'ın kartına)

- Admin sekmesinde **İşlem Simülatörü** → "İmkânsız Seyahat" kutusunda kart olarak **Can Öztürk •••• 4444**'ü seç → "İmkânsız Seyahat"
- Beklenen: Alarm açıldı mesajı.

## 6) Analist — alarmı inceler ve FRAUD der

- Giriş: **analist@demo.com**
- **Alarmlar** → BEKLIYOR filtreli listede Can Öztürk'ü bul → satıra tıkla
- Detay sayfasında göster: **Kural Kırılımı** (İmkânsız seyahat, Yeni işyeri), **Müşterinin Son İşlemleri**, ve **Uyarı: cihaz paylaşan bağlı hesaplar** kutusu (Elif Şahin, Mehmet Kaya)
- **"FRAUD — kartı blokla"** butonuna bas
- Beklenen: *"Karar kaydedildi: FRAUD"*, Can'ın kartı artık BLOKE, parası iade edildi.

## 7) Admin — kural eşiğini değiştirir

- Admin sekmesinde **Kural Yönetimi** → YUKSEK_TUTAR satırında "Düzenle"
- Eşiği (örn. 5 → 6) değiştir, Kaydet
- Beklenen: *"Kural güncellendi."*

## 8) Denetim Log

- **Denetim Log** → tablo filtresinden **kural** seç
- Beklenen: En üstte, az önceki değişikliğin eski/yeni JSON'ı ve "Sistem Yöneticisi" kullanıcı adıyla göründüğünü göster.

## 9) Rapor

- Analist (veya admin) sekmesinde **Raporlar**
- Şehir/kategori tablosunu ve altındaki bar grafiği göster.

## 10) Fraud Halkası

- **Fraud Halkası Sorgulama** → Hesap Id olarak **4** (Can) gir → Sorgula
- Beklenen: Elif Şahin (derinlik 1), Mehmet Kaya (derinlik 2) — aynı cihazı paylaşan zincir.

## 11) Harcama Grafiği (Ayşe)

- Ayşe sekmesine dön → **Harcama Profilim**
- Beklenen: 3 çizgili grafik (gerçek/beklenen/üst sınır); içinde bulunulan ay kesikli çizgi ve
  "(devam ediyor)" etiketiyle ayrı gösteriliyor. Altındaki kutuda EWMA formülünün sade açıklaması.

---

Demo bitince (tekrar sunmadan önce) veriyi tekrar sıfırlayın: `scripts/demo-reset.sh`.
