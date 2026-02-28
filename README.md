# Spring Boot Boilerplate

Production-ready Spring Boot başlangıç şablonu. JWT kimlik doğrulama, Role-Permission yetkilendirme, PostgreSQL, Flyway migration ve Docker altyapısı hazır olarak gelir.

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Güvenlik | Spring Security + JWT (jjwt 0.12.6) |
| Veritabanı | PostgreSQL 16 |
| Migration | Flyway |
| ORM | Spring Data JPA (Hibernate) |
| Dokümantasyon | SpringDoc OpenAPI (Swagger UI) |
| Yardımcı | Lombok |
| Konteyner | Docker + Docker Compose |

---

## Hızlı Başlangıç

### Gereksinimler

- Docker Desktop

### 1. Ortam dosyasını oluştur

```bash
cp .env.example .env
```

`.env` dosyasını aç ve değerleri doldur. En az `JWT_SECRET` değerini değiştir:

```bash
# Güvenli secret üretmek için:
openssl rand -base64 32
```

### 2. Uygulamayı başlat

```bash
docker-compose up --build
```

Arka planda çalıştırmak için:

```bash
docker-compose up -d --build
```

### 3. Hazır

| Servis | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:5050 |

> **Not:** Flyway migration'ları uygulama ayağa kalkarken otomatik çalışır.  
> Roller ve permission'lar seed migration'ı ile DB'ye otomatik eklenir.

---

## Auth Endpointleri

Tüm endpointler Swagger UI üzerinden interaktif olarak test edilebilir.

### Kayıt

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "123456"
}
```

### Giriş

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "123456"
}
```

**Yanıt:**

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci..."
}
```

### Token Yenileme

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGci..."
}
```

### Korumalı Endpoint (örnek)

```http
GET /api/demo
Authorization: Bearer eyJhbGci...
```

---

## Güvenlik Mimarisi

### JWT — Stateless Akış

Her istekte veritabanına gidilmez. Token içine `authorities` claim'i gömülür ve filtre yalnızca imzayı doğrular.

```
İstek gelir
  └─ JwtAuthenticationFilter
       ├─ İmza + expiry doğrula          (DB yok)
       ├─ authorities claim'den çek      (DB yok)
       └─ SecurityContext'e set et
```

**Token içeriği:**

```json
{
  "sub": "user@example.com",
  "authorities": ["ROLE_USER", "users:read"],
  "iat": 1700000000,
  "exp": 1700900000
}
```

| Token | Süre |
|---|---|
| Access Token | 15 dakika (900 000 ms) |
| Refresh Token | 7 gün (604 800 000 ms) |

### Role-Permission Modeli

```
users  ──< user_roles >── roles ──< role_permissions >── permissions
```

**Seed edilen varsayılan roller ve yetkiler:**

| Permission | USER | ADMIN |
|---|:---:|:---:|
| `users:read` | ✅ | ✅ |
| `users:write` | — | ✅ |
| `users:delete` | — | ✅ |
| `admin:access` | — | ✅ |

Yeni permission eklemek için `V2__seed_roles_and_permissions.sql` dosyasına `INSERT` satırı ekle veya yeni bir migration yaz.

Endpoint'e permission kontrolü eklemek için `@PreAuthorize` kullan:

```java
@PreAuthorize("hasAuthority('users:write')")
@PostMapping("/users")
public ResponseEntity<?> createUser(...) { ... }

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public ResponseEntity<?> deleteUser(...) { ... }
```

### Hata Yanıt Formatı

