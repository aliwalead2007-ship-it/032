# قبس (Qabas) — استوديو صناعة المحتوى الإسلامي بالذكاء الاصطناعي

> **قاعدة الحوكمة:** اقرأ **`AGENTS.md` بالكامل أولاً** قبل أي تعديل — هو الوثيقة الحاكمة (الحالة التشغيلية، الممنوعات، الخطوة التالية الوحيدة).
> أي عمل جوهري **يجب** أن يحدّث `AGENTS.md` و`README.md` معاً وإلا فالعمل غير مكتمل.

| | |
|---|---|
| الحزمة | `com.example` / `com.example.fbviki` |
| الإصدار | 1.2.0 (versionCode 2) |
| SDK | minSdk 24 · target/compile 36 |
| التقنية | Kotlin 2.0.21 · Jetpack Compose (Material 3) |

---

## 1. ما هو قبس

**قبس** استوديو أندرويد متكامل لصناعة المحتوى الدعوي والإسلامي: من **فكرة** يكتبها المستخدم حتى **فيديو MP4** منتهٍ (كابشن عربي + تعليق صوتي + مونتاج سينمائي):

```
فكرة → فهم وتحليل (AI) → سكربت ومشاهد → B-Roll → صوت (TTS) → معالجة FFmpeg → مراجعة → حفظ/مشاركة
```

المعالجة تجري **محلياً على الجهاز عبر FFmpeg Kit**، والذكاء الاصطناعي يشغّل كتابة السكربت وهندسة المشاهد والتحليل — مع fallback مجاني عند غياب المفاتيح أو الشبكة.

---

## 2. الهوية البصرية واللغة

- **السمة الداكنة (الافتراضية):** Dark Slate + Gold
  - الخلفية العميقة `#0B0F19` · سطوح البطاقات `#151B2B`
  - الذهبي الأساسي `#E8C547` · الذهبي الثانوي `#F5D76E`
  - النصوص `#F8FAFC` / `#94A3B8`
- **السمة الفاتحة:** `#F8FAFC` بخلفيات بيضاء وسطحى `#F1F5F9`.
- **الخط:** Cairo (عربي/لاتيني مرن).
- **RTL كامل:** اللغة الافتراضية `ar` (تُحفظ في `SharedPreferences` تحت `app_language`)، مع موارد `values-ar`.
- **شاشة الدخول:** سبيلاش بهوية قبس + آية الدخول «إن إلى ربك الرجعى» (~7ث، fade in/out عبر `AudioPlayerManager`) + بوابة استوديو ذهبية (Lottie).

- **توقيع AI (سبتمبر 2026):** هوية تقنية بنفسجية مهيمنة — `primary` بنفسجي `#8B5CF6` + إكسنت سيان `#22D3EE` + توهج أزرق `#3B82F6` في السمتين (الفاتحة بنفسجي `#7C3AED` / سيان `#0891B2` / بنفسجي فاتح `#A78BFA`). **الذهبي محفوظ للمقدسات** (السبلاش، بطاقة التجويد، ترويسة الدخول التبركية).
- **شاشة الدخول (سبتمبر 2026):** اللوجو الرسمي `qabas_logo` + ترحيب «أهلاً بك في قبس / استوديو الإنتاج وصناعة الأثر» + تدرج العلامة (بنفسجي→أزرق→سيان) على زر الدخول.

---

## 3. البنية التقنية

| الطبقة | التقنية |
|---|---|
| الواجهة | Jetpack Compose · Material 3 · Navigation عبر `AppState` (قائمة موحّدة من الحالات + `AnimatedContent` transition) |
| الحالة | ViewModel + StateFlow |
| قاعدة البيانات | Room (`qabas_database` v4): `projects` · `taste_profile` · `hadith_cards` · `audio_tracks` · `reel_scripts` · `series_records` |
| معالجة الفيديو | FFmpeg Kit (`ffmpeg-kit-full`) عبر `VideoProcessor` / `VideoEngineManager` / `MontageDirector` |
| التشغيل والوسائط | Media3 ExoPlayer · Coil · Lottie · OpenCV · CameraX |
| الذكاء الاصطناعي | Gemini 2.0-flash · Groq · تحليل محلي (`LocalImageAnalyzer`) |
| الصوت | Azure Speech TTS · ElevenLabs TTS · محرك نطق أندرويد (fallback مجاني دائماً) |
| الوسائط | Pexels · Pixabay (B-Roll) · HuggingFace (توليد صور) |
| السحابة | **Supabase** (نشطة) + **Firebase** (مسار اختياري) |
| المدفوعات | Billing Library 7.x (متجر/باقات/اشتراكات) |
| التخزين | Room + DataStore + SharedPreferences |

