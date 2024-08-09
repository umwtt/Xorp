# Java Sohbet Programı

Java Sohbet Programı, kullanıcıların birbirleriyle metin tabanlı sohbetler yapmasına olanak tanıyan basit bir anlık mesajlaşma uygulamasıdır. Bu proje, Java'da socket programlamayı öğrenmek ve uygulamak isteyenler için harika bir başlangıçtır.

## Özellikler

- **Kullanıcı Kayıt ve Giriş:** Kullanıcılar bir hesap oluşturabilir ve mevcut hesaplarıyla giriş yapabilirler.
- **Gerçek Zamanlı Sohbet:** Kullanıcılar arasında anlık mesajlaşma desteği.
- **Çok Kullanıcılı Sohbet:** Birden fazla kullanıcı aynı anda sohbet edebilir.
- **Kullanıcı Listesi:** Aktif kullanıcıların listesi görüntülenir.
- **Mesaj Geçmişi:** Sohbet penceresinde mesaj geçmişi saklanır.

## Kurulum

Bu projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları izleyin.

### Gereksinimler

- Java 8 veya üzeri
- Maven (bağımlılık yönetimi için)

### Adımlar

1. Bu repository'yi klonlayın:

    ```bash
    git clone https://github.com/umwtt/Xorp.git
    ```

2. Proje dizinine gidin:

    ```bash
    cd Xorp\"xorp 1.0.0v"\src
    ```

3. Maven kullanıyorsanız, bağımlılıkları yükleyin:

    ```bash
    mvn install
    ```

4. Uygulamayı derleyin ve çalıştırın:

    ```bash
    javac -d bin src/**/*.java
    java -cp bin com.example.Main
    ```

## Kullanım

1. **Sunucuyu Başlatın:**

   Sunucu tarafını çalıştırmak için aşağıdaki komutu kullanın:

    ```bash
    java -cp bin org.umwtt.Server
    ```

2. **İstemciyi Başlatın:**

   İstemciyi başlatmak için yeni bir terminalde aşağıdaki komutu çalıştırın:

    ```bash
    java -cp bin org.umwtt.Client
    ```

3. **Giriş Yapın veya Kayıt Olun:**

   Giriş ekranında yeni bir kullanıcı kaydı yapabilir veya mevcut hesabınızla giriş yapabilirsiniz.

4. **Sohbete Katılın:**

   Giriş yaptıktan sonra, mevcut kullanıcılarla anlık olarak sohbet etmeye başlayabilirsiniz.

## Proje Yapısı

```plaintext
Xorp v1.0.0/
│
├── src/
│   ├── com.example/
│   │   ├── Server.java
│   │   ├── Client.java
│   │   ├── chatLog.log
│   └── └── StatusLog.log
│
├── bin/ (Derlenmiş sınıf dosyaları)
│
├── README.md (Bu dosya)
│
└── pom.xml (Maven yapılandırma dosyası, opsiyonel)
```

