# AGENTS.md — تسليم المشروع لأي مساعد يكمل العمل على «قبس» (Qabas)

> اقرأ هذا الملف **كاملاً** قبل أي تعديل.  
> هو المرجع الحاكم مع `README.md`.  
> الهدف: تكمل كأنك نفس المساعد السابق — نفس الأولويات، نفس الممنوعات، نفس الخطوة التالية.

---

## 0. قاعدة الملفين الحاكمين

| الملف | الوظيفة |
|-------|---------|
| **`AGENTS.md`** | الحالة التشغيلية، الخطوة التالية، القواعد، التسليم بين الجلسات |
| **`README.md`** | وصف المنتج، سجل الميزات، تحصين المسار، حالة البناء |

**مع كل عمل جوهري:** حدّث الملفين معاً. بدون ذلك العمل غير مكتمل.

---

## 1. من أنت على هذا المشروع

- لست مساعداً عابراً. تعامل مع قبس كمنتج يجب أن **ينتج فيديو حقيقياً** ويستقر على الجهاز.
- قراراتك لصالح استقرار المسار الطويل، لا لإرضاء طلبات تجميلية قبل الأوان.
- بعد كل قرار: **خطوة تالية واحدة** فقط.
- سلّم الملفات المعدّلة كاملة جاهزة للاستبدال على GitHub.
- صراحة تقنية مباشرة بالعربية.

---

## 2. هوية المنتج (مختصر)

- أندرويد / Kotlin / Jetpack Compose
- Dark Slate + Gold
- إنتاج محتوى إسلامي: فكرة → سكربت → B-Roll → صوت → نص على الفيديو → تصدير MP4
- FFmpeg Kit + Gemini/Groq/TTS حقيقي مع fallbacks
- بناء: GitHub Actions → `Qabas-Studio-Debug-APK`

---

## 3. أين وصلنا (آخر تسليم — أغسطس 2026)

### أ) مسار الإنتاج (محصَّن على مستوى الكود — يحتاج إثبات جهاز)

| ملف | ماذا أُصلح |
|-----|------------|
| `VideoProcessor.kt` | نص عربي `StaticLayout`، timeouts ذكية، `isValidVideoFile` / `hasAudioTrack` |
| `VideoEngineManager.kt` | كاش B-Roll، إطار offline 1080×1920، لوج مدة/صوت |
| `ProcessingScreen.kt` | timeout ديناميكي 180–420ث حسب عدد المشاهد، مؤشر تقدم خطي `LinearProgressIndicator` فاخر، وربط نتائج التصدير بالعقل التراكمي `StyleBrain.recordProductionResult` لتطوير الأداء باستمرار |
| `StyleBrain.kt` | اعتماد وتخزين الأسلوب الأساسي للاستوديو (`primaryStyleId` + `setPrimaryStudioStyle`)، توجيه الإنتاج في `chooseBestStyleForIdea` بالدمج المرجح (70% هوية قبس + 30% أسلوب معتمد)، ودورة التغذية الراجعة التلقائية `recordProductionResult` |
| `StyleBrainSection.kt` | إضافة بادج وزر اعتماد الأسلوب كـ «أساسي للاستوديو 🌟» في بطاقات الأساليب مع التبديل التلقائي والحفظ الدائم |
| `RealServices.kt / AndroidManifest.xml / .env.example` | حل أخطاء التجميع وتصاريح أندرويد الناقصة (VIBRATE)، وتنظيف متغيرات البيئة لبناء التطبيق. |

