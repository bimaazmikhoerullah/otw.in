# Session Progress Log

## Current State

**Last Updated:** 2026-11-27
**Session ID:** otw.in-admin-auth-fix
**Active Feature:** feat-032 (completed)

## Status

### What's Done

- [x] Created the complete otw.in drawable resource library (111 XML drawables) in `app/src/main/res/drawable/`
  - [x] 63 vector icons (24dp viewport) covering auth, navigation, car rental, payment, and general UI
  - [x] 10 button backgrounds with default / pressed / focused / disabled states and ripple effects
  - [x] 6 input field backgrounds (default, focused, error, disabled, success, rounded)
  - [x] 9 card and container backgrounds (including subtle layered shadow for elevated card)
  - [x] 6 checkbox / radio state resources with checked and unchecked drawables
  - [x] 7 status badges (availability, booking, payment, promotional)
  - [x] Brand assets: `ic_logo_otwin.xml` (tint-safe square mark used by existing layouts), `ic_logo_otwin_symbol.xml` (full-color badge), `ic_logo_otwin_wordmark.xml` (160x48 lockup), `bg_logo_container.xml`
- [x] Extended `app/src/main/res/values/colors.xml` with the remaining official palette colors, state colors, and ripple colors; all pre-existing color entries preserved
- [x] Replaced low-quality existing `ic_eye.xml`, `ic_eye_off.xml`, `ic_logo_otwin.xml`, `bg_button_primary.xml`, and `bg_button_google.xml` with palette-consistent implementations; all other existing drawables preserved
- [x] Validated all 127 project XML files with an XML parser: zero errors
- [x] Verified every `@color/` and `@drawable/` reference in the new resources resolves

### What's Done (follow-up: login/register warnings)

- [x] `Theme.Otwin` now inherits `Theme.Material3.Light.NoActionBar` with otw.in color attributes — fixes "Failed to instantiate one or more classes" for Material components in the layout preview
- [x] Removed `android:contentDescription` from all 7 `TextInputEditText` views (login: email/password; register: name/email/phone/password/confirm) — hint/label supplies the accessible name
- [x] Added `android:minHeight="48dp"` to all 7 inputs and 48dp minWidth/minHeight to `checkboxRemember`
- [x] Applied `bg_checkbox_selector` to the remember-me checkbox; checkbox and radio drawables now declare explicit 20dp sizes
- [x] Re-validated all 134 XML files (res + manifest): zero errors

### What's Done (follow-up: high-fidelity design assets)

- [x] Configured the Stitch MCP server in `~/.config/opencode/opencode.json` using the provided API key
- [x] Downloaded the two requested Stitch screens from project `16696644163770594084` with `curl -L`
- [x] Decoded the JSON `dataString` wrappers into real files:
  - `design/otwin-login.jpg` (390x898) and `design/otwin-register.jpg` (390x1107)
  - `design/otwin-login.html` and `design/otwin-register.html` (source code)
- [x] Opened both images in the Review pane for visual inspection

### What's Done (follow-up: Stitch design applied to XML)

- [x] `activity_login.xml` rebuilt to match the Stitch login screen: app bar with back/brand/avatar, gradient car mark with "Terpercaya" badge, "Mobil Bersih & Terawat" pill, "Masuk ke Akun Anda" heading, Google button, "atau masuk dengan email" divider, labeled inputs with leading icons + focus/error states, Remember Me / Lupa Kata Sandi, "Masuk Sekarang" button, SSL banner, fleet preview, and register link
- [x] `activity_register.xml` rebuilt to match the Stitch register screen: promo pill, "Buat Akun otw.in" heading, two feature cards, elevated form card with 5 fields, +62 prefix, password strength meter, terms checkbox, "Daftar Akun" button, divider, Google sign-up, login link, and trust badges
- [x] All IDs used by `Login.kt` / `Register.kt` preserved (txtEmail, txtPassword, checkboxRemember, til*, btnLogin, btnRegister, btnGoogle, tvForgotPassword, tvRegisterLink, tvLoginLink)
- [x] Added new icon and button drawables: `ic_alternate_email`, `ic_lock_reset`, `ic_arrow_forward`, `ic_star`, `ic_shield`, `ic_shield_check`, `bg_button_login`, `bg_button_google_outline`, `bg_button_icon_circle`, `bg_logo_gradient`, `bg_avatar_circle`, `bg_badge_green_pill`, `bg_strength_bar`, `bg_strength_track`
- [x] Added `color/button_primary_states.xml`, `color/button_google_states.xml`, `color/box_stroke_states.xml` for pressed/disabled/focused/error states
- [x] Added Indonesian strings and 8pt-grid dimens for the new layout scale
- [x] Removed 106 unreferenced drawables; 32 drawables remain, all referenced
- [x] Validated 46 XML files: zero parse errors, all `@drawable`, `@color`, `@string`, `@dimen` references resolve, no percentage radii

### What's Done (follow-up: Firebase online auth/database)

