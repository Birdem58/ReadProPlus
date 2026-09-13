package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.pdf.PdfDocument
import com.example.readproplus.model.tts.TtsVoice
import com.example.readproplus.ui.theme.ReadProPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAudiobookDialog(
    availableBooks: List<PdfDocument>,
    preselectedBook: PdfDocument? = null,
    initialStartPage: Int = 1,
    initialVoice: TtsVoice = TtsVoice.PIPER_DFKI,
    onDismiss: () -> Unit,
    onConfirm: (book: PdfDocument, voice: TtsVoice, startPage: Int, endPage: Int, mainTextOnly: Boolean) -> Unit,
) {
    var selectedBook by remember { mutableStateOf(preselectedBook ?: availableBooks.firstOrNull()) }
    val totalPages = selectedBook?.totalPages?.coerceAtLeast(1) ?: 1
    var startPage by remember(selectedBook, initialStartPage) {
        mutableIntStateOf(initialStartPage.coerceIn(1, totalPages))
    }
    var endPage by remember(selectedBook) { mutableIntStateOf(totalPages) }
    var selectedVoice by remember(initialVoice) { mutableStateOf(initialVoice) }
    var mainTextOnly by remember { mutableStateOf(true) }
    var bookMenuExpanded by remember { mutableStateOf(false) }

    val voices = remember {
        listOf(
            TtsVoice.PIPER_DFKI,
            TtsVoice.PIPER_FAHRRETTIN,
            TtsVoice.PIPER_FETTAH,
            TtsVoice.NICOLE,
        ) + TtsVoice.ALL.filter { it.displayName in setOf("Heart", "Michael", "Sarah") }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ReadProPalette.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = ReadProPalette.primarySoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = ReadProPalette.primaryDeep,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sesli kitap oluştur",
                        color = ReadProPalette.ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "İlk %5 hazır olduğunda dinlemeye başla",
                        color = ReadProPalette.muted,
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = ReadProPalette.muted)
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Kitap")
            if (preselectedBook == null) {
                Box {
                    SelectionCard(
                        title = selectedBook?.title ?: "Bir kitap seç",
                        subtitle = selectedBook?.let { "${it.totalPages} sayfa" } ?: "Kitaplığından bir dosya seç",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = { bookMenuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = bookMenuExpanded,
                        onDismissRequest = { bookMenuExpanded = false },
                    ) {
                        availableBooks.forEach { book ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${book.totalPages} sayfa", fontSize = 11.sp, color = ReadProPalette.muted)
                                    }
                                },
                                onClick = {
                                    selectedBook = book
                                    startPage = 1
                                    endPage = book.totalPages.coerceAtLeast(1)
                                    bookMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            } else {
                SelectionCard(
                    title = preselectedBook.title,
                    subtitle = "${preselectedBook.totalPages} sayfa • okuyucudan seçildi",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Seslendirmen")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voices.forEach { voice ->
                    val isSelected = selectedVoice.id == voice.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedVoice = voice },
                        color = if (isSelected) ReadProPalette.primarySoft else ReadProPalette.surface,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = if (isSelected) ReadProPalette.primary else ReadProPalette.surfaceMuted,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(34.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else ReadProPalette.muted,
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (voice.piperModelFileName != null) "🇹🇷 ${voice.displayName}" else "🇺🇸 ${voice.displayName}",
                                    color = if (isSelected) ReadProPalette.primaryDeep else ReadProPalette.ink,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                                Text(
                                    text = if (voice.piperModelFileName != null) "Türkçe • ${voice.accent} (${voice.gender})" else "English • ${voice.accent} (${voice.gender})",
                                    color = ReadProPalette.muted,
                                    fontSize = 11.sp,
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Seçildi", tint = ReadProPalette.primary)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Seslendirilecek bölüm")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageField(
                    value = startPage,
                    label = "Başlangıç",
                    modifier = Modifier.weight(1f),
                    onValueChange = { startPage = it.coerceIn(1, endPage.coerceAtLeast(1)) },
                )
                Text("—", color = ReadProPalette.muted, fontWeight = FontWeight.Bold)
                PageField(
                    value = endPage,
                    label = "Bitiş",
                    modifier = Modifier.weight(1f),
                    onValueChange = { endPage = it.coerceIn(startPage.coerceAtLeast(1), totalPages) },
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { mainTextOnly = !mainTextOnly }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = mainTextOnly,
                    onCheckedChange = { mainTextOnly = it },
                    colors = CheckboxDefaults.colors(checkedColor = ReadProPalette.primary),
                )
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("Yalnızca ana metin", color = ReadProPalette.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Başlık ve dipnotları temizle", color = ReadProPalette.muted, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ReadProPalette.warm,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("✦", color = ReadProPalette.warmStrong, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Seslendirme arka planda sürer. Her %5'lik checkpoint diske kaydedilir; uygulamayı kapatsan da kaldığın yerden devam eder.",
                        color = ReadProPalette.warning,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    selectedBook?.let { book ->
                        onConfirm(book, selectedVoice, startPage, endPage, mainTextOnly)
                    }
                },
                enabled = selectedBook != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReadProPalette.primary,
                    contentColor = Color.White,
                    disabledContainerColor = ReadProPalette.surfaceMuted,
                    disabledContentColor = ReadProPalette.muted,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Seslendirmeyi başlat", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Vazgeç", color = ReadProPalette.muted)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = ReadProPalette.ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = ReadProPalette.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = ReadProPalette.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = ReadProPalette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = ReadProPalette.muted, fontSize = 11.sp)
            }
            if (onClick != null) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Kitap seç", tint = ReadProPalette.muted)
            }
        }
    }
}

@Composable
private fun PageField(
    value: Int,
    label: String,
    modifier: Modifier,
    onValueChange: (Int) -> Unit,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { input -> input.toIntOrNull()?.let(onValueChange) },
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ReadProPalette.primary,
            focusedLabelColor = ReadProPalette.primary,
            unfocusedBorderColor = ReadProPalette.outline,
            focusedTextColor = ReadProPalette.ink,
            unfocusedTextColor = ReadProPalette.ink,
            cursorColor = ReadProPalette.primary,
        ),
    )
}
