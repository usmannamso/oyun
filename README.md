# ComplexClash (Online 1v1, Java-free packages)

Bu depo, tek `scratch_8.java` dosyasından **Mac DMG**, **Windows app-image (zip, Java gömülü)** ve **Linux app-image** üretir.

## Kullanım
1. GitHub'da yeni bir repo oluştur, bu zip içeriğini yükle (veya burayı `main` dalına push et).
2. **Actions** sekmesine gir. "Build ComplexClash" workflow'u otomatik başlar (ya da **Run workflow**).
3. Workflow tamamlandığında, sayfanın altındaki **Artifacts** bölümünden:
   - `ComplexClash-mac-dmg` → `.dmg` (Java gerekmez)
   - `ComplexClash-windows-zip` → `.zip` (içindeki `ComplexClash/ComplexClash.exe` direkt çalışır, Java gerekmez)
   - `ComplexClash-linux` → `.tar.gz` (içindeki app-image, Java gerekmez)
4. Dosyayı arkadaşına gönder. Onlar **hiçbir şey kurmadan** açıp oynar.

> Not: Windows'ta istersen `--type exe` kullanarak installer da üretilebilir; workflow şu an portablesız sorun çıkarmasın diye `app-image` kullanıyor.
