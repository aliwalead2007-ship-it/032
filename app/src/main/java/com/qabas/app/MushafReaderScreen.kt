package com.qabas.app

/**
 * MushafReaderScreen.kt
 *
 * قارئ المصحف بالصفحات — تصميم أصلي لقبس (لا كود منقول من أي مشروع).
 * - واجهة عربية RTL، صفحة في المنتصف، سحب أفقي بين 604 صفحات (مصحف المدينة).
 * - شريط علوي: السورة + الجزء. شريط سفلي: رقم الصفحة + تنقل + علامة + اختيار.
 * - نافذة اختيار: سورة / جزء / صفحة / علاماتي.
 * - حفظ آخر صفحة + العلامات في "qabas_prefs" (نفس تخزين التطبيق).
 * - النصوص من `uthmani.json` المحلي حصراً — لا شبكة ولا مفاتيح.
 * - هيكل مفتوح لاحقاً: onOpenTafseer / onPlayAudio (اختياريان، يُمرَّران عند الجاهزية).
 */

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFS = "qabas_prefs"
private const val KEY_LAST_PAGE = "last_read_page"
private const val KEY_BOOKMARKS = "mushaf_bookmarks"

private fun easternDigits(n: Int): String {
    val eastern = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return n.toString().map { c -> if (c in '0'..'9') eastern[c - '0'] else c }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafReaderScreen(
    onClose: () -> Unit,
    onOpenTafseer: (surahId: Int, ayah: Int) -> Unit = { _, _ -> },
    initialPage: Int? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var dataReady by remember { mutableStateOf(false) }
    var bookmarks by remember {
        mutableStateOf(prefs.getStringSet(KEY_BOOKMARKS, emptySet())?.mapNotNull { it.toIntOrNull() }?.sorted() ?: emptyList())
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            QuranDataProvider.loadFromAssets(context)
            QuranDataProvider.loadTafsirFromAssets(context)
            MushafPageData.loadPageMap(context)
        }
        dataReady = MushafPageData.isLoaded()
        if (!dataReady) {
            Toast.makeText(context, "تعذر تحميل خريطة الصفحات — تحقق من ملف madani_pages.json", Toast.LENGTH_LONG).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!dataReady) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B0F19)), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onPageNumberClick)
            ) {
                    CircularProgressIndicator(color = GoldPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text("جاري تجهيز صفحات المصحف…", color = TextSecondary, fontFamily = CairoFont)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onClose) { Text("رجوع", color = GoldPrimary, fontFamily = CairoFont) }
                }
            }
        } else {
            MushafReaderContent(
                startPage = (initialPage ?: prefs.getInt(KEY_LAST_PAGE, 1)).coerceIn(1, MushafPageData.PAGE_COUNT),
                bookmarks = bookmarks,
                onBookmarksChange = { updated ->
                    bookmarks = updated
                    prefs.edit().putStringSet(KEY_BOOKMARKS, updated.map { it.toString() }.toSet()).apply()
                },
                onSavePage = { page -> prefs.edit().putInt(KEY_LAST_PAGE, page).apply() },
                onClose = {
                    onClose()
                },
                onOpenTafseer = onOpenTafseer
            )
        }
    }
}

@Composable
private fun MushafReaderContent(
    startPage: Int,
    bookmarks: List<Int>,
    onBookmarksChange: (List<Int>) -> Unit,
    onSavePage: (Int) -> Unit,
    onClose: () -> Unit,
    onOpenTafseer: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = startPage - 1, pageCount = { MushafPageData.PAGE_COUNT })
    var showPicker by remember { mutableStateOf(false) }

    // حفظ آخر صفحة عند كل تنقل
    LaunchedEffect(pagerState.currentPage) {
        onSavePage(pagerState.currentPage + 1)
    }

    val currentPage = pagerState.currentPage + 1
    val surahsOnPage = remember(currentPage) { MushafPageData.getSurahsOnPage(currentPage) }
    val juz = remember(currentPage) { MushafPageData.getJuzForPage(currentPage) }
    val hizb = remember(currentPage) { MushafPageData.getHizbForPage(currentPage) }
    val isBookmarked = currentPage in bookmarks
    var showSheet by remember { mutableStateOf(false) }

    fun toggleBookmark() {
        val updated = if (isBookmarked) bookmarks - currentPage else (bookmarks + currentPage).sorted()
        onBookmarksChange(updated)
        Toast.makeText(
                context,
                if (isBookmarked) "أُزيلت العلامة من صفحة $currentPage 🔖" else "حُفظت صفحة $currentPage في علاماتك 🔖",
                Toast.LENGTH_SHORT
            ).show()
        }

        Column(Modifier.fillMaxSize().background(Color(0xFF03060C))) {
            // ── الشريط العلوي: السورة + الجزء ──
            MushafTopBar(
                surahNames = surahsOnPage.map { QuranDataProvider.surahNameOf(it) },
                juz = juz,
                hizb = hizb,
                onClose = {
                    onSavePage(currentPage)
                    onClose()
                }
            )
            // ── صفحة المصحف (سحب أفقي) ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { index ->
                MushafPageCard(page = index + 1, onOpenTafseer = onOpenTafseer)
            }
            // ── الشريط السفلي: رقم الصفحة + التحكم ──
            MushafBottomBar(
                page = currentPage,
                isBookmarked = isBookmarked,
                onPrev = { scope.launch { pagerState.animateScrollToPage((currentPage - 2).coerceAtLeast(0)) } },
                onNext = { scope.launch { pagerState.animateScrollToPage((currentPage).coerceAtMost(MushafPageData.PAGE_COUNT - 1)) } },
                onToggleBookmark = ::toggleBookmark,
                onOpenPicker = { showPicker = true },
                onPageNumberClick = { showSheet = true }
            )
        }

        if (showSheet) {
            MushafPageSheet(
                currentPage = currentPage,
                surahName = surahsOnPage.firstOrNull()?.let { QuranDataProvider.surahNameOf(it) } ?: "المصحف الشريف",
                hizb = hizb,
                onGoToPage = { page ->
                    showSheet = false
                    scope.launch { pagerState.scrollToPage((page - 1).coerceIn(0, MushafPageData.PAGE_COUNT - 1)) }
                },
                onDismiss = { showSheet = false }
            )
        }

        if (showPicker) {
            MushafPickerDialog(
                currentPage = currentPage,
                lastPage = currentPage,
                bookmarks = bookmarks,
                onGoToPage = { page ->
                    showPicker = false
                    scope.launch { pagerState.scrollToPage((page - 1).coerceIn(0, MushafPageData.PAGE_COUNT - 1)) }
                },
                onRemoveBookmark = { page ->
                    onBookmarksChange(bookmarks - page)
                },
                onDismiss = { showPicker = false }
            )
        }
}