- [x] Added `firebase-bom`, `firebase-auth`, `firebase-database` to the version catalog and `app/build.gradle.kts`
- [x] Google Services plugin is applied only when `app/google-services.json` exists, so the project stays buildable before Firebase setup
- [x] Added `android.permission.INTERNET` to the manifest
- [x] Created root `database/` folder with `database.rules.json`, `users.sample.json`, `google-services.example.json`, and `README.md` (Indonesian setup guide)
- [x] Added `res/values/firebase.xml` with `firebase_database_url` placeholder for the URL the user will paste later
- [x] Added `com.example.otwin.database`: `FirebaseService` (lazy auth + `users` reference), `UserProfile`, `AuthRepository` (login/register with Firebase Auth and `users/{uid}` read/write)
- [x] `Login.kt` / `Register.kt` now validate locally, show a loading state, call Firebase, map errors to Indonesian messages, and navigate on success
- [x] Terms checkbox is now enforced during registration
- [x] Validated TOML, JSON, and XML: all parse cleanly, no missing `@string` references

### What's Done (follow-up: Google / Gmail sign-in)

- [x] Added `play-services-auth` (21.2.0) and `@integer/google_play_services_version` metadata in `AndroidManifest.xml`
- [x] Saved the user-provided Web client ID in `res/values/firebase.xml` as `google_web_client_id`
- [x] Added `GoogleAuthRepository.kt`: GoogleSignInOptions with `requestIdToken`, Firebase `GoogleAuthProvider` credential exchange, and `users/{uid}` profile upsert
- [x] Wired both `btnGoogle` buttons through `startActivityForResult` in `Login.kt` / `Register.kt`
- [x] Added `FirebaseService.isFirebaseReady` (via `FirebaseApp.getApps`) so a missing `google-services.json` shows a message instead of crashing
- [x] Google login no longer depends on the Realtime Database URL; profile writing is skipped until that URL is filled
- [x] Google sign-up from `Register` now opens `MainActivity` instead of bouncing back to `Login`
- [x] Updated `database/README.md` with the Google provider, Web client ID, SHA-1, and config-file steps
- [x] Validated 47 XML files, 3 JSON files, and the TOML catalog: zero parse errors, no missing `@string` references

### What's Done (follow-up: database URL)

- [x] Filled `firebase_database_url` in `res/values/firebase.xml` with the user-provided URL `https://otwin-89dca-default-rtdb.asia-southeast1.firebasedatabase.app` (trailing slash removed)
- [x] Re-parsed all XML: zero errors
- [ ] `app/google-services.json` still not present in the project — user must place the downloaded file there and Sync Gradle before running

### What's Done (follow-up: username + phone aliases)

- [x] Added username field (`tilUsername` / `txtUsername`) to `activity_register.xml`; login field now accepts any identifier (`inputType="text"`)
- [x] `UserProfile` gained a `username` field; `FirebaseService.rootReference()` added for multi-path writes
- [x] `AuthRepository.login()` resolves `usernames/{username}` or `phones/{phone}` → uid → `loginEmails/{uid}` → email, then calls `signInWithEmailAndPassword`
- [x] `AuthRepository.register()` validates alias uniqueness, normalizes username (lowercase) and phone (62-prefixed digits), then writes `users/{uid}`, `loginEmails/{uid}`, `usernames/{username}`, `phones/{phone}` atomically via `updateChildren`
- [x] Failed profile write deletes the just-created auth account so the user can retry
- [x] Updated `database.rules.json` with the three alias nodes: public read (needed pre-auth), owner-only single write (`!data.exists()`)
- [x] Updated `users.sample.json` and `database/README.md` with the new schema and login flow
- [x] Added validation + Indonesian error strings for username/phone in `Login.kt` and `Register.kt`
- [x] Validated 47 XML, 3 JSON, TOML: zero errors; all ViewBinding IDs present; no missing `@string` references

### What's Done (follow-up: Google sign-in diagnostics)

- [x] `AuthResult.Failure` now carries an optional `detail` shown in the toast
- [x] `GoogleAuthRepository` checks Google Play services availability before launching sign-in
- [x] Added `Log.e` with tag `GoogleAuth` for ApiException status codes and FirebaseAuthException error codes
- [x] Added dedicated strings for missing Google Play services, client-ID/ID-token mismatch, and account-exists-with-different-credential
- [x] `Login.kt` / `Register.kt` show the detailed failure via `showAuthFailure`
- [ ] Root cause still likely environment: `app/google-services.json` is still absent from the project and must be placed there + Gradle synced

### What's Done (follow-up: tidy layouts + password strength)

