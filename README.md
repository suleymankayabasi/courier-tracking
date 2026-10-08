# Courier Tracking

Kuryelerden akış halinde gelen konumları alan, kuryelerin Migros mağazalarına girişlerini (100 m yarıçap) loglayan ve
her kuryenin kat ettiği toplam mesafeyi raporlayan Spring Boot REST servisi.

## Çalıştırma

### Docker ile (önerilen)

Yalnızca Docker gerekir; Java veya Maven kurulu olması gerekmez.

```bash
docker compose up --build       # imajı derler ve http://localhost:8080 üzerinde başlatır
./demo.sh                       # ikinci bir terminalde: örnek bir kuryenin konumlarını gönderir ve sonuçları yazdırır
docker compose down             # durdurur
```

İlk derleme bağımlılıkları indirdiği için birkaç dakika sürebilir. 8080 portu doluysa başka bir port seçin:

```bash
APP_PORT=8090 docker compose up --build
./demo.sh http://localhost:8090
```

### Java ile

Java 21 veya üstü gerekir; Maven wrapper projeye dahildir.

```bash
./mvnw spring-boot:run          # http://localhost:8080 üzerinde başlar
./mvnw test                     # unit ve entegrasyon testleri
```

8080 portu doluysa: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8090`.

Swagger UI: http://localhost:8080/swagger-ui.html. Mağaza girişleri uygulama loguna yazılır
(`Courier ... entered ... at ...`); Docker'da `docker compose logs -f` ile izlenebilir.

## Case gereksinimleri

| Gereksinim | Karşılığı |
| --- | --- |
| Akış halinde konum alan REST uygulaması | `POST /api/couriers/locations` |
| Mağazanın 100 m çevresine giren kurye ve mağazanın loglanması | `StoreEntranceDetector` kuralı, `StoreEntranceLogger` logu |
| Aynı mağazaya 1 dakika içindeki tekrar girişin sayılmaması | `StoreEntranceDetector`, ayarlanabilir eşik |
| Mağazaların `stores.json`'dan okunması | `JsonStoreReader`, `src/main/resources/stores.json` |
| Toplam mesafe sorgusu (`getTotalTravelDistance`) | `CourierTrackingService.getTotalTravelDistance`, `GET /api/couriers/{courierId}/total-distance` |
| En az 2 tasarım deseni | Strategy (`DistanceCalculator`), Observer (Spring event + `StoreEntranceLogger`) |
| Kolay çalıştırma ve test | Docker Compose, `demo.sh`, `./mvnw test` |

## Yapılandırma

İş kuralı değerleri `src/main/resources/application.yml` içindedir ve kod değişmeden değiştirilebilir:

| Ayar | Varsayılan | Anlamı |
| --- | --- | --- |
| `courier-tracking.store-entrance.radius-meters` | `100` | Mağaza yarıçapı (metre) |
| `courier-tracking.store-entrance.reentry-threshold` | `1m` | Aynı mağazaya tekrar girişin sayılması için gereken en kısa süre |
| `courier-tracking.location.allowed-clock-skew` | `1m` | Konum zamanının sunucu saatinden en fazla ne kadar ileride olabileceği |

Örneğin `./mvnw spring-boot:run -Dspring-boot.run.arguments=--courier-tracking.store-entrance.radius-meters=150`.
Geçersiz bir değerde (sıfır veya negatif yarıçap, negatif süre) uygulama başlamaz.

## API

| Metot | Yol | Sonuç |
| --- | --- | --- |
| `POST` | `/api/couriers/locations` | `204`, geçersiz konumda `400` |
| `GET` | `/api/couriers/{courierId}/total-distance` | `{"courierId":"c1","totalDistanceMeters":909.96}`, bilinmeyen kuryede `404` |
| `GET` | `/api/couriers/{courierId}/store-entrances` | `[{"storeName":"Ataşehir MMM Migros","time":"..."}]`, bilinmeyen kuryede `404` |

```bash
curl -X POST http://localhost:8080/api/couriers/locations \
  -H 'Content-Type: application/json' \
  -d '{"time":"2026-01-01T10:00:00Z","courierId":"courier-1","lat":40.9923307,"lng":29.1244229}'
