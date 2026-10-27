#!/usr/bin/env bash
# Создаёт постоянный ключ подписи для Antigravity и готовит base64 для GitHub Secrets.
# Запускать ОДИН раз. Потеряете файл или пароль — обновлять приложение будет нельзя.
set -euo pipefail

ALIAS="${AG_KEY_ALIAS:-antigravity}"
OUT="${1:-antigravity.jks}"

if [ -e "$OUT" ]; then
  echo "Файл $OUT уже существует. Не перезаписываю." >&2
  exit 1
fi

read -r -s -p "Пароль keystore (минимум 6 символов): " STORE_PASS; echo
read -r -s -p "Пароль ключа (можно тот же): " KEY_PASS; echo

keytool -genkeypair -v \
  -keystore "$OUT" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
  -dname "CN=Antigravity, O=NanoDev, C=RU"

base64 -w0 "$OUT" > "$OUT.b64"

cat << MSG

Готово.
  Keystore:       $OUT            (храните в надёжном месте, НЕ в git)
  Base64 для CI:  $OUT.b64         (значение секрета AG_KEYSTORE_B64)

Добавьте в GitHub: Settings → Secrets and variables → Actions:
  AG_KEYSTORE_B64        = содержимое $OUT.b64
  AG_KEYSTORE_PASSWORD   = пароль keystore
  AG_KEY_ALIAS           = $ALIAS
  AG_KEY_PASSWORD        = пароль ключа

Потом удалите $OUT.b64 с диска, если он больше не нужен.
MSG