- [x] `activity_login.xml` and `activity_register.xml` rebuilt with the same app bar, 20dp gutter, consistent 8pt spacing and one form card — no more random offsets/oversized touch targets
- [x] All ViewBinding IDs used by `Login.kt` / `Register.kt` preserved; new IDs `tvStrengthValue`, `strengthBar1-3` added
- [x] `Register.kt` now watches the password field: empty → gray bars, weak (<8 or missing letter/digit) → red, medium → amber, strong (symbol/mixed case/12+) → green
- [x] `bg_strength_bar.xml` baseline color changed to inactive gray so untinted state is correct
- [x] Registration validation aligned with the meter: 8+ chars with letter + digit; password no longer trimmed
- [x] Confirm-password field now uses `password_toggle`; back buttons close the screens; forgot-password toast localized
- [x] Revalidated: 47 XML + 3 JSON + TOML parse cleanly; no missing `@string`/`@color`/`@dimen`/`@drawable` refs; no duplicate or unresolved binding IDs

### What's Done (follow-up: Google services Gradle plugin)

- [x] `google-services.json` confirmed present in `app/` (project `otwin-89dca`, package `com.example.otwin`, matching RTDB URL)
- [x] `gradle/libs.versions.toml`: Google services plugin bumped to `4.6.0`
- [x] `app/build.gradle.kts`: `alias(libs.plugins.google.services)` applied in the `plugins` block; removed the old conditional `apply(...)` workaround
- [x] Root `build.gradle.kts` already had `alias(libs.plugins.google.services) apply false`; `settings.gradle.kts` already has `google()` repository
- [x] `app/.gitignore` now ignores `google-services.json`
- [ ] Gradle sync/build intentionally not run (user instruction); needs Android Studio sync
- [ ] `oauth_client` in `google-services.json` is empty and the hardcoded Web client ID belongs to project `otwin-515917`, not `otwin-89dca` — user must supply the correct Web client ID

### What's Done (follow-up: generated Firebase resources)

- [x] Removed manual `firebase_database_url` from `firebase.xml` — the google-services plugin already generates it (avoiding a duplicate-resource build error)
- [x] Replaced the wrong-project hardcoded `google_web_client_id` with `google_web_client_id_fallback=YOUR_WEB_CLIENT_ID`
- [x] `GoogleAuthRepository` now reads the plugin-generated `default_web_client_id` from `google-services.json` (`oauth_client` client_type 3) via `resources.getIdentifier`, falling back only when absent
- [x] Updated the "not configured" message to tell the user to re-download `google-services.json` after enabling Google
- [ ] User must re-download `google-services.json` now that Google is enabled (current copy has `oauth_client: []`), replace `app/google-services.json`, then Sync Gradle
- [ ] Verify SHA-1 `C4:81:FB:8E:94:A2:F1:01:28:78:40:1A:1E:72:B7:98:EF:EC:18:76` is registered in project `otwin-89dca`
- [ ] If OAuth consent screen is in Testing, add `jancokw33@gmail.com` as a test user

### What's Done (follow-up: unique debug SHA-1)

- [x] Confirmed the old SHA-1 `C4:81:...` is genuinely from `~/.android/debug.keystore` and from the existing debug APK (verified with `apksigner`)
- [x] Generated a project-specific `app/debug.keystore` with a fresh SHA-1 `86:78:53:E9:A1:A8:B0:72:1B:43:85:86:84:66:2C:AF:1E:02:24:C2`
- [x] `app/build.gradle.kts` overrides `signingConfigs.debug` to use that keystore
- [ ] User: add the NEW SHA-1 above in Firebase `otwin-89dca` → Project Settings → Your apps → `com.example.otwin`
- [ ] User: re-download `google-services.json`, replace `app/google-services.json`, Sync Gradle
- [ ] User: uninstall the old app before installing the newly signed build

### Compile fix

- [x] `Register.kt` TextWatcher: replaced `s.orEmpty().toString()` with `s?.toString() ?: ""` — `Editable?` has no `orEmpty()` extension (reported by compiler at line 62)

### google-services.json diperbarui

- [x] Found newer download at `/home/xav/Downloads/google-services (1).json` (01:08) and copied it to `app/google-services.json`
- [x] Verified: project `otwin-89dca`, package `com.example.otwin`, 2 `oauth_client` entries (type 1 + type 3)
- [x] Web client ID for generated `default_web_client_id`: `639129995825-m0ak50hs6vlt4dijb6k5e8nhqnt4c1eu.apps.googleusercontent.com` — belongs to the correct project

### Google sign-up profile failure

- [x] Identified that `auth_profile_error` only comes from `GoogleAuthRepository.saveProfile` — the user signed up with the Google button, not email/password
- [x] `saveProfile` now continues the sign-in with an in-memory profile when RTDB read/write is denied (typically rules not published), logging the cause under tag `GoogleAuth` instead of blocking navigation
- [ ] User must still Publish the rules from `database/database.rules.json` in Firebase → Realtime Database → Rules for profile persistence

### Registration success screen

- [x] Added `RegisterSuccess.kt` + `activity_register_success.xml` (Stitch-style app bar, green check badge, account summary, CTA "Masuk Sekarang")
- [x] Added `bg_success_circle.xml` drawable and Indonesian success strings
- [x] Registered `.RegisterSuccess` in `AndroidManifest.xml`
- [x] `Register.kt` success path now opens the success screen with the registered profile; removed the old toast + immediate login redirect
- [x] Static validation clean (XML/JSON/TOML, resource refs, duplicate IDs, binding IDs)

