#!/bin/zsh
# Установить debug-APK на эмулятор и пройти создание питомца BUNNY + CORAL с именем Finni.
# Использование: scripts/dev/walk.sh [папка для скриншота]
ADB=~/Library/Android/sdk/platform-tools/adb
OUT="${1:-/tmp}"
cd "$(dirname "$0")/../.."
$ADB install -r app/build/outputs/apk/debug/app-debug.apk | tail -1
$ADB shell am force-stop ru.larpinovplay.finniapp
$ADB shell am start -n ru.larpinovplay.finniapp/.presentation.MainActivity >/dev/null; sleep 4
$ADB shell input tap 540 1435; sleep 1      # BUNNY
$ADB shell input tap 541 1119; sleep 1      # CORAL
$ADB shell input tap 540 1197; sleep 0.7; $ADB shell input text Finni; sleep 0.5
$ADB shell input tap 541 1366; sleep 4      # Создать
$ADB exec-out screencap -p > "$OUT/pet-idle.png"
$ADB logcat -d | grep -E 'FATAL|AndroidRuntime: .*Exception' | tail -5
echo walk-done