| `IdeaInputSection.kt` | إصلاح كسر `compileDebugKotlin` (state + أقواس + AlertDialog) |
| `DeveloperDashboardScreen.kt` | إزالة تضارب `DevLog` مع `object DevLog`، وتصحيح `LogsSection`، وإزالة تعريف `AccountSettingsSection` المزدوج |
| `StyleBrain.kt` | تغيير سياسة العقل التراكمي ليصبح **فارغاً ومحايداً تماماً** عند التهيئة، ولا يستخدم أساليب كاذبة. إرجاع `null` للمخرج في حالة العقل الفارغ ليُمرر التوليد بحالة محايدة صريحة، والبدء في بناء العقل التراكمي حصرياً من أول فيديو يتم امتصاصه عبر `evolveCoreStyleWithNewAbsorption` |
| `ProcessingScreen.kt / AppNavigation.kt` | استلام الفكرة والنبرة الحقيقية، منع الانتقال الوهمي للمراجعة دون ملف فيديو محلي حقيقي وصالح، وإظهار خيار "إعادة المحاولة/رجوع" الواضح عند الفشل، وإزالة كل صور `picsum` كخلفيات وهمية. |
| `UnderstandingScreen.kt / RealServices.kt` | إزالة الخطافات العشوائية تماماً حال فشل التحليل، واعتماد مشاهد مشتقة ديناميكياً من الفكرة الفعلية حصراً، وتطبيق تحليل JSON دقيق عبر Gemini بنسبة ثقة صارمة. |
| `StyleBrainSection.kt` | إضافة واجهة مقارنة بصرية متكاملة جنباً إلى جنب مع **رسم بياني راداري (Radar Chart)** مخصص للمطور يوضح توزيع القوة بين الأنماط المختارة (التباين، الإيقاع، عمق الألوان، ديناميكية الحركة، الخطوط) مع مؤشرات الاختلاف والتطابق ومعاينة حية للنمط الهجين قبل الدمج. |
| `StyleBrain.kt / StyleBrainSection.kt / ProcessingScreen.kt` | إضافة **محرك التعلم المستمر (Continuous Learning Engine)**: تحليل دوري وتلقائي لفيديوهات الاستوديو السابقة الناجحة لاستخلاص أنماط النجاح البصري، واقتراح تحسينات دقيقة ومدروسة للأسلوب النشط (الألوان، التباين، الإيقاع، الكابشن اللفظي) مع إمكانية تطبيقها فوراً أو تعديلها. |
| `StyleBrain.kt / StyleBrainCrashGuard.kt / StyleBrainSection.kt` | **تحصين مسار امتصاص واستخراج الأنماط الفنية من الفيديو**: حماية الذاكرة وضمان استخراج 6 إطارات مفتاحية بدقة متساوية مع تحجيم آمن لمنع الانهيار و OOM، التحقق الصارم من وجود مفتاح ذكاء اصطناعي (Gemini/Groq) قبل البدء، منع النجاح الوهمي أو السمات الفارغة، وتطبيق خوارزمية تطور القوة الحقيقية المحدودة بـ +0 إلى +3 لكل امتصاص ناجح دون قفز مفاجئ إلى 80 أو 50. |
| `QabasBrainViewModel.kt / StyleBrainSection.kt` | **تتبع دقيق للمراحل ونسبة مئوية في واجهة التحميل مع تجميد زر الاستخراج**: ربط شريط التقدم بـ `QabasBrainViewModel.absorptionState`، عرض نسبة مئوية لحظية لكل مرحلة من المراحل الثلاث (استخراج الإطارات 0-33%، تحليل الألوان 34-66%، استعلام الذكاء الاصطناعي 67-100%)، وتجميد زر الاستخراج تماماً مع إشعار قفل واضح عند عدم توفر مفتاح API. |
| `StyleBrain.kt / MontageDirector.kt / VideoEngineManager.kt / VideoProcessor.kt / ProcessingScreen.kt` | **تطبيق الأسلوب المختار/الممتص (StyleDirective) فعلياً على ملف الفيديو النهائي**: ربط كامل للتوجيه الفني بحيث ينعكس بصرياً في الفيديو الناتج (الألوان المخصصة والتدرج اللوني، فلاتر LUT السينمائية، حركة الكاميرا والزووم والانتقالات متعددة المشاهد xfade، ونمط وموضع الكابشن) مع منع التخطي الصامت وضمان صحة ملف MP4 الناتج. |
| `StyleBrain.kt / ContinuousLearningEngine` | **تحديث محرك التعلم المستمر وحساب القوة**: استبدال قوالب المقترحات الثابتة في `runContinuousLearningAnalysis` ببناء ديناميكي مرن عبر `ContinuousLearningEngine.buildProposals`. منع أي مقترحات مزيفة (Fake Proposals) إذا كان العقل فارغاً من الأساليب الممتصة (`styles.isEmpty()`) أو عدد المشاريع المكتملة صفراً (`completedCount == 0`). وحصر نمو القوة للـ Core في النطاق الصارم `+0 إلى +2` لمدى `0..100` دون أي قفزة فرضية لـ 50. |
| `StyleBrain.kt / extractKeyFrames` | **إصلاح الانهيار التلقائي (Crash) عند استخراج الإطارات**: إزالة الاعتماد على `FFmpegKit` في مهمة الاستخراج الجزئي للصور (والتي كانت تسبب انهياراً للمحرك الأصلي C++ على بعض الأجهزة)، والاعتماد بشكل كامل وموثوق على `MediaMetadataRetriever` لاستخراج الإطارات بأمان، مع تصحيح دورة حياة الكائن `release()` لضمان الاستقرار. |
| `VideoEngineManager.kt / RealServices.kt` | **إصلاح جلب وتحميل موارد B-Roll و Pexels و Pixabay**: تصحيح شرط `preferGuaranteedPath` لمنع التخطي القسري لتحميل الوسائط على مقاطع 3 مشاهد، وتفعيل دعم مفاتيح Pexels و Pixabay تلقائياً من `BuildConfig`، وضمان جلب وتنزيل مقاطع حقيقية تناسب فكرة ونبرة الفيديو. |
| `CinematicExportScreen.kt / AppNavigation.kt` | **تحسين مسار التصدير وإزالة الريندر المزدوج**: إعادة استخدام ملف الفيديو المنتج مسبقاً في `ProcessingScreen` فوراً في `SaveShareScreen` بدون إعادة معالجة غير ضرورية، والانتقال السلس والمباشر من شاشة المراجعة إلى الحفظ والمشاركة. |
| `MontageDirector.kt / RealServices.kt` | **الخطوة 1: ثورة الاستعارة البصرية**: تم إعادة ربط `MontageDirector` بـ `AppServices.generateScript` ليقوم Gemini فعلياً بهندسة مشاهد تعتمد على الاستعارة البصرية (Visual Metaphors) وتغذية المحرك بها، وإلغاء الاعتماد على القوالب الثابتة إلا في حالة فشل الاتصال. |
| `ProcessingScreen.kt` | **إضافة واجهة تحليل الفشل التلقائي (Automated Error Feedback UI)**: بدلاً من الانهيار الصامت أو رسائل الخطأ المبهمة، تم بناء واجهة ذكية تحلل سجل النشاط ونوع الخطأ (مثل فشل FFmpeg، نقص مساحة، رفض اتصال API، مشكلة في Pexels) وتعرض للمستخدم تفسيراً بشرياً واضحاً مع التوجيه السليم للإصلاح مباشرة في نفس شاشة المعالجة. |
| `RealServices.kt / VideoProcessor.kt` | **ثورة الإخراج الفني الشاملة (The Art Director Update)**: تنفيذ 5 قواعد إخراجية احترافية في الكود: 1. استعارات بصرية للمشاهد بدلاً من الترجمة الحرفية (تعديل Prompt). 2. تفعيل دائم للكابشن الحركي (Word-by-word) في `VideoProcessor`. 3. دمج فلتر (Audio Ducking) عبر `sidechaincompress` في FFmpeg لخفض الصوت المحيطي تلقائياً. 4. التلوين السينمائي النفسي عبر تعيين LUTs أو فلاتر `eq` تلقائياً تعكس المزاج (Dark teal vs Golden). 5. إيقاع تقطيع متغير (Pacing) بمدد مشاهد ديناميكية. |
| `StyleBrain.kt` | **العقل الاستباقي النشط (Gen-2 Proactive Brain)**: تحويل `chooseBestStyleForIdea` ليدمج أسلوبين تلقائياً عبر الذكاء الاصطناعي بدلاً من الاختيار السلبي، وتوليد نمط هجين لحظي مخصص لكل فكرة.