### Persistent session

- [x] `Login.kt` checks `FirebaseService.auth.currentUser` on launch; with "Ingat Saya" on (default), it goes directly to `MainActivity` with `CLEAR_TASK|NEW_TASK`
- [x] "Ingat Saya" preference is saved on email/password and Google login; when unchecked, the old session is signed out on next launch
- [x] `activity_login.xml` checkbox now defaults to checked
- [x] `MainActivity` shows the signed-in email and has a **Keluar** button that signs out and returns to Login
- [x] Confirmed via cached `firebase-auth-23.2.1` API jar that `setPersistence` is unavailable — used Firebase Auth's default local persistence instead
- [ ] User: Sync/Build, login, close/swipe the app, reopen — should go straight to home; use **Keluar** to actually log out
- [ ] Catatan: clearing app *data* (Settings → Clear data) will still remove the session by design

### App logo (user asset)

- [x] Copied `/home/xav/Downloads/logo.jpeg` to `app/src/main/res/drawable-nodpi/app_logo.jpg` (resized 1600→512 px)
- [x] Login, Register and RegisterSuccess app bars now use `app_logo` instead of `ic_car`; login hero brand uses it too
- [x] Removed `app:tint` on those ImageViews so the logo keeps its original colors; added `scaleType="fitCenter"`
- [x] MainActivity home screen displays the same logo
- [x] Static validation clean

### Launcher icon from same logo

- [x] Generated legacy square + round icons and adaptive foreground PNGs for all five densities from `logo.jpeg`
- [x] Removed stock webp icons and default robot vector drawables
- [x] `mipmap-anydpi/ic_launcher.xml` + `ic_launcher_round.xml` now use white background color + the new foreground
- [x] Manifest icon references unchanged; static validation clean

### Login screen cleanup

- [x] Removed top app bar/back button from `activity_login.xml` (and its `btnBack` listener in `Login.kt`)
- [x] Removed the SSL security note and the "35+ Pilihan Unit Siap Jalan" fleet preview
- [x] Login form is now vertically centered via `contentContainer` `match_parent` + `gravity="center_vertical"`; `NestedScrollView` + `fillViewport` keeps it usable on small screens
- [x] Static validation clean

### Register screen cleanup

- [x] Removed the top app bar/back button from `activity_register.xml` (and its `btnBack` listener in `Register.kt`)
- [x] Replaced header spacing with a 16dp top padding; long form remains `wrap_content` inside `NestedScrollView` + `fillViewport` so it scrolls automatically without clipping
- [x] Static validation clean

### Stitch Beranda customer home (post login/register)

- [x] Fetched the Stitch screen `Beranda - otw.in` through Stitch MCP (`download_file`); raw `curl` of its hosted URL returns 401, but the four car images and the avatar URL are public and were downloaded with `curl -L`
- [x] Added `activity_main.xml` matching the Stitch Beranda layout: header, greeting + verified badge, rental-mode toggle, booking card, category chips, vehicle cards, benefits, Chat CS and fixed bottom nav
- [x] Rewrote `MainActivity.kt` from Compose to ViewBinding with dynamic greeting (Firebase Auth + RTDB name), DatePickerDialog date/duration logic, interactive toggle/chips/nav, WhatsApp link, and logout through the account dialog (clears `remember_me`)
- [x] Added Stitch palette colors, Indonesian strings, 20 shape drawables and 31 vector icons
- [x] Static validation clean
- [x] Fixed IDE error `Unresolved reference 'DatePickerDialog'` by adding the missing `android.app.DatePickerDialog` import in `MainActivity.kt`
- [x] Fixed AAPT2 resource-linking errors in `activity_main.xml`: replaced the unsupported `android:gravity="baseline"` on two price rows with `center_vertical`

### What's Done (follow-up: full menu/feature functionality)

- [x] Added `Vehicle.kt`, `VehicleRepository.kt` and `RentalOrder.kt` for the local fleet catalog and RTDB order model
- [x] Added `item_vehicle.xml`, `item_order.xml`, `dialog_booking.xml` and `dialog_name.xml`
- [x] Rebuilt `activity_main.xml` with real Beranda/Cari/Pesanan/Akun page containers, search field/chips, "Semua" chips, card/price IDs and an empty-category message
- [x] Rewrote `MainActivity.kt` so every home/menu action is real:
  - [x] Category chips filter the Beranda cards and the Cari list
  - [x] Date cards use `DatePickerDialog`; inclusive duration updates labels and prices
  - [x] Lepas Kunci / Dengan Sopir changes the daily rate (+Rp 250.000/day for a driver)
  - [x] Every vehicle card and Sewa button opens a booking confirmation with 10% discount for 3+ days
  - [x] Confirmed bookings are written to `orders/{uid}/{orderId}` in Realtime Database
  - [x] Pesanan tab uses a live `ValueEventListener`, shows totals/status and supports cancellation
  - [x] Akun tab shows the Firebase/RTDB profile, edits the name, opens Pesanan and logs out
  - [x] Location header opens a city picker persisted in `otwin_session`
  - [x] Chat CS opens WhatsApp; profile shortcut opens Akun
