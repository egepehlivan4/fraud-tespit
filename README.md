# Fraud Tespit Sistemi

BMT-311 Veri Tabanı Yönetim Sistemleri dönem projesi. Kart işlemlerinde
fraud tespiti yapan ve müşterinin harcama alışkanlığını online öğrenen
bir sistem. İş mantığı (trigger/procedure/fonksiyon) PostgreSQL'de;
Spring Boot + Thymeleaf arayüzü bunları çağırır, mantığı tekrar yazmaz.

**Stack:** Java 21, Spring Boot 3, Thymeleaf, Spring Security, JdbcTemplate
(JPA yok), PostgreSQL.

## Kurulum

```bash
# PostgreSQL (zaten kuruluysa atlayın)
brew install postgresql@16
brew services start postgresql@16

# Veritabanı
createdb frauddb

# Şemayı + seed veriyi yükle (trigger/procedure/fonksiyonlar dahil)
scripts/demo-reset.sh

# Uygulamayı çalıştır
mvn spring-boot:run
```

Tarayıcıda `http://localhost:8080/login`.

Varsayılan bağlantı `localhost:5432/frauddb`, kullanıcı olarak işletim
sistemi kullanıcınızı (`whoami`) kullanır (yerel trust/peer auth). Farklı
bir kullanıcı/şifre gerekiyorsa ortam değişkenleriyle geçersiz kılın:

```bash
DB_USERNAME=postgres DB_PASSWORD=sifre mvn spring-boot:run
```

## Veriyi sıfırlama

Test ederken veri değişir (bakiye, alarm, kart durumu vb). Elle
UPDATE/DELETE yapmak yerine **her zaman** şu scripti çalıştırın:

```bash
scripts/demo-reset.sh
```

Bu script `db/fraud_db.sql`'i `frauddb`'ye yeniden yükler (şema
DROP/CREATE ile sıfırdan kurulur). Çalıştırmadan önce uygulamayı
durdurun (açık bağlantılar şemayı kilitleyebilir).

## Demo kullanıcıları

Tüm şifreler **1234**.

| Email | Rol | Not |
|---|---|---|
| admin@demo.com | ADMIN | Kullanıcı/işyeri/kart/kural yönetimi, denetim log, model yönetimi, işlem simülatörü; analist ekranlarına da erişir |
| analist@demo.com | ANALIST | Alarmları inceler, FRAUD/TEMIZ kararı verir, rapor görür |
| ayse@demo.com | MUSTERI | Kart •••• 1111 |
| mehmet@demo.com | MUSTERI | Kart •••• 2222 |
| zeynep@demo.com | MUSTERI | Kart •••• 3333 — aylık harcaması artan, grafik için iyi örnek |
| can@demo.com | MUSTERI | Kart •••• 4444 — Elif/Mehmet ile cihaz paylaşan fraud halkasında |
| elif@demo.com | MUSTERI | Kart •••• 5555 — fraud halkasında |

Tam demo akışı için: [`docs/DEMO.md`](docs/DEMO.md).

## Veritabanı şeması

`db/fraud_db.sql` — tablolar, trigger'lar, stored procedure'ler, rapor
fonksiyonları ve örnek veri tek dosyada. Şemayı değiştirmeden önce proje
sahibine danışın (bkz. `CLAUDE.md`).

## Testler

```bash
mvn test
```

Testler gerçek `frauddb` veritabanına karşı çalışır, `@Transactional`
olduğu için her test sonunda otomatik rollback olur — demo verisini
etkilemez.