Tüm hatalar standart bir JSON formatında döner:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Email already in use: user@example.com",
  "path": "/api/auth/register",
  "timestamp": "2026-02-28T10:00:00Z"
}
```

| Durum | HTTP Kodu |
|---|---|
| Validation hatası | 400 |
| Geçersiz kimlik bilgisi | 401 |
| Geçersiz / süresi dolmuş token | 401 |
| Yetkisiz erişim | 403 |
| Email zaten kayıtlı | 409 |
| Beklenmedik hata | 500 |

---

## Proje Yapısı

```
src/main/java/com/buyukozkan/boilerplate/
├── config/
│   ├── OpenApiConfig.java          # Swagger/OpenAPI + BearerAuth tanımı
│   └── SecurityConfig.java         # Stateless security, JWT filter, whitelist
├── controller/
│   ├── AuthController.java         # /api/auth/register, /login, /refresh
│   └── DemoController.java         # Korumalı endpoint örneği
├── dto/
│   ├── AuthResponse.java           # accessToken + refreshToken yanıtı
│   ├── ErrorResponse.java          # Standart hata yanıtı
│   ├── LoginRequest.java
│   ├── RefreshTokenRequest.java
│   └── RegisterRequest.java
├── entity/
│   ├── Permission.java             # permissions tablosu
│   ├── Role.java                   # roles tablosu (permissions ile ManyToMany)
│   └── User.java                   # users tablosu (roles ile ManyToMany)
├── exception/
│   ├── DuplicateEmailException.java
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice — merkezi hata yönetimi
│   └── InvalidTokenException.java
├── repository/
│   ├── PermissionRepository.java
│   ├── RoleRepository.java         # findByNameWithPermissions (JOIN FETCH)
│   └── UserRepository.java         # findByEmailWithRolesAndPermissions (JOIN FETCH)
├── security/
│   └── JwtAuthenticationFilter.java  # DB'ye gitmeden token doğrulama
└── service/
    ├── AuthService.java            # register / login / refreshToken
    ├── JwtService.java             # token üretimi, doğrulama, claim okuma
    └── UserDetailsServiceImpl.java # Spring Security UserDetailsService impl

src/main/resources/
├── db/migration/
│   ├── V1__init_schema.sql         # Tüm tablolar
│   └── V2__seed_roles_and_permissions.sql  # Başlangıç rol/permission verileri
└── application.yml
```

---

## Veritabanı Yönetimi (pgAdmin)

1. http://localhost:5050 adresini aç
2. Giriş: `.env` dosyasındaki `PGADMIN_DEFAULT_EMAIL` ve `PGADMIN_DEFAULT_PASSWORD`
3. Yeni sunucu ekle:
   - **Host:** `database`
   - **Port:** `5432`
   - **Database:** `.env` → `POSTGRES_DB`
   - **Username:** `.env` → `POSTGRES_USER`
   - **Password:** `.env` → `POSTGRES_PASSWORD`

### CLI ile bağlan

```bash
docker-compose exec database psql -U postgres -d boilerplate
```

```sql
-- Tabloları listele
\dt

-- Rolleri gör
SELECT * FROM roles;

-- Permission'ları gör
SELECT r.name AS role, p.name AS permission
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
ORDER BY r.name, p.name;
```

---

## Ortam Değişkenleri

| Değişken | Açıklama |
|---|---|
| `POSTGRES_USER` | PostgreSQL kullanıcı adı |
| `POSTGRES_PASSWORD` | PostgreSQL şifresi |
| `POSTGRES_DB` | Veritabanı adı |
| `SPRING_DATASOURCE_URL` | JDBC bağlantı URL'i |
| `SPRING_DATASOURCE_USERNAME` | Uygulama DB kullanıcısı |
| `SPRING_DATASOURCE_PASSWORD` | Uygulama DB şifresi |
| `JWT_SECRET` | Base64 encoded, min 32 byte |
| `PGADMIN_DEFAULT_EMAIL` | pgAdmin giriş e-postası |
| `PGADMIN_DEFAULT_PASSWORD` | pgAdmin giriş şifresi |

---

## Komutlar

```bash
# Başlat
docker-compose up --build

# Arka planda başlat
docker-compose up -d --build

# Durdur (veriler korunur)
docker-compose down

# Durdur + verileri sil
docker-compose down -v

# Logları takip et
docker-compose logs -f app

# Yeniden build et (kod değişikliği sonrası)
docker-compose up --build app
```