- [x] Added `AuthRepository.resolveEmail`; Login now sends a real Firebase password-reset link and Register's forgot-password link navigates to Login
- [x] Updated `database.rules.json`, `users.sample.json` and `database/README.md` with the `orders` schema and owner-only rules (still needs Publish in Firebase console)
- [x] Fixed lint error `StringFormatInvalid` in `register_promo` by escaping `10%%`
- [x] Made `init.sh` auto-detect Android Studio's JBR when `JAVA_HOME`/`java` are absent
- [x] Verification with user permission: `./gradlew clean compileDebugKotlin`, `./gradlew test lint build`, and `./init.sh` all ended with **BUILD SUCCESSFUL**; lint has 0 errors
- [x] Static validation: XML/JSON/TOML parse clean, no duplicate layout IDs, no unresolved binding IDs

### What's Done (follow-up: remove post-login/register photo)

- [x] Beranda header profile button and Akun profile card no longer show `avatar_customer.jpg`
- [x] Both now use the neutral `ic_home_person` icon tinted with `home_primary`
- [x] Removed the unused `app/src/main/res/drawable-nodpi/avatar_customer.jpg`
- [x] `./init.sh` re-run: BUILD SUCCESSFUL

### What's Done (follow-up: remove all dummy data)

- [x] Confirmed approach with user: admin uploads via **both** in-app panel and Firebase Console; images from **URL or uploaded photo**; Beranda cards become a **dynamic RTDB list**
- [x] Deleted all hardcoded fleet data: 4 static Beranda cards, vehicle name/price/spec strings, `users.sample.json`, and `car_innova/ioniq/xpander/hrv.jpg`
- [x] Generalized placeholder `tools:text` values in the register-success layout (no fake person names)
- [x] `Vehicle.kt` now uses `imageUrl` + `active` + `createdAt` and serializes with `toMap()`
- [x] `VehicleRepository.kt` no longer has a local catalog — it reads/writes `vehicles/{id}` in RTDB and filters live data
- [x] Added `AdminActivity` + admin layouts: admin-only CRUD form, image picker (compressed data URI) and URL field
- [x] Added `VehicleImageLoader.kt`: Coil for http(s), base64 decode for uploaded data URIs, generic car icon fallback
- [x] `MainActivity` observes `vehicles` and `admins`, renders Beranda/Cari from RTDB, shows empty state until admin uploads, and stores `imageUrl` in orders
- [x] `database.rules.json`: public-read `vehicles`, admin-only writes, self-readable `admins` node that cannot be written from the app
- [x] `database/README.md`: admin setup, both upload paths, generic example schema
- [x] `./init.sh` re-run: BUILD SUCCESSFUL (test + build + lint), lint 0 errors; static scan finds no dummy data left

### What's Done (follow-up: Stitch admin dashboard)

- [x] Downloaded the Stitch screen `Admin Mobile - Ringkasan Dashboard` (project `16696644163770594084`, screen `7fa5578f0ca64bd5adf24ece8cf43017`) through Stitch MCP `download_file`; raw `curl` of the hosted URL returns 401. Saved `design/otwin-admin-dashboard.html` and `design/otwin-admin-dashboard.jpg` (390x1614).
- [x] Replaced `activity_admin.xml` with the Stitch information architecture: Admin Portal header with logo/notification/avatar, welcome + total revenue banner, operational pills, four KPI cards, Menunggu Verifikasi card list, Komposisi Armada segmented bars, Aktivitas Terkini, and a fixed Ringkasan/Armada/Pesanan/Keuangan bottom nav.
- [x] Added `item_admin_verification.xml`, `item_admin_order.xml`, `item_admin_activity.xml`, plus admin-specific shape/vector drawables for gradients, pills, tracks, icons and avatar.
- [x] `AdminActivity.kt` now drives every dashboard value from Realtime Database: total/monthly revenue, open bookings, pickups today, idle fleet, fleet total, utilization label/fill, urgent count, verification list, category composition bars, recent activity and finance summary. No Stitch example amounts, unit counts or customer names are hardcoded.
- [x] Kept the full vehicle CRUD on the Armada tab (add/edit/delete, URL or uploaded photo, active flag) and added an order list on Pesanan with a least-privilege action to mark `status = Selesai`.
- [x] `RentalOrder.kt` gained `customerName` and `customerUsername`; `MainActivity.kt` copies the signed-in profile into new orders so admin order cards have real customer data.
- [x] `database.rules.json` now lets admins read all orders and write only the `status` child; normal users still create/cancel their own orders. `database/README.md` documents the new fields and rules.
- [x] Added Indonesian strings for all dashboard copy and relative-time labels; the header logo uses the existing `app_logo` asset instead of the Stitch sample letter.
- [x] Fixed compile error: `AlertDialog.Builder.setMessage` has no varargs overload here, so the order date range is formatted with `getString(...)` first.
- [x] Verification: `./init.sh` BUILD SUCCESSFUL (test + build + lint), lint 0 errors / 314 warnings; 124 XML files, all JSON files and TOML parse cleanly; all resource references resolve; no duplicate IDs inside a layout; zero dummy-data string hits.