@Composable
private fun MushafTopBar(surahNames: List<String>, juz: Int, hizb: Int, onClose: () -> Unit) {
    Surface(color = Color(0xFF0B1120), tonalElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = GoldPrimary)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    surahNames.joinToString(" · ").ifBlank { "المصحف الشريف" },
                    color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    textAlign = TextAlign.Center, maxLines = 1
                )
                if (juz > 0 || hizb > 0) {
                    Text(
                        listOf(
                            if (juz > 0) "الجزء ${easternDigits(juz)}" else null,
                            if (hizb > 0) "الحزب ${easternDigits(hizb)}" else null
                        ).filterNotNull().joinToString(" • "),
                        color = GoldSecondary, fontFamily = CairoFont, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun MushafPageCard(page: Int, onOpenTafseer: (Int, Int) -> Unit) {
    val refs = remember(page) { MushafPageData.getPageRefs(page) }
    val scroll = rememberScrollState()
    Box(
        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFEF7))
            .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        if (refs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا تتوفر بيانات هذه الصفحة", color = Color.Gray, fontFamily = CairoFont)
            }
            return@Box
        }
        val pageText = remember(page) {
            buildAnnotatedString {
                var lastSurah = -1
                for ((s, a) in refs) {
                    if (a == 1 && s != lastSurah) {
                        // رأس سورة جديدة تبدأ في هذه الصفحة
                        pushStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold
                            )
                        )
                        append("\nسورة ${QuranDataProvider.surahNameOf(s)}\n")
                        pop()
                        if (s != 9) {
                            append("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ\n")
                        }
                        lastSurah = s
                    }
                    val text = QuranDataProvider.getVerseText(s, a) ?: ""
                    append(text)
                    append(" ﴿${easternDigits(a)}﴾ ")
                }
            }
        }
        Text(
            text = pageText,
            modifier = Modifier.fillMaxSize().verticalScroll(scroll),
            fontFamily = AmiriFont,
            fontSize = 21.sp,
            lineHeight = 40.sp,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Justify
        )
    }
}