### هـ) لوحة المطور — إزالة كل البيانات المزيفة (أرقام حقيقية فقط)

| ملف | ماذا أُنجز |
|-----|------------|
| `ApiUsageTracker.kt` (جديد) | متتبع استهلاك وزمن استجابة حقيقي لكل خدمة (`ApiStat`) يُحفظ في SharedPreferences. **البداية فارغة تماماً** (`emptyMap()`) — لا أقزام مسبقة، وتمتلئ فقط من استدعاءات HTTP الفعلية. |
| `RealServices.kt` | تغليف **12 نقطة HTTP حقيقية** بـ `ApiUsageTracker.track(...)`: Gemini ×6، Groq، Azure TTS، ElevenLabs، HuggingFace، Pexels، Pixabay. لا توجد قياسات وهمية. |
| `CloudServices.kt / SupabaseServices.kt` | `Database.getAllUsers()` (قائمة المستخدمين الحقيقية من Firestore ثم Supabase fallback) و`getDevUserProjectCount(uid)` (عدّ مشاريع حقيقية من `users/{uid}/projects`). |
| `DeveloperDashboardScreen.kt` | **إزالة كل البيانات المختلقة**: قائمة المستخدمين تُحمَّل فعلياً (لا مقالات مزيفة)، إزالة أسماء أعضاء وهميين، تحويل `RevenueSection` لتحميل حقيقي عبر `observeAllTransactions().first()`، إعادة كتابة `ApiConsumptionChart` و`SystemPerformanceHealthKpiGrid` بقياسات حقيقية من `ApiUsageTracker.snapshot`، العناوين/أية خطوات نقل محنكة. |
| `DeveloperDashboardScreen.kt` — `WeeklyEngagementTrendsChart` | **استبدال بيانات أسبوعية مختلقة بأرقام حقيقية من قاعدة البيانات المحلية**: عدّ يومي حقيقي لجدول `projects` (فيديوهات 🎬)، `reel_scripts` (نصوص AI ✍️)، `hadith_cards` (أحاديث 🎴) عبر DAO الجديدة `countXBetween(start,end)`. |
| `DeveloperDashboardScreen.kt` — شاشات الزيارة | استبدال أرقام "الزيارات" المختلقة (84/67/52...) برسالة صادقة "لا تتوفر بيانات زيارة لكل شاشة بعد" مع مؤشرات فارغة في انتظار التتبع الفعلي. |
| `DeveloperDashboardScreen.kt` — `formatRegDate` | معالجة `createdAt` الحقيقي بألوان/صيغ ISO عبر `SimpleDateFormat` (بدون `java.time` لأن `minSdk=24` بلا desugaring) مع fallback رقمية آمن. |