### What's Done (follow-up: register tidak auto-login)

- [x] Root cause: `AuthRepository.createAccount` memakai `createUserWithEmailAndPassword`, yang otomatis menandatangani akun baru sebagai `currentUser`. Setelah `RegisterSuccess` membuka `Login`, `restoreExistingSession()` melihat sesi aktif dan langsung mengalihkan ke `MainActivity`.
- [x] Setelah multi-path write `users` + `loginEmails` + `usernames` + `phones` berhasil, `AuthRepository` sekarang memanggil `auth.signOut()` sebelum mengembalikan `AuthResult.Success`.
- [x] Path gagal tetap menghapus akun Auth yang setengah jadi dengan `auth.currentUser?.delete()` sebelum callback error; sign-out tidak dipindah ke path gagal.
- [x] Google sign-up dari Register sengaja tetap masuk otomatis karena itu memang alur Google Sign-In.
- [x] Dampak: pengguna bisa mendaftar beberapa akun dari satu perangkat; setelah daftar, aplikasi kembali ke form login dan user login manual dengan email/username/HP akun mana pun. Firebase Auth tetap hanya menjaga satu sesi aktif dalam satu waktu.
- [x] Verification: `./init.sh` BUILD SUCCESSFUL (test + build + lint), lint 0 errors; `feature_list.json` parse cleanly.

### What's Done (follow-up: admin login routing)

- [x] `VehicleRepository.isAdmin(uid)` membaca `admins/{uid}` dengan `addListenerForSingleValueEvent`; rule saat ini mengizinkan user membaca flag miliknya sendiri, sehingga pengecekan peran aman dilakukan tepat setelah Firebase Auth menyimpan sesi.
- [x] Karena rewrite `VehicleRepository.kt` tidak sengaja menghapus helper lama, `save(vehicle)` dan `delete(vehicleId)` dikembalikan; `AdminActivity` memakai keduanya untuk CRUD armada.
- [x] `Login.kt`: `goToTarget()` menggantikan `goToMain()` untuk tiga jalur — login email/password, Google Sign-In, dan `restoreExistingSession()`. Admin masuk ke `AdminActivity`, user biasa ke `MainActivity`, keduanya dengan `CLEAR_TASK | NEW_TASK`.
- [x] `MainActivity.redirectAdminIfNeeded()` menjaga jika admin somehow membuka activity customer (termasuk cold-start); `AdminActivity.verifyAdmin()` tetap menjadi gate terakhir.
- [x] Akun admin sudah dibuat: username `admin`, password `admin123`, email internal `admin@otwin.in`, UID `iNYV2xCvzZTuKOeU7Ewbexfy9eH2`; alias `usernames/admin` dan `loginEmails/{uid}` sudah aktif dan password sudah diuji.
- [x] Verification: `./init.sh` BUILD SUCCESSFUL (test + build + lint), lint 0 errors; `feature_list.json` parse cleanly.

### What's In Progress

- None

### What's Next

- Publish the updated `database/database.rules.json` in Firebase Console → Realtime Database → Rules. It now contains `orders` admin read access and admin-only `status` writes, plus the `admins`/`vehicles` nodes.
- Add the admin's Firebase Auth **UID** to Realtime Database `admins/{uid} = true` from the Console; the app cannot grant admin to itself by design. UID: `iNYV2xCvzZTuKOeU7Ewbexfy9eH2`.
- Admin must upload at least one vehicle via Panel Admin → Armada/Tambah Unit or Firebase Console → `vehicles`; until then Beranda/Cari show the empty state and dashboard KPIs stay at zero/empty states.
- On-device smoke test: logout → login `admin` / `admin123` → harus mendarat di dashboard admin (bukan Beranda) → tutup app dan buka lagi → tetap di dashboard admin → logout → register akun kedua → form login → login user biasa → Beranda → admin creates vehicle with URL/photo → Cari filter → Sewa → admin sees the order in Ringkasan/Pesanan → mark Selesai → Akun edit name → logout.

### What's Done (follow-up: resource linking fix)

- [x] `bg_button_icon.xml` used `android:radius="50%"`, which AAPT2 rejects because `radius` requires a dimension; replaced with `24dp` in all four states
- [x] Added an explicit 48dp intrinsic size so the icon button renders as a circle
- [x] Re-validated all 134 XML files: zero errors, no percentage radii remain

### What's Done (follow-up: ColorStateList linking fix)

- [x] Removed `android:state_error` from `color/box_stroke_states.xml` — AAPT2 does not define that state attribute for a ColorStateList
- [x] Error color is already handled by `app:boxStrokeErrorColor="@color/error_color"` on each `TextInputLayout`
- [x] Re-validated all XML: zero parse errors, no `state_error` occurrences remain