---

## 4. الشاشات والأقسام

التنقّل مركزي عبر `AppState` (أكثر من 40 حالة). المجموعات الأساسية:

- **مسار الإنتاج:** المدخل `INPUT` → `UNDERSTANDING` (تحليل الفكرة ومشاهد) → `RESOURCES` (B-Roll) → `PROCESSING` (FFmpeg مع تقدم خطي وأوقاف آمنة) → `REVIEW` → `SAVE_SHARE`.
- **الرئيسية والدراسة:** `HOME` (الرئيسية) · `LEADERBOARD` (المتصدرين والمكافآت) · `KNOWLEDGE_HUB` (طلب العلم + XP + سلاسل يومية) · `QURAN_HUB` · `HADITH_STUDIO` (بطاقات الحديث + أذكار يومية + إشعارات) · `CONTENT_GUARD`.
- **الصناعة القصيرة:** `REELS` (ريلز 9:16، هوكس، كابشن، هاشتاقات) · `TELEPROMPTER` (المُلقن الذكي) · `VIDEO_STYLE_CLONER` (استنساخ الأنماط البصرية) · `PHOTO_STUDIO`.
- **الاستوديو الذكي:** `STYLE_SELECTION` / `CREATE_STYLE_OBJECT` (عقل أسلوبي `StyleBrain` مع Radar Chart ومقارنات) · `SMART_DIRECTOR` (المخرج الذكي) · `TASTE_PROFILE`.
- **الحساب والنشر:** `LOGIN` / `REGISTER` · `PROFILE` · `SETTINGS` · `API_KEYS` (مفاتيح API وتصدير/استيراد نسخ احتياطي) · `NOTIFICATIONS` · نشر مباشر (`DirectPublisherDialog`) عبر حسابات التواصل.
- **المطور:** `DEVELOPER_DASHBOARD` · `API_DOCS`.
- **التجارب المعمارية:** `ENTERPRISE_PORTAL` · `YOUTUBE_STUDIO` · `REQUEST_CHAT` · `APP_IDEA_FORM` · `PREMIUM_UPGRADE` · `PAYMENT`.

---

## 5. الخدمات الذكية والسياسة المجانية

- **يعمل التطبيق بلا أي مفتاح:** التحليل المحلي + محرك النطق المدمج في أندرويد (TTS) + FFmpeg تعمل **دون إنترنت ودون مفاتيح**.
- **المفاتيح اختيارية** وتُدخل من شاشة «مفاتيح API» داخل الإعدادات، أو عبر ملف `.env` في البناء:
  - أنا Gemini (تحليل/سكربت/أنماط) · Groq (بديل أسرع)
  - Azure Speech (TTS) + المنطقة · ElevenLabs (TTS)
  - Pexels / Pixabay (B-Roll حقيقي) · HuggingFace (صور)
  - Supabase (URL + anon key)
- **مبدأ صارم:** لا حقن محتوى ديني خاطئ كـ fallback للصوت/النص، ولا `return true` بعد فشل صامت.
- **شاشة «مفاتيح API» (الحصاد شبه الآلي + الصحة الحيّة):**
  - **التقاط من الحافظة 📋:** يكتشف المفتاح تلقائياً بالنمط (Regex) عندما تنسخه، ويملأ الحقل ويحقّقه فوراً (عند اللصق أو عند التركيز على حقل فارغ)، مع أزرار «التقاط من الحافظة» و«افتح الموقع» للإنشاء — وهي صفّ كامل العرض **أسفل الحقل** (وليست داخل `trailingIcon` حتى لا تُسحق كباركود غير قابل للضغط).
  - **تحقق Gemini واضح:** المفتاح الذي لا يبدأ بـ `AIzaSy…` (مثل توكنات `AQ.…` الخاصة بـ OAuth) يُرفض برسالة عربية تفسّر السبب وتوجّه لإنشاء مفتاح من `aistudio.google.com/app/apikey` — لا أخطاء HTTP 400 مبهمة بعد الآن.
  - **شارة «🆓 بديل مجاني»:** على كل بطاقة بلا مفتاح، توضح البديل المحلي الذي يعمل مجاناً — الصدق أولاً، لا وهم.
  - **فحص صحة المفاتيح الحي 🩺:** لوحة ذاتية الاكتفاء تفحص 7 خدمات بأزمنة استجابة حقيقية (🟡 غير محدد / 🟢 متصل / 🔴 مرفوض) + «خطة إصلاح مقترحة 🛡️» + إجمالي مكالمات/نجاح حقيقي من `ApiUsageTracker`.