```

| Alan | Kural |
| --- | --- |
| `time` | zorunlu, saat dilimi içeren ISO-8601 metin (`2026-01-01T10:00:00Z` veya `2026-01-01T13:00:00+03:00`), sunucu saatinden en fazla 1 dakika ileride; sayısal epoch değerleri birimi belirsiz olduğu için reddedilir |
| `courierId` | zorunlu, 1-64 karakter; harf, rakam, `.`, `_`, `-` |
| `lat` / `lng` | zorunlu, -90..90 / -180..180 |

Tüm hatalar (Spring'in ürettiği `404`, `405`, `415` ve beklenmeyen `500` dahil) RFC 9457 problem formatında döner;
doğrulama hatalarında geçersiz alanlar `errors` altında listelenir. `500` yanıtı iç ayrıntıyı göstermez, hata loglanır.

## Tasarım

```
CourierController ──> CourierTrackingService ──> CourierRepository ◄── InMemoryCourierRepository
                              │
                              ├─ StoreEntranceDetector ──> StoreRepository ◄── InMemoryStoreRepository
                              │                                    (StoreConfig, stores.json'u JsonStoreReader ile okuyup doldurur)
                              ├─ DistanceCalculator ◄── HaversineDistanceCalculator      (Strategy)
                              └─ StoreEntrance yayınlar ──> StoreEntranceLogger          (Observer)
```

`CourierTrackingService.recordLocation` her konum için sırasıyla:

1. çok ileri tarihli zamanı reddeder,
2. kuryenin kabul edilen son konumundan eski olan konumu yok sayar,
3. girilen mağazaları `StoreEntranceDetector`'a sorar: kurye 100 m yarıçapın dışından içine geçtiyse ve bu kuryenin
   aynı mağazaya son girişi en az 1 dakika önceyse giriş sayılır,
4. önceki konumdan olan mesafeyi toplama ekler.

Servis yalnızca akışı yönetir; giriş kuralı `StoreEntranceDetector`'da, mesafe formülü `DistanceCalculator`'da,
saklama ve eşzamanlılık `CourierRepository` arayüzünün arkasındadır. Mağazalarda dosya formatı (`JsonStoreReader`),
liste kuralları ve saklama (`InMemoryStoreRepository`) ile hangi dosyanın okunacağı (`StoreConfig`) ayrı sınıflardadır;
örneğin YAML'a geçmek yalnızca yeni bir okuyucu gerektirir. Servis bu bağımlılıkların hepsini constructor'dan alır, bu yüzden
Spring olmadan unit test edilebilir.

### Paket yapısı

| Paket | İçerik |
| --- | --- |
| `controller` | REST endpoint'leri ve HTTP hata eşlemesi (`GlobalExceptionHandler`) |
| `dto` | İstek ve yanıt nesneleri |
| `json` | JSON'a özel dönüşümler (`IsoInstantDeserializer`, `stores.json` okuyucusu `JsonStoreReader`) |
| `service` | İş akışı (`CourierTrackingService`) ve giriş kuralı (`StoreEntranceDetector`) |
| `distance` | Mesafe hesaplama stratejisi |
| `model` | Domain nesneleri (`Courier`, `Store`, `GeoPoint`, `StoreEntrance`) |
| `repository` | Depolama arayüzleri ve in-memory gerçekleştirmeleri |
| `listener` | Giriş event'ini dinleyen observer |
| `exception` | Domain exception'ları |
| `config` | Bean tanımları (`Clock`, mağaza deposu) ve ayar sınıfları (`StoreEntranceProperties`, `LocationProperties`) |

### Tasarım desenleri

- **Strategy**: `DistanceCalculator` mesafenin nasıl ölçüldüğünü soyutlar. Servis bunu hem mağaza yarıçapı hem de kat
  edilen mesafe için kullanır; başka bir formül (örneğin Vincenty) servise dokunmadan eklenebilir.
- **Observer**: sayılan her giriş Spring application event olarak yayınlanır. `StoreEntranceLogger` bunu loglar
  ("kurye ve mağazayı logla" şartı). Bildirim veya metrik gibi başka tepkiler, takip mantığı değişmeden yeni
  listener olarak eklenebilir.

## Kararlar ve edge case'ler

Aşağıdaki 100 m ve 1 dakika değerleri varsayılanlardır (bkz. Yapılandırma).

**Mağaza girişleri**
- Mağazaya tam 100 m uzaklıktaki nokta içeride sayılır.
- Giriş, dışarıdan içeriye geçiştir. Mağazanın içinde bekleyen kurye her konumda değil, bir kez giriş yapmış sayılır.
  İlk konumu zaten yarıçap içinde olan kurye için giriş sayılır.
- "1 dakika içindeki tekrar girişler": aynı mağazaya son sayılan girişten 1 dakikadan kısa süre sonra yapılan tekrar
  giriş sayılmaz. Tam 1 dakika sonrası sayılır. Kural sunucu saatini değil, konumların zamanını kullanır.
- Mağazalar ve kuryeler birbirinden bağımsızdır: aynı kuryenin iki mağazaya ya da iki kuryenin aynı mağazaya bir dakika
  içinde girişlerinin hepsi sayılır.

**Zaman ve sıralama**
- İstemcinin gönderdiği zaman kullanılır. Kuryenin kabul edilen son konumundan eski bir konum yok sayılır (akışta geç
  gelen konumlar olağan olduğu için debug seviyesinde loglanır). Aksi halde mesafeye zikzak eklenir veya sahte giriş
  oluşur. Yok sayılan konum da `204` döner: geç gelen veri bir istemci hatası değildir ve tekrar gönderilmesi sonucu
  değiştirmez; bu yüzden istemciye ayrıca bildirilmez.
- Son konumla aynı zamana sahip konum işlenir. Birebir aynı tekrar eden konum 0 m ekler ve giriş oluşturmaz.
- 1 dakikadan fazla ileri tarihli zaman `400` ile reddedilir. Aksi halde o kuryenin sonraki tüm doğru konumları sıra
  dışı sayılırdı.

**Mesafe**
- Metre cinsinden büyük daire (Haversine) mesafesi, ardışık konumlar arasında toplanır. İlk konum 0 verir.
- Bilinmeyen kurye (henüz konumu yok) `0` yerine `404` döner; böylece yanlış yazılmış bir id, hiç hareket etmemiş bir
  kuryeyle karıştırılmaz.

**Eşzamanlılık**
- Aynı kuryenin konumları sırayla işlenir; böylece paralel iki istek bir girişi iki kez sayamaz veya bir mesafe
  güncellemesini kaybedemez. Bu, `CourierRepository.updateAtomically` sözleşmesidir: kurye yoksa oluşturulur ve
  güncelleme o kurye için tek başına çalışır. `InMemoryCourierRepository` bunu `ConcurrentHashMap.compute` ile sağlar;
  farklı kuryeler paralel işlenir. Yeni bir kurye ancak ilk konumu tamamen işlendikten sonra görünür olur; güncelleme
  hata verirse kurye hiç oluşmaz.
- Giriş event'leri kurye kilidi bırakıldıktan sonra yayınlanır; yavaş bir listener o kuryenin sonraki konumunu
  geciktirmez.

**Girdi**
- Eksik alan, aralık dışı koordinat, sayısal olmayan değer, geçersiz zaman ve bozuk JSON `400` döner. `courierId` log
  satırlarında yer aldığı için güvenli karakterlerle sınırlandırılmıştır.
- `stores.json` yoksa, bozuksa, boşsa, ismi ya da koordinatı eksik veya geçersiz bir mağaza içeriyorsa ya da iki mağaza
  aynı isme sahipse uygulama başlamaz. (Giriş zamanları mağaza ismine göre tutulduğu için isimler benzersiz olmalıdır.)

## Kısıtlar

- Veriler bellekte tutulur ve yeniden başlatmada kaybolur. Veritabanına geçişte servis değişmez; yeni bir
  `CourierRepository` gerçekleştirmesi `updateAtomically`'yi bir transaction ve satır kilidiyle (`SELECT ... FOR UPDATE`)
  sağlar.
- Girişler yalnızca gönderilen noktalarda tespit edilir. Kurye seyrek iki konum arasında yarıçaptan geçerse giriş
  kaçırılır.
- GPS gürültüsü filtrelenmez: dururken oluşan küçük sıçramalar mesafeye eklenir.
- Bellek sınırsız büyür: kuryeler ve giriş geçmişleri hiç silinmez. Uzun süre çalışan bir sistemde saklama süresi
  (örneğin belirli bir süre konum göndermeyen kuryelerin temizlenmesi) gerekir.
