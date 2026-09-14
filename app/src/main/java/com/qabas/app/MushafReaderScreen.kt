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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    val isBookmarked = currentPage in bookmarks

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
                onOpenPicker = { showPicker = true }
            )
        }

        if (showPicker) {
            MushafPickerDialog(
                currentPage = currentPage,
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
private fun MushafTopBar(surahNames: List<String>, juz: Int, onClose: () -> Unit) {
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
                if (juz > 0) {
                    Text(
                        "الجزء ${easternDigits(juz)}",
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
    onOpenPicker: () -> Unit
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

@Composable
private fun MushafPickerDialog(
    currentPage: Int,
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
                            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                                items(QuranDataProvider.surahs) { surah ->
                                    val page = remember { MushafPageData.getPageForSurah(surah.id) }
                                    Row(
                                        Modifier.fillMaxWidth().clickable { if (page > 0) onGoToPage(page) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            surah.name, color = Color.White, fontFamily = CairoFont,
                                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            if (page > 0) "ص ${easternDigits(page)}" else "—",
                                            color = GoldSecondary, fontFamily = CairoFont, fontSize = 12.sp
                                        )
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
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
