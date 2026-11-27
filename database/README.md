# otw.in — Firebase Database

Folder ini berisi konfigurasi yang perlu dipasang di Firebase. Aplikasi Android memakai
**Firebase Authentication** (Email/Password + Google) dan **Firebase Realtime Database**.
**Tidak ada data dummy di dalam aplikasi** — seluruh armada, user, dan pesanan berasal
dari Firebase.

Satu akun bisa dipakai login dengan tiga cara: **email + password**, **username + password**,
**nomor HP + password**, atau **Gmail (Google Sign-In)**. Firebase Auth hanya menerima email
sebagai identifier, jadi username/nomor HP disimpan sebagai alias yang dipetakan ke uid.

## Isi folder

| File | Fungsi |
| --- | --- |
| `database.rules.json` | Security rules Realtime Database. Salin ke tab **Rules** lalu **Publish**. |
| `google-services.example.json` | Contoh format konfigurasi Android. Salin ke `app/google-services.json` setelah download file asli dari Firebase. |

Tidak ada file contoh berisi data palsu. Data diisi oleh admin lewat panel admin di aplikasi
atau langsung di Firebase Console.

## Langkah setup

1. Buat project di [Firebase Console](https://console.firebase.google.com/).
2. Tambah Android app dengan package name **`com.example.otwin`**.
3. Download `google-services.json`, lalu taruh di **`app/google-services.json`**.
4. Di Firebase: **Authentication → Sign-in Method → aktifkan Email/Password DAN Google**.
   Setelah provider Google aktif, download ulang `google-services.json` ke `app/`.
   Web client ID dan URL database dibaca otomatis dari file itu oleh plugin Google Services.
5. Buat **Realtime Database** (lokasi `asia-southeast1`), lalu salin isi
   `database.rules.json` ke tab **Rules** dan **Publish**.
6. Tambahkan fingerprint di Firebase → Project Settings → Your apps → **SHA certificate fingerprints**.
   Untuk debug, jalankan:
   ```bash
   keytool -keystore "app/debug.keystore" -list -v -storepass android
   ```
   SHA-1 debug mesin ini:
   ```
   86:78:53:E9:A1:A8:B0:72:1B:43:85:86:84:66:2C:AF:1E:02:24:C2
   ```
   (SHA-1 lama `C4:81:...` bukan dipakai project ini.)
7. Sync Gradle dan jalankan aplikasi.

## Menentukan admin

Panel admin hanya muncul untuk uid yang terdaftar di node `admins`:

1. Di aplikasi, daftar/login akun yang akan dijadikan admin.
2. Di Firebase Console → Authentication → Users, salin **UID** akun tersebut.
3. Realtime Database → **Data** → tambahkan node:
   ```
   admins
     └── UID_TERSALIN: true
   ```
4. Tutup dan buka ulang aplikasi. Menu **Panel Admin** muncul di halaman Akun.

Node `admins` sengaja `"write": false` pada rules — tidak bisa diubah dari aplikasi agar
user biasa tidak bisa mengangkat dirinya sendiri menjadi admin.

## Mengisi data armada

Dua cara, keduanya menulis ke node yang sama:

- **Panel admin di aplikasi**: Akun → Panel Admin → Tambah Armada. Admin bisa mengisi field,
  menempel URL gambar, atau memilih foto dari perangkat. Foto dikompres lalu disimpan sebagai
  *data URI* di field `imageUrl`, jadi tidak perlu Firebase Storage.
- **Firebase Console**: Realtime Database → Data → `vehicles` → tambah child dengan
  `vehicleId` sebagai key, lalu isi field-field di bawah.

Karena aturan `vehicles` membolehkan pembacaan publik, user biasa langsung melihat armada
begitu admin menyimpannya. Field `active: false` menyembunyikan armada dari Beranda dan Cari.

## Struktur data

```
users/{uid}/
  name       : String
  username   : String (huruf kecil, 3-20 karakter)
  email      : String (email asli Firebase Auth)
  phone      : String (digit, diawali 62)
  createdAt  : Number (epoch millis)

admins/{uid}            : Boolean true -> user mendapat Panel Admin

loginEmails/{uid}       : String  -> email asli, dibaca sebelum login
usernames/{username}    : String  -> uid
phones/{phone}          : String  -> uid

vehicles/{vehicleId}/
  id            : String   (sama dengan key vehicleId)
  brand         : String   (contoh: "Merk Mobil")
  name          : String   (contoh: "Nama Model")
  category      : String   (mpv / city / ev / suv / sedan)
  seats         : String   (contoh: "7 Kursi")
  transmission  : String   (contoh: "Matic")
  feature       : String   (contoh: "Sunroof")
  pricePerDay   : Number   (contoh: 650000)
  rating        : String   (contoh: "4.9")
  reviews       : String   (contoh: "128")
  imageUrl      : String   (URL http(s) atau data URI hasil unggahan admin)
  badge         : String   (contoh: "Hybrid")
  badgeType     : String   (blue / green)
  active        : Boolean
  createdAt     : Number   (epoch millis)

orders/{uid}/{orderId}/
  id            : String   (sama dengan orderId)
  vehicleId     : String   (key di node vehicles)
  vehicleName   : String
  brand         : String
  imageUrl      : String   (salinan dari armada saat pesanan dibuat)
  startDate     : String   (contoh: "27 Nov 2026")
  endDate       : String
  durationDays  : Number
  mode          : String   ("Lepas Kunci" / "Dengan Sopir")
  total         : Number
  customerName  : String   (nama pelanggan saat order dibuat, untuk panel admin)
  customerUsername : String (username pelanggan, untuk panel admin)
  status        : String   ("Diproses" / "Selesai" / "Dibatalkan")
  createdAt     : Number   (epoch millis)
```

## Alur login

1. Identifier mengandung `@` → langsung `signInWithEmailAndPassword`.
2. Identifier berupa nomor HP → `phones/{phone}` → uid → `loginEmails/{uid}` → email → sign in.
3. Identifier berupa username → `usernames/{username}` → uid → `loginEmails/{uid}` → email → sign in.
4. Tombol Google → Google Sign-In → kredensial Firebase → profil tersimpan di `users/{uid}`.

## Keamanan rules

- `users/{uid}` hanya bisa dibaca/ditulis oleh pemiliknya (`auth.uid === $uid`).
- `admins/{uid}` hanya bisa dibaca oleh uid bersangkutan dan tidak bisa ditulis dari aplikasi.
- `vehicles` bisa dibaca publik, tapi hanya uid di `admins` yang boleh menulis/menghapus.
- `orders/{uid}` bisa dibaca oleh pemiliknya; admin boleh membaca semua order untuk dashboard.
- Pemilik order boleh membuat/membatalkan order; admin hanya boleh mengubah child `status`
  menjadi `Selesai` lewat panel admin (tidak bisa mengubah data order lain).
- `usernames` dan `phones` bisa dibaca publik (diperlukan resolusi login sebelum auth), tapi
  hanya bisa ditulis sekali oleh user yang login dan nilainya harus uid-nya sendiri.
- `loginEmails/{uid}` bisa dibaca publik agar alias bisa diterjemahkan ke email sebelum login.

⚠️ Setelah mengubah rules ini, **Publish** ulang di Firebase Console. Tanpa node `admins`
dan `vehicles`, panel admin dan tombol Sewa akan gagal saat runtime.
