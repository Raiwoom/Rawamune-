package p2p.messenger.hardcore.ui

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import p2p.messenger.hardcore.models.Contact
import p2p.messenger.hardcore.models.MessageContentType
import java.util.Calendar

enum class ChatType { PRIVATE_DIRECT, GROUP_CHAT, CHANNEL_POST }

// 1. КОНТЕЙНЕР ФОНОВЫХ ТЕМ (Белая карандашная и Черная киберпанк)
@Composable
fun ChatBackgroundContainer(
    isDarkTheme: Boolean,
    customBgUri: String?,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isDarkTheme && customBgUri == null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color(09762FB))
                val strokeWidth = 1.dp.toPx()
                val lineSpacing = 16.dp.toPx()
                val pencilColor = Color(0xFF0E0)
                var xOffset = -size.height
                while (xOffset < size.width) {
                    drawLine(
                        color = pencilColor,
                        start = androidx.compose.ui.geometry.Offset(xOffset, 0f),
                        end = androidx.compose.ui.geometry.Offset(xOffset + size.height, size.height),
                        strokeWidth = strokeWidth
                    )
                    xOffset += lineSpacing
                }
            }
        } else if (isDarkTheme && customBgUri == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)))
        }
        Box(Modifier.fillMaxSize(), content = content)
    }
}

// 2. АНИМИРОВАННАЯ МАСКА ВЫРЕЗА АВАТАРКИ ПОД ЗЕЛЕНУЮ ДУГУ
fun createAnimatedCutoutShape(animationProgress: Float) = GenericShape { size, _ ->
    if (animationProgress > 0f) {
        val path = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            val cutoutPath = Path().apply {
                moveTo(size.width / 2, size.height / 2)
                val offset = 20f * animationProgress
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(-offset, -offset, size.width + offset, size.height + offset),
                    startAngleDegrees = -70f,
                    sweepAngleDegrees = 80f,
                    forceMoveTo = false
                )
                close()
            }
            op(this, cutoutPath, androidx.compose.ui.graphics.PathOperation.Difference)
        }
        addPath(path)
    } else {
        addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
    }
}

// 3. СТРОКА СЛИЯНИЯ ПОИСКА С КРУГЛОЙ КНОПКОЙ И ТРЕУГОЛЬНЫМ МЕНЮ ЮИ
@Composable
fun HardcoreSearchBarWithMenu(
    onSearch: (String) -> Unit,
    onCreateContactClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreateChannelClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; onSearch(it) },
            placeholder = { Text("поиск контактов, каналов, групп.", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Cyan) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray, focusedContainerColor = Color(0xFF1E1E1E))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(contentAlignment = Alignment.TopCenter) {
            FloatingActionButton(onClick = { isMenuExpanded = !isMenuExpanded }, shape = CircleShape, containerColor = Color.Cyan, contentColor = Color.Black, modifier = Modifier.size(48.dp)) {
                Icon(if (isMenuExpanded) Icons.Default.Close else Icons.Default.Add, null)
            }
            AnimatedVisibility(visible = isMenuExpanded, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(), modifier = Modifier.padding(top = 56.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)).padding(8.dp)) {
                    IconButton(onClick = { onCreateContactClick(); isMenuExpanded = false }) { Icon(Icons.Default.Person, null, tint = Color.White) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { onCreateGroupClick(); isMenuExpanded = false }) { Icon(Icons.Default.AccountBox, null, tint = Color.White) }
                        IconButton(onClick = { onCreateChannelClick(); isMenuExpanded = false }) { Icon(Icons.Default.Share, null, tint = Color.White) }
                    }
                }
            }
        }
    }
}

// 4. ДЕТАЛЬНОЕ ВВОДНОЕ ПОЛЕ С ЖЕЛТОЙ ОШИБКОЙ РКН И КРЕСТИКОМ
@Composable
fun UniqueIdInputField(idPrefix: String, currentValue: String, isIdAlreadyTaken: Boolean, onValueChange: (String) -> Unit) {
    var rawInput by remember { mutableStateOf(currentValue.removePrefix(idPrefix)) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = rawInput,
            onValueChange = { input ->
                val filtered = if (idPrefix == "•Group ID: ") input.filter { it.isLetterOrDigit() && it.code < 128 } else input
                rawInput = filtered; onValueChange("$idPrefix$filtered")
            },
            label = { Text("Придумайте Уникальный ID") },
            prefix = { Text(idPrefix, color = Color.Cyan) },
            modifier = Modifier.fillMaxWidth(),
            isError = isIdAlreadyTaken
        )
        if (isIdAlreadyTaken) {
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("❌ ", fontSize = 10.sp)
                Text(text = "Ошибка! Уникальный индификатор ID уже занят! Пожалуйста, придумайте другой.", color = Color(0xFFFFCC00), fontSize = 11.sp)
            }
        }
    }
}

// 5. АСИММЕТРИЧНЫЕ КАПСУЛЫ СООБЩЕНИЙ С СИСТЕМОЙ СТРОГИХ ГАЛОЧЕК И КРУЖОЧКОВ
@Composable
fun HardcoreMessageBubble(
    textContent: String, contentType: MessageContentType, timestamp: Long, status: Int,
    isOutgoing: Boolean, intentBlocked: Boolean, chatType: ChatType
) {
    val formattedTime = remember(timestamp) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        DateFormat.format("HH:mm", cal).toString()
    }
    val bubbleAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isOutgoing) Color(0xFF1A1A2E) else Color(0xFF2D2D2D)
    val shape = if (isOutgoing) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 12.dp), contentAlignment = bubbleAlignment) {
        Card(shape = shape, colors = CardDefaults.cardColors(containerColor = bubbleColor), modifier = Modifier.widthIn(max = 280.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                when (contentType) {
                    MessageContentType.TEXT -> Text(text = textContent, color = Color.White, fontSize = 15.sp)
                    MessageContentType.VOICE -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Mic, null, tint = Color.Cyan, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Голосовое сообщение", color = Color.White, fontSize = 14.sp) }
                    MessageContentType.AUDIO -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AudioFile, null, tint = Color(0xFFFF0066), modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(text = textContent.ifBlank { "Аудиофайл" }, color = Color.White, fontSize = 14.sp) }
                    MessageContentType.VIDEO -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayCircle, null, tint = Color.Cyan, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Видеозапись", color = Color.White, fontSize = 14.sp) }
                    MessageContentType.IMAGE -> Text("📷 Фотография", color = Color.White, fontSize = 14.sp)
                    
