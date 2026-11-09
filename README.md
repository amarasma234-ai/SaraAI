# Sara AI - Phase 1

## Option A: GitHub Actions (sabse aasaan)
1. github.com par free repo banao, is folder ki saari files upload karo.
2. Actions tab -> "Build APK" -> Run workflow.
3. Khatam hone par Artifacts se SaraAI-debug-apk download karo, phone mein install karo.

## Option B: Termux (aarch64 par aksar issue aata hai)
pkg update && pkg install openjdk-17 gradle aapt2 wget unzip -y
# Android SDK (platform 34) ka folder banao aur local.properties mein sdk.dir=... likho
# gradle.properties mein ye line add karo:
# android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
# cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/

## Commands
youtube kholo | whatsapp kholo | youtube pe <gaana> chalao | torch on/off
volume badhao/kam karo/50 | brightness 80 | battery kitni hai | storage | wifi | device