### متغيرات البيئة (`gradle` → `BuildConfig`)

قراءة المفاتيح تأتي من `.env` (جذر المشروع) عبر **secrets-gradle-plugin**. القوالب في `.env.example`.

> **تحذير:** قيمة المفتاح **لا تُترك فارغة** (`KEY=""`) — تسبب انكسار توليد `BuildConfig.java`. استخدم دائماً placeholder غير فارغ مثل `"your_key"`.

---

## 6. طبقة السحابة

- **Supabase = الطبقة النشطة حالياً** (URL `https://wyevrdnnttckaihhqxbl.supabase.co` + anon key في `.env` المحلي).
  - `SupabaseConfig.kt` محصّن ضد placeholder: أي APK بلا قيم حقيقية **يتدهور للوضع المحلي** بدل الاتصال بعنوان وهمي.
  - قراءة `users` · كتابة/حذف `transactions` · مشاريع المستخدمين — كلها مسارات حقيقية (تتحقق عبر `SupabaseServices` / `CloudServices`).
- **Firebase = مسار ثانوي/اختياري** (تحليل + Auth + Firestore + Analytics محترم لإعداد `analytics_enabled` في `qabas_prefs`).
- لوحة المطور لا تعرض بلوغاً كاذباً: متتبع الاستهلاك `ApiUsageTracker` فارغ عند البدء ويمتلئ من استدعاءات HTTP الفعلية فقط.

---

## 7. البناء والتحقق

```bash
./gradlew :app:compileDebugKotlin   # فحص سريع
./gradlew assembleDebug             # بناء APK
```

- **CI:** `.github/workflows/android-ci.yml` — build & sign، إصلاح `gradle-wrapper.jar` تلقائياً، و«**Inject secrets**»: يكتب `.env` من أسرار GitHub إن وُجدت (بدونها يبني بوضع محلي آمن). 📦 APK يرفع كـ Artifact `qabas-debug-apk` (احتفاظ 7 أيام).
- **بناء محلي مُتحقَّق:** `:app:assembleDebug` → BUILD SUCCESSFUL محلياً (Temurin JDK 17 + Android SDK platform 36 في `~/tooling`، `sdk.dir` في `local.properties`) — يثبت ربط كل الوظائف دون أخطاء ترجمة.
- **حارس الانهيارات:** `QabasCrashGuard` يلتقط أي استثناء JVM غير مُتصدّى له في أي Thread → ملف `filesDir/crash_logs/` + سطر في لوحة المطور بستاك كامل، وإعادة إطلاق تلقائية محمية ضد التكرار (نافذة 15 ثانية) للـ main thread.
- **التوقيع:** release عبر `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD` أو `my-upload-key.jks` (alias `upload`)، وdebug عبر `debug.keystore`. البناء يسير unsigned إذا غابت القيم.
- **اختبارات:** `StyleBrainTest` (عقل الأساليب) · Robolectric unit tests · Roborazzi screenshots · `ExampleInstrumentedTest`.

---

## 8. لوحة المطور — أرقام حقيقية فقط

- `ApiUsageTracker` يلتفّ حول **14 نقطة HTTP حقيقية**: Gemini ×6، Groq، Azure TTS، ElevenLabs، HuggingFace، Pexels، Pixabay، Supabase/Firestore.
- المستخدمون والشحنات والقوائم: تحميل فعلي من Firestore ثم Supabase fallback.
- العدّ الأسبوعي للمشاريع/النصوص/الأحاديث: عدّ يومي حقيقي من Room عبر `countXBetween(start,end)`.
- رسائل صادقة بدل الأرقام المختلقة: «شاشات الزيارة» غير المُتتبَّعة تُعرض كمؤشرات فارغة، وليست أرقاماً مزيّفة.