### ب) آية الدخول + السبلاش + أنيميشن دخول الاستوديو (Lottie Entry)

| ملف | الحالة |
|-----|--------|
| `assets/audio/entry_ayah_ruj3a.m4a` | «إن إلى ربك الرجعى» ~7ث |
| `AudioPlayerManager.kt` | تشغيل + **fade in ~0.9ث** + **fade out ~1.1ث** + كتم |
| `SplashScreen.kt` | شاشة السبلاش بهوية قبس الرسمية الكاملة (الشعلة فوق الكتاب + قبس + الآية والعبارة) مع أنيميشن خفيف (Fade + Scale + Glow) بدون صوت ودعم التخطي ومهلة أمان |
| `QabasLottieAnimations.kt` | بوابة الاستوديو الذهبية `STUDIO_ENTRY_PORTAL_JSON` وموجة المايك `STUDIO_MIC_WAVE_JSON` |
| `QabasHomeScreen.kt` | تفاعل دخول ناعم (`scale` + `alpha` مع `spring()`)، بوابة استوديو Lottie، وترتيب الأقسام (المشاريع بعد الـ Hero مباشرة مع `defaultExpanded = true` والأقسام المساعدة مطوية افتراضياً) |

### ج) البناء

- تم تصحيح أخطاء التجميع وتضارب الأسماء في `DeveloperDashboardScreen` وبناء التطبيق بنجاح (`compile_applet` Build succeeded).
- APK Debug يُبنى عبر Actions ويُنزَّل من Artifacts.

### د) ما لم يُغلق

- إثبات التصدير على **جهاز حقيقي** (تم تفعيل العقل الاستباقي Proactive Brain بطلب مباشر من المستخدم قبل الإثبات).
- أصول B-Roll فيديو داخل `assets` (حالياً كاش + إطار صناعي).
- فرض مسار صوت في الملف النهائي عند نجاح TTS.

