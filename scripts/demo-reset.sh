#!/usr/bin/env bash
# db/fraud_db.sql'i frauddb veritabanina yeniden yukler (sema DROP/CREATE ile
# sifirdan kurulur, seed veri ve demo senaryo oluyla gelir). Test amacli
# elle UPDATE/DELETE yapmak yerine her zaman bu scripti calistirin.
#
# Kullanim: scripts/demo-reset.sh
# Not: Uygulama calisiyorsa once durdurun (acik baglantilar DROP SCHEMA'yi kilitleyebilir).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJE_KOKU="$(dirname "$SCRIPT_DIR")"

DB_ADI="${DB_ADI:-frauddb}"
DB_KULLANICI="${DB_KULLANICI:-$(whoami)}"
SQL_DOSYASI="$PROJE_KOKU/db/fraud_db.sql"

if [ ! -f "$SQL_DOSYASI" ]; then
    echo "HATA: $SQL_DOSYASI bulunamadi." >&2
    exit 1
fi

echo "==> $DB_ADI sifirlaniyor (sema fraud DROP + CREATE)..."
psql -U "$DB_KULLANICI" -d "$DB_ADI" -v ON_ERROR_STOP=1 -q -f "$SQL_DOSYASI"
echo "==> Tamamlandi: $DB_ADI pristine demo veriyle hazir."