---

## 9. الوضع الحالي والخطوة التالية

- **مُحصَّن في الكود:** مسار الإنتاج (كابشن عربي، توقيتات آمنة، دمج xfade حقيقي، فلاتر LUT، Audio Ducking، عزل كابشن إلزامي، StyleDirective مطبّق فعلياً) · العقل الأسلوبي بلا أساليب مزيّفة · لوحة مطور خالية من البيانات المزيّفة · Supabase مفعّل · CI أخضر · **بناء محلي أخضر + حارس الانهيارات `QabasCrashGuard`**.
- **الأولوية المعلّقة:** **إثبات التصدير على جهاز حقيقي** — تثبيت APK → فكرة عربية قصيرة → معالجة → تصدير → تشغيل الملف (تحقق: كابشن عربي، مدة، عمل بلا نت/بلا TTS). أي انهيار الآن يُسجَّل تلقائياً في `crash_logs/` ولوحة المطور.
- **لتفعيل السحابة في الـ APK الموزّع عبر Actions:** أضف `SUPABASE_URL` و`SUPABASE_ANON_KEY` (اختيارياً `GEMINI_API_KEY`) في **GitHub Secrets** — الخطوة جاهزة في الـ workflow.

---

## 10. سجل مختصر (ما وَثّقه المساعدون سابقاً — ليس دليلاً)

- **أغسطس 2026:** تحصين مسار الإنتاج و`StyleBrain` ضد النجاح الوهمي؛ تحرير التركيب والاعتماد على البيانات الحقيقية في لوحة المطور؛ آية الدخول والسبلاش؛ إزالة صور وهمية من الشاشات.
- **سبتمبر 2026:** Gemini ↑ إلى 2.0-flash؛ دمج FFmpeg حقيقي (`mergeVideo` عبر xfade)؛ تعليق صوتي مجاني دائماً (TTS النظام)؛ شاشة مفاتيح كاملة للطبقة المجانية (Azure + ElevenLabs + بطاقة «يعمل بلا مفاتيح»)؛ انتقالات `AnimatedContent` بين الشاشات؛ مكوّنا `QabasCard`/`QabasSectionHeader`؛ حماية `SupabaseConfig` ضد placeholder؛ خطوة `Inject secrets` في CI.
- **سبتمبر 2026 (تنظيف):** حذف سكربتات التصحيح وآثار التفكيك (`jadx_dir/`, `decompiled_apk/`, ملفات patch/scripts) من جذر المشروع؛ إعادة كتابة `README.md` كتوثيق حقيقي للمنتج بدل سجلّ التغييرات.
- **سبتمبر 2026 (بناء + حارس):** تجهيز بيئة بناء محلية كاملة (JDK 17 Temurin + SDK 36 في `~/tooling`) وإثبات `assembleDebug` أخضر محلياً؛ إضافة `QabasCrashGuard` العالمي (التقاط/توثيق/استعادة الانهيارات) وتركيبه في `QabasApplication`.
- **سبتمبر 2026 (شاشة المفاتيح):** حصاد شبه آلي (التقاط/تحقق تلقائي من الحافظة + زرّا «التقاط من الحافظة»/«افتح الموقع»)، شارة «🆓 بديل مجاني» لكل بطاقة، ولوح «فحص صحة المفاتيح الحي 🩺» لـ 7 خدمات — 8 نقاط تحقق ملفوفة بـ `ApiUsageTracker`. **إصلاح:** أزرار الحقل الفارغ خرجت من `trailingIcon` (شكل باركود غير قابل للضغط) إلى صفّ أسفل الحقل، و`ApiKeyValidator` يرفض مبكراً أي مفتاح Gemini لا يبدأ بـ `AIzaSy…` برسالة عربية واضحة (يكشف توكنات `AQ.…`).
- **سبتمبر 2026 (هوية الإنتاج AI):** خلفيات برمجية مولّدة (`ProceduralBackdropEngine`, CI `1c8ba7d`) لمسار الاستوديو بدل الصور الوهمية؛ نظام ألوان بنفسجي مهيمن + إكسنت سيان (`f5bcdec`) مع حفظ الذهبي للمقدسات؛ شاشة دخول بدمج اللوجو الرسمي مع الترحيب (`bd00524`).