@Composable
private fun MushafBottomBar(
    page: Int,
    isBookmarked: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenPicker: () -> Unit,
    onPageNumberClick: () -> Unit
) {
    Surface(color = Color(0xFF0B1120), tonalElevation = 4.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "السابق", tint = GoldPrimary)
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "علامة مرجعية",
                    tint = if (isBookmarked) GoldPrimary else Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "صفحة ${easternDigits(page)}",
                    color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Text(
                    "من ${easternDigits(MushafPageData.PAGE_COUNT)}",
                    color = TextSecondary, fontFamily = CairoFont, fontSize = 11.sp
                )
            }
            IconButton(onClick = onOpenPicker) {
                Icon(Icons.Default.List, contentDescription = "اختيار سورة / جزء / صفحة", tint = GoldPrimary)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "التالي", tint = GoldPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MushafPageSheet(
    currentPage: Int,
    surahName: String,
    hizb: Int,
    onGoToPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sliderValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF151B2B)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (hizb > 0) "الحزب ${easternDigits(hizb)}" else "",
                    color = TextSecondary, fontFamily = CairoFont, fontSize = 13.sp
                )
                Text(
                    surahName,
                    color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "صفحة ${easternDigits(currentPage)}",
                color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 30.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onGoToPage(sliderValue.toInt().coerceIn(1, MushafPageData.PAGE_COUNT)) },
                valueRange = 1f..MushafPageData.PAGE_COUNT.toFloat(),
                steps = MushafPageData.PAGE_COUNT - 2,
                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ((currentPage - 3)..(currentPage + 3))
                    .filter { it in 1..MushafPageData.PAGE_COUNT }
                    .forEach { p ->
                        val selected = p == currentPage
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (selected) GoldPrimary else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (selected) GoldPrimary else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onGoToPage(p) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                easternDigits(p),
                                color = if (selected) Color.Black else TextSecondary,
                                fontFamily = CairoFont,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (selected) 18.sp else 14.sp
                            )
                        }
                    }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MushafPickerDialog(
    currentPage: Int,
    lastPage: Int,
    bookmarks: List<Int>,
    onGoToPage: (Int) -> Unit,
    onRemoveBookmark: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("سورة", "جزء", "صفحة", "🔖 علاماتي")
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().heightIn(max = 520.dp),
            shape = RoundedCornerShape(20.dp), color = Color(0xFF151B2B)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "الانتقال في المصحف",
                    color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold,
                    fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = tab == i, onClick = { tab = i },
                            text = { Text(title, fontFamily = CairoFont, fontSize = 12.sp, color = if (tab == i) GoldPrimary else TextSecondary) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f, fill = false)) {
                    when (tab) {
                        0 -> {
                            Column(Modifier.heightIn(max = 420.dp)) {
                                // بطاقة فاصل الصفحة: آخر موضع قراءة
                                val lastRefs = remember(lastPage) { MushafPageData.getPageRefs(lastPage) }
                                if (lastRefs.isNotEmpty()) {
                                    val (ls, la) = lastRefs.first()
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF0B1120))
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                            .clickable { onGoToPage(lastPage) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                            Text(
                                                "فاصل الصفحة",
                                                color = Color.White, fontFamily = CairoFont,
                                                fontWeight = FontWeight.Bold, fontSize = 15.sp
                                            )
                                            Text(
                                                "${QuranDataProvider.surahNameOf(ls)} - الآية ${easternDigits(la)} - صفحة ${easternDigits(lastPage)}",
                                                color = GoldSecondary, fontFamily = CairoFont, fontSize = 12.sp
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text("➤", color = GoldPrimary, fontSize = 20.sp)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                }
                                LazyColumn(Modifier.weight(1f, fill = false)) {
                                    items(QuranDataProvider.surahs) { surah ->
                                        val page = remember { MushafPageData.getPageForSurah(surah.id) }
                                        val selected = surah.id in MushafPageData.getSurahsOnPage(currentPage)
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (selected) Color(0xFF6B5A1E) else Color.Transparent)
                                                .clickable { if (page > 0) onGoToPage(page) }
                                                .padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(GoldPrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    easternDigits(page.takeIf { it > 0 } ?: 0),
                                                    color = GoldPrimary, fontFamily = CairoFont,
                                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                Text(
                                                    surah.name, color = Color.White, fontFamily = CairoFont,
                                                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                                                )
                                                Text(
                                                    "آياتها ${easternDigits(surah.versesCount)} - ${surah.type}",
                                                    color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Box(
                                                Modifier.size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(GoldPrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    easternDigits(surah.id),
                                                    color = GoldPrimary, fontFamily = CairoFont,
                                                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5), Modifier.heightIn(max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items((1..MushafPageData.JUZ_COUNT).toList()) { juz ->
                                    Box(
                                        Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF0B1120))
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                val page = MushafPageData.getPageForJuz(juz)
                                                if (page > 0) onGoToPage(page)
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${easternDigits(juz)}", color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        2 -> {
                            var input by remember { mutableStateOf(currentPage.toString()) }
                            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                OutlinedTextField(
                                    value = input,
                                    onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 3) input = v },
                                    label = { Text("رقم الصفحة (١–٦٠٤)", fontFamily = CairoFont) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPad),
                                    singleLine = true, modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val p = input.toIntOrNull()
                                        if (p != null && p in 1..MushafPageData.PAGE_COUNT) onGoToPage(p)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                                ) {
                                    Text("انتقال", fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        3 -> {
                            if (bookmarks.isEmpty()) {
                                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("لا علامات بعد — استخدم 🔖 أسفل أي صفحة لحفظها", color = TextSecondary, fontFamily = CairoFont, textAlign = TextAlign.Center)
                                }
                            } else {
                                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                                    items(bookmarks) { page ->
                                        Row(
                                            Modifier.fillMaxWidth().clickable { onGoToPage(page) }
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "صفحة ${easternDigits(page)}",
                                                color = Color.White, fontFamily = CairoFont, modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { onRemoveBookmark(page) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
                                            }
                                        }
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("إغلاق", color = GoldPrimary, fontFamily = CairoFont)
                }
            }
        }
    }
}