## Blockers / Risks

- Firebase Realtime Database rules still need to be Published from `database/database.rules.json`; otherwise `orders`, `vehicles` and profile writes, admin order-list reads and admin status updates will be denied at runtime.
- Uploaded photos are stored as compressed data URIs in RTDB (no Firebase Storage). Keep images small; the app downscales to 640 px / JPEG quality 70.
- Admin access must be granted from Firebase Console; this is intentional so a normal user cannot promote themselves.
- `JAVA_HOME` is unset in this shell environment. `init.sh` now falls back to Android Studio's bundled JBR, and manual Gradle runs use `JAVA_HOME=/snap/android-studio/252/jbr`.
- Google sign-in and password reset need an on-device check with the current debug SHA-1 registered in Firebase.

## Decisions Made

- **Reusability**: Icons are monochrome outline vectors tinted via `@color/dark_navy`; status icons use semantic palette colors. All can be re-tinted from layouts/Kotlin.
- **Interactive states**: Buttons use `StateListDrawable` with ripple items; disabled and focused states are explicit.
- **Color discipline**: New colors are named resources in `colors.xml`; only the multi-color Google "G" and the logo use literal brand hex values where required.
- **Existing resources**: Kept legacy drawables that are still referenced by login/register layouts, only upgraded the ones whose names were required by the new library.

## Files Modified This Session