### هـ) تحسينات الطبقة المجانية (سبتمبر 2026)

| ملف | ماذا أُنجز |
|-----|------------|
| `RealServices.kt` / `StyleBrain.kt` / `QabasBrainViewModel.kt` | ترقية نموذج Gemini `gemini-1.5-flash` → `gemini-2.0-flash` في كل نقاط الاتصال (8 مواضع) — أحدث وأفضل ومجاني. |
| `RealServices.kt` — `RealFFmpegService.mergeVideo` | استبدال النموذج الأولي (no-op `delay(2000)`) بدمج حقيقي عبر `VideoProcessor.concatenateVideosWithTransitions` (xfade) مع فحص صلاحية MP4 وسجل واضح — لا عودة كاذبة. |
| `RealServices.kt` — `AndroidTTSService` (جديد) | محرك النطق المدمج في أندرويد `TextToSpeech` (بلا مفتاح API، يعمل دون إنترنت) كـ fallback أخير في `generateVoiceover` — الفيديو لا يُترك بلا صوت عند غياب المفاتيح، دون حقن تلاوة خاطئة. |
| `.env.example` | توثيق الطبقات المجانية لكل خدمة مع الروابط. **تحذير:** قيم placeholder يجب أن تبقى غير فارغة (`"your_key"`) وإلا انكسر `BuildConfig.java` (خطأ `illegal start of expression`). |
| CI | تشغيل كامل أخضر بعد إصلاحين (capture TTS + قيم `.env.example`). |
| `ApiKeysScreen.kt` / `ApiKeyValidator.kt` | إكمال خيارات المفاتيح المجانية: إضافة بطاقتي **Azure Speech (TTS)** (مع حقل المنطقة Region) و**ElevenLabs (TTS)** — وكلاهما يُقرآن فعلياً في `generateVoiceover` وكانا غائبين عن الواجهة — مع فحص اتصال حقيقي لكل منهما في `validateKey`، وإدراج المفتاحين والمنطقة في تصدير/استيراد النسخ الاحتياطي JSON، وإضافة بطاقة افتتاحية «**يعمل مجاناً دون أي مفتاح 🎉**» توضح أن التحليل المحلي + محرك النطق المدمج + FFmpeg تعمل بلا مفاتيح وبدون إنترنت، وأن بقية المفاتيح اختيارية. |

### و) تحسينات UI (سبتمبر 2026)

| ملف | ماذا أُنجز |
|-----|------------|
| `AppNavigation.kt` | انتقال أنيميشن بين الشاشات: لفّ `when (state.appState)` داخل `AnimatedContent` بانتقال `slide (1/4 عرض) + fade` 240ms (Compose BOM 2024.09.00 → stable، بلا OptIn) — نقلة سينمائية بدل التبديل الفاصل. |
| `ApiKeyValidator.kt` / `ApiKeysScreen.kt` | تغطية تحقق مفتاحي Azure TTS وElevenLabs بـ `ApiUsageTracker.track(context, "Azure TTS"|"ElevenLabs")` — أصبحت قياساتهما الحقيقية تظهر في لوحة المطور (كانتا النقطتين الوحيدتين خارج العدّاد). إمضاء `validateKey` الجديد: `(context, serviceType, key, hint = null)`. |
| `QabasStudioSharedComponents.kt` (جديد) | مكوّنا `QabasCard` (حدود ذهبية متدرّجة + توهج + `luxuryCardStyle`) و`QabasSectionHeader` (رأس قسم موحّد مع ترايل اختياري) — إعادة استخدام للمظهر الفاخر. |
| `ProjectsScreen.kt` | اعتماد `QabasCard` لبطاقات المشاريع (يبقى `clickable` على البطاقة) — فض 13 سطراً من التكرار. |

### ز) دروس مستفادة (أخطاء حُلت في CI)

1. **`TextToSpeech` capture:** لا تَستدعِ `tts.method()` داخل لامدا `TextToSpeech(context){...}` عبر الثابت الخارجي — استخدم `var tts: TextToSpeech? = null` ثم `val instance = tts ?: return@TextToSpeech` (اللامدا تُنفَّذ أثناء الإنشاء قبل اكتمال التخصيص).
2. **`.env.example`:** القيمة الفارغة (`KEY=""`) تُنتج `BuildConfig` معطوباً (`String KEY = ;`). أبقِ دائماً قيمة placeholder غير فارغة.

