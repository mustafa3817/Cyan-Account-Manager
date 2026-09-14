# Cyan Acc Manager

<p align="center">
  <b>Modern, Hızlı ve Güvenli Minecraft Hesap Yöneticisi (Fabric 1.21.11)</b><br>
  <i>Powered by CyanAFK</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.11-00E5FF?style=for-the-badge&logo=minecraft" alt="Minecraft 1.21.11">
  <img src="https://img.shields.io/badge/Fabric-Loom-blue?style=for-the-badge" alt="Fabric">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License MIT">
</p>

---

## 🌟 Özellikler

- **Microsoft OAuth 2.0 Entegrasyonu:**
  - **Tarayıcı ile Otomatik Giriş (Localhost / IAS Modeli):** Tek tıkla tarayıcıda Microsoft onay sayfasını açar, onaylandığında otomatik olarak oyuna bağlanır.
  - **Doğrudan Kod Linki (microsoft.com/link?otc=...):** Cihaz onay kodunu otomatik panoya kopyalar ve doğrudan onay sayfasını açar.
- **Akıllı Oturum Yenileme (Silent Refresh):**
  - Kaydedilen hesapların efresh_token verisini güvenle saklar. Minecraft token süresi dolduğunda arka planda sessizce yeni token üretir, tekrar şifre istemez.
- **Doğrudan Access Token ile Giriş:**
  - Elinizdeki Minecraft Bearer Access Token ile saniyeler içinde hesaba bağlanın ve listeye kaydedin.
- **1.21.11 Kriptografik İmza ve Sohbet Senkronizasyonu:**
  - Session, UserApiService, ProfileKeys ve SocialInteractionsManager servislerini çalışma zamanında eksiksiz günceller; çökme ve sohbet imza hatalarını önler.
- **Tek Tıkla Orijinal Hesaba Dönüş:**
  - Ekrandaki **"Çık"** butonuna basarak anında Minecraft Launcher'ın orijinal hesabına geri dönebilirsiniz.
- **Çift Tıklama ile Hızlı Giriş:**
  - Hesap listesindeki herhangi bir hesaba çift tıklayarak anında oturum açabilirsiniz.
- **Özel Sıralama (▲ / ▼):**
  - Hesaplarınızı dilediğiniz sıraya göre yukarı ve aşağı taşıyabilirsiniz.
- **İçe / Dışa Aktarma (Import & Export):**
  - Hesaplarınızı JSON formatında tek tıkla panoya kopyalayabilir veya başka profillerden panodan içe aktarabilirsiniz. OpSec hesap dosyalarını otomatik tanır.
- **Çok Katmanlı Boş Port Güvenliği:**
  - Yerel sunucu başlatılırken port çakışmalarını sıfırlamak için 3 kademeli dinamik port taraması yapar.

---

## 🚀 Kurulum

1. [Releases](https://github.com/mustafa3817/Cyan-Account-Manager/releases) sayfasından en son .jar dosyasını indirin.
2. İndirdiğiniz .jar dosyasını Minecraft profilinizin .minecraft/mods klasörüne atın.
3. **Fabric Loader** (>=0.15.0) ve **Fabric API** kurulu olduğundan emin olun.
4. Oyunu başlatın; Ana Menü'de veya Çok Oyunculu Sunucu Listesi'nde sağ üstteki **"Hesaplar"** butonuna tıklayın.

---

## 🛠️ Kaynak Koddan Derleme

Projeyi yerel ortamınızda kendiniz derlemek isterseniz:

`ash
# Projeyi klonlayın
git clone https://github.com/mustafa3817/Cyan-Account-Manager.git

# Proje dizinine girin
cd Cyan-Account-Manager

# Modu derleyin
./gradlew build
`

Derlenen mod dosyası uild/libs/ klasörü altında oluşturulacaktır.

---

## 💬 Topluluk & Destek

- **Discord:** [CyanAFK Discord Sunucusu](https://discord.com/invite/YzujvvZQjE)
- **Web:** [cyanafk.com](https://cyanafk.com)

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) altında korunmaktadır.