- `app/src/main/res/layout/activity_login.xml` - fully rebuilt from the Stitch login design
- `app/src/main/res/layout/activity_register.xml` - fully rebuilt from the Stitch register design
- `app/src/main/res/drawable/*.xml` - 14 new icon/button assets added, 106 unreferenced assets removed (32 kept)
- `app/src/main/res/color/*.xml` - 3 new ColorStateLists for buttons and input strokes
- `app/src/main/res/values/strings.xml` - Indonesian copy matching the Stitch screens
- `app/src/main/res/values/dimens.xml` - 8pt-grid auth dimensions
- `app/src/main/res/values/themes.xml` - Material3 theme with otw.in colors
- `app/src/main/res/values/colors.xml` - otw.in palette / state / ripple colors
- `app/src/main/res/values/firebase.xml` - Firebase Realtime Database URL placeholder
- `app/src/main/AndroidManifest.xml` - INTERNET permission
- `app/src/main/java/com/example/otwin/database/*.kt` - FirebaseService, UserProfile, AuthRepository
- `app/src/main/java/com/example/otwin/Login.kt` / `Register.kt` - Firebase-backed auth flow
- `database/*.json`, `database/README.md` - Firebase uploadable rules/sample/config template and setup guide
- `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` - Firebase + Play Services Auth dependencies, conditional Google Services plugin
- `app/src/main/java/com/example/otwin/database/GoogleAuthRepository.kt` - Google/Gmail sign-in through Firebase
- `app/src/main/res/values/firebase.xml` - Web client ID + database URL placeholder
- `app/src/main/AndroidManifest.xml` - INTERNET permission + google_play_services_version metadata
- `app/src/main/java/com/example/otwin/Login.kt` / `Register.kt` - Google buttons wired with activity results
- `app/src/main/res/layout/activity_register.xml` - username field added
- `app/src/main/java/com/example/otwin/database/AuthRepository.kt` - alias resolution + multi-path registration
- `app/src/main/java/com/example/otwin/database/UserProfile.kt` - username field
- `database/database.rules.json`, `database/users.sample.json`, `database/README.md` - alias schema/rules/flow
- `app/src/main/res/layout/activity_register.xml` - cleaned, auto-sized register form
- `app/src/main/res/layout/activity_login.xml` - cleaned, auto-centered login form
- `app/src/main/res/mipmap-*/ic_launcher*.png` - launcher icons generated from user logo
- `app/src/main/res/drawable-nodpi/app_logo.jpg` - logo from user asset
- `app/src/main/res/layout/activity_main.xml` - Stitch Beranda customer home layout
- `app/src/main/java/com/example/otwin/MainActivity.kt` - Stitch Beranda behaviour, dynamic greeting, booking dates, nav and logout
- `app/src/main/res/drawable-nodpi/car_*.jpg`, `avatar_customer.jpg` - Beranda images downloaded from the Stitch screen
- `app/src/main/java/com/example/otwin/Login.kt` - session restore + remember preference
- `app/src/main/java/com/example/otwin/RegisterSuccess.kt` - post-registration success screen
- `app/src/main/res/layout/activity_register_success.xml` - success screen layout
- `app/debug.keystore` - new project-specific debug signing key
- `app/build.gradle.kts` - debug signing config points to the new keystore
- `app/src/main/res/values/firebase.xml` - manual generated-resource definitions removed
- `app/src/main/java/com/example/otwin/database/GoogleAuthRepository.kt` - reads generated default_web_client_id
- `gradle/libs.versions.toml` - Google services plugin 4.6.0
- `app/build.gradle.kts` - plugin applied in `plugins` block (conditional workaround removed)
- `app/.gitignore` - ignore google-services.json
- `app/src/main/res/layout/activity_login.xml` / `activity_register.xml` - tidied to one consistent grid
- `app/src/main/res/drawable/bg_strength_bar.xml` - inactive baseline color
- `app/src/main/res/values/strings.xml` - strength labels + localized auth strings
- `feature_list.json` - added feat-006 through feat-014
- `progress.md` - recorded session state
- `app/src/main/java/com/example/otwin/Vehicle.kt` - fleet model and Rupiah formatter
- `app/src/main/java/com/example/otwin/VehicleRepository.kt` - local fleet catalog and filters
- `app/src/main/java/com/example/otwin/RentalOrder.kt` - RTDB order model
- `app/src/main/java/com/example/otwin/MainActivity.kt` - fully working Beranda/Cari/Pesanan/Akun flows
- `app/src/main/java/com/example/otwin/database/AuthRepository.kt` - `resolveEmail` for password reset
- `app/src/main/java/com/example/otwin/Login.kt` - real Firebase password-reset action
- `app/src/main/java/com/example/otwin/Register.kt` - forgot-password link opens Login
- `app/src/main/res/layout/activity_main.xml` - page containers, chips, card/price IDs, empty state
- `app/src/main/res/layout/item_vehicle.xml`, `item_order.xml`, `dialog_booking.xml`, `dialog_name.xml` - new dynamic UI
- `app/src/main/res/values/strings.xml` - new booking/order/account/reset strings plus `10%%` lint fix
- `database/database.rules.json`, `users.sample.json`, `README.md` - orders schema/rules
- `init.sh` - Android Studio JBR fallback for environments without `JAVA_HOME`
- `app/src/main/java/com/example/otwin/Vehicle.kt` - RTDB-backed vehicle model with imageUrl/toMap
- `app/src/main/java/com/example/otwin/VehicleRepository.kt` - RTDB fleet access + filters (no dummy catalog)
- `app/src/main/java/com/example/otwin/VehicleImageLoader.kt` - URL/data-URI image loading and photo compression
- `app/src/main/java/com/example/otwin/AdminActivity.kt` - admin-only vehicle CRUD panel
- `app/src/main/res/layout/activity_admin.xml`, `item_admin_vehicle.xml`, `dialog_vehicle.xml` - admin UI
- `database/database.rules.json` - added admins and vehicles nodes
- `database/README.md` - admin setup, upload paths, generic schema; removed users.sample.json
- `gradle/libs.versions.toml`, `app/build.gradle.kts` - Coil 2.7.0 for remote images
- `design/otwin-admin-dashboard.html`, `design/otwin-admin-dashboard.jpg` - Stitch admin dashboard source and screenshot
- `app/src/main/res/layout/activity_admin.xml` - rebuilt as the Stitch Ringkasan dashboard with four bottom-nav pages
- `app/src/main/res/layout/item_admin_verification.xml`, `item_admin_order.xml`, `item_admin_activity.xml` - new admin list rows
- `app/src/main/res/drawable/bg_admin_*.xml`, `ic_admin_*.xml` - admin dashboard shapes/icons
- `app/src/main/java/com/example/otwin/AdminActivity.kt` - dashboard metrics, order verification/status update and four-page navigation
- `app/src/main/java/com/example/otwin/RentalOrder.kt`, `MainActivity.kt` - customer identity copied into orders for the admin list
- `app/src/main/res/values/strings.xml` - Indonesian admin dashboard copy and relative-time labels
- `database/database.rules.json`, `database/README.md` - admin order read + status-only write rules and schema updates
- `feature_list.json`, `progress.md` - feat-029 status and continuity log
- `app/src/main/java/com/example/otwin/database/AuthRepository.kt` - sign out after successful email/password registration so it no longer auto-logs in
- `feature_list.json`, `progress.md` - feat-030 status and continuity log
- `app/src/main/java/com/example/otwin/VehicleRepository.kt` - `isAdmin()` role lookup plus restored `save()`/`delete()` fleet helpers
- `app/src/main/java/com/example/otwin/Login.kt` - role-aware post-login/session-restore routing
- `app/src/main/java/com/example/otwin/MainActivity.kt` - admin redirect guard
- `feature_list.json`, `progress.md` - feat-031 status and continuity log

## Evidence of Completion

- [x] XML validation: 103 files parsed, 0 errors
- [x] TOML / JSON / XML validation: zero parse errors
- [x] Reference validation: no duplicate layout IDs and no unresolved Kotlin ViewBinding IDs
- [x] `./gradlew clean compileDebugKotlin`: BUILD SUCCESSFUL
- [x] `./gradlew test lint build`: BUILD SUCCESSFUL (lint 0 errors, 261 warnings)
- [x] `./init.sh`: BUILD SUCCESSFUL for test, build and lint (re-run after the admin login routing fix)
- [x] Lint report: 0 errors / 314 warnings; XML/JSON/TOML parse clean; no duplicate IDs inside a layout; zero dummy-data string hits
