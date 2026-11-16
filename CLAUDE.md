# Fraud Tespit Sistemi — BMT-311 dönem projesi

## Hocanın zorunlu isterleri
- PostgreSQL + Java (başka dil yasak)
- En az 3 nesne için tam CRUD (ekle/listele/güncelle/sil) arayüzde
- Stored procedure, trigger, normalizasyon zorunlu
- Kullanıcı giriş paneli, en az 2 kullanıcı tipi (MUSTERI, ANALIST, ADMIN)
- Sorgulara arayüzden erişilebilmeli

## Mimari kurallar
- Stack: Spring Boot 3, Java 21, Thymeleaf, Spring Security, JdbcTemplate (JPA yok)
- Veritabanı şeması db/fraud_db.sql'de; şemayı değiştirmeden önce bana sor
- İş mantığı veritabanında: Java tarafı procedure'leri CALL eder,
  trigger/procedure mantığını Java'da tekrar yazma
- Her işlemden önce SET app.kullanici_id ile aktif kullanıcıyı veritabanına ilet
- Silmede FK hatalarını kullanıcıya anlaşılır mesajla göster

## Çalışma şekli
- Ben öğrenciyim ve bu kodu hocaya savunacağım
- Her adımda yalnızca istenen kısmı yap, sonra ne yaptığını ve
  neden öyle yaptığını kısaca açıkla
- Basit ve okunabilir kod; gereksiz soyutlama yok