---

## 4. الخطوة التالية الوحيدة الآن

**يعتمد على ما يبلّغ به المستخدم بعد التجربة:**

### إن لم يُختبر مسار التصدير بعد
→ ثبّت APK → فكرة عربية قصيرة → معالجة → تصدير → تشغيل الملف.  
تحقق: كابشن عربي، مدة، تشغيل بدون نت / بدون TTS إن أمكن.

### إن نجح التصدير
→ لا ميزات عشوائية. إصلاح أي عيب لاحظه المستخدم، أو تقوية offline، ثم حدّث الملفين.

### إن فشل شيء
→ أصلح **ذلك الفشل فقط** (سبلاش / FFmpeg / كابشن / صوت). لا تفتح جبهات جديدة.

### طلبات هوية (آية، أنيميشن)
→ مقبولة إذا قصيرة وغير حاجبة؛ لا تؤخّر إصلاح المسار.

---

## 5. ممنوعات صارمة

1. لا هدم مسار FFmpeg أو التصدير من أجل UI.
2. لا `return true` بعد فشل صامت.
3. لا حقن محتوى ديني خاطئ كـ fallback للصوت/النص.
4. لا ميزة جديدة فوق مسار تصدير غير مُثبت على الجهاز.
5. لا تترك `AGENTS.md` / `README.md` بخطوة تالية قديمة بعد إنجاز عمل.

---

## 6. كيف تكمل «كأنك نفس المساعد»

1. اقرأ `AGENTS.md` ثم قسم **64–66** في `README.md`.
2. اسأل المستخدم (إن لزم): هل نجح التصدير على الجهاز؟ ماذا رأيت بالضبط؟
3. نفّذ **خطوة واحدة** حسب القسم 4.
4. عدّل الملفات الكاملة → سلّمها للتنزيل.
5. حدّث `AGENTS.md` (أين وصلنا + الخطوة التالية) و`README.md` (سجل مختصر).

---

## 7. ملفات حساسة للمسار (لا تلمسها دون قراءة)

```
app/src/main/java/com/example/VideoProcessor.kt
app/src/main/java/com/example/VideoEngineManager.kt
app/src/main/java/com/example/ProcessingScreen.kt
app/src/main/java/com/example/RealServices.kt
app/src/main/java/com/example/ui/input/IdeaInputSection.kt
app/src/main/java/com/example/AudioPlayerManager.kt
app/src/main/java/com/example/SplashScreen.kt
app/src/main/assets/audio/entry_ayah_ruj3a.m4a
```

---

## 8. ملخص سطر واحد للجلسة القادمة

> المسار محصَّن في الكود؛ آية الدخول والسبلاش جاهزان؛ لوحة المطور خالية من البيانات المزيفة (مستخدمون/إيرادات/استهلاك API/عدّ أسبوعي حقيقي).  
> **الأولوية:** نتيجة اختبار التصدير على الجهاز → ثم إصلاح أو تقوية offline فقط.  
> لوحة المطور: 14 نقطة HTTP ملفوفة بـ `ApiUsageTracker` (انضمّت Azure TTS وElevenLabs من شاشة المفاتيح)، وتحميل فعلي من Firestore/Supabase/Room.  
> **سبتمبر 2026:** Gemini ↑ 2.0-flash، دمج FFmpeg حقيقي، تعليق صوتي مجاني دائماً (TTS النظام)، توثيق مجانيّ في `.env.example`، CI أخضر.  
> شاشة المفاتيح اكتملت لكل الطبقة المجانية (Azure + ElevenLabs وبطاقة «يعمل بلا مفاتيح»).  
> **UI سبتمبر 2026:** انتقال أنيميشن بين الشاشات (`AnimatedContent` slide+fade)، مكوّنا `QabasCard`/`QabasSectionHeader` مع اعتمادها في المشاريع، وتغطية تحقق المفاتيح بالعداد — CI أخضر للدفعة (`7542c0e`).  
> وثّق كل حركة في `AGENTS.md` + `README.md`.
