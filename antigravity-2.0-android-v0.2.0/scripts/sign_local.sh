#!/usr/bin/env bash
# Подписывает неподписанный APK из CI вашим локальным ключом.
# Использование: ./scripts/sign_local.sh app-release-unsigned.apk antigravity.jks
# Ключ никогда не покидает ваш компьютер.
set -euo pipefail
APK="$1"; KS="$2"
APKSIGNER=$(ls -d "${ANDROID_HOME:?задайте ANDROID_HOME}"/build-tools/*/apksigner | sort -V | tail -n1)
OUT="${APK%-unsigned.apk}-signed.apk"
read -r -s -p "Пароль keystore: " PASS; echo
"$APKSIGNER" sign --ks "$KS" --ks-key-alias antigravity \
  --ks-pass pass:"$PASS" --key-pass pass:"$PASS" --out "$OUT" "$APK"
"$APKSIGNER" verify --print-certs "$OUT"
echo "Готово: $OUT"
