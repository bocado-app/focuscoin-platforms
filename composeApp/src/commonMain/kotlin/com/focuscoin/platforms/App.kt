package com.focuscoin.platforms

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val Purple = Color(0xFF6650A4)
private val Gold = Color(0xFFF2B84B)
private val Ink = Color(0xFF211B2D)
private val Muted = Color(0xFF756F7D)

private enum class Page(val label: String) { HOME("홈"), LOCK("잠금 앱"), REPORT("리포트"), SETTINGS("설정") }
private enum class TimerStatus { IDLE, RUNNING, PAUSED }

private fun nowMillis() = Clock.System.now().toEpochMilliseconds()
private fun todayKey() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
private fun fmt(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val secs = safe % 60
    return "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

@Composable
fun FocusCoinApp() {
    var darkMode by remember { mutableStateOf(PlatformStore.readLong("dark", 0L) == 1L) }
    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(primary = Color(0xFFCCB9FF)) else lightColorScheme(primary = Purple)) {
        FocusCoinRoot(darkMode = darkMode, setDarkMode = { darkMode = it; PlatformStore.writeLong("dark", if (it) 1 else 0) })
    }
}

@Composable
private fun FocusCoinRoot(darkMode: Boolean, setDarkMode: (Boolean) -> Unit) {
    var page by remember { mutableStateOf(Page.HOME) }
    var coins by remember { mutableStateOf(PlatformStore.readLong("coins", 0L).toInt().coerceAtLeast(0)) }
    var totalMinutes by remember { mutableStateOf(PlatformStore.readLong("studyMinutes", 0L).toInt().coerceAtLeast(0)) }
    var attendanceCount by remember { mutableStateOf(PlatformStore.readLong("attendanceCount", 0L).toInt().coerceAtLeast(0)) }
    var lastAttendance by remember { mutableStateOf(PlatformStore.readString("attendanceDate", "")) }
    var plannedMinutes by remember { mutableStateOf(PlatformStore.readLong("plannedMinutes", 25L).toInt().coerceIn(1, 240)) }
    var remainingSeconds by remember { mutableStateOf(PlatformStore.readLong("remainingSeconds", 0L).toInt().coerceAtLeast(0)) }
    var timerStatus by remember { mutableStateOf(TimerStatus.valueOf(PlatformStore.readString("timerStatus", "IDLE").let { if (it in TimerStatus.entries.map(TimerStatus::name)) it else "IDLE" })) }
    var runningUntil by remember { mutableStateOf(PlatformStore.readLong("runningUntil", 0L)) }
    var showAbandon by remember { mutableStateOf(false) }
    var showAttendanceConfirm by remember { mutableStateOf(false) }
    var weeklyGoal by remember { mutableStateOf(PlatformStore.readLong("weeklyGoal", 600L).toInt().coerceAtLeast(60)) }
    var blockedApps by remember { mutableStateOf(PlatformStore.readString("blockedApps", "")) }
    var unlockCost by remember { mutableStateOf(PlatformStore.readLong("unlockCost", 60L).toInt().coerceAtLeast(1)) }
    var unlockMinutes by remember { mutableStateOf(PlatformStore.readLong("unlockMinutes", 60L).toInt().coerceAtLeast(1)) }
    var unlockUntil by remember { mutableStateOf(PlatformStore.readLong("unlockUntil", 0L)) }
    var selectedAppCount by remember { mutableStateOf(PlatformStore.readLong("selectedAppCount", 0L).toInt().coerceAtLeast(0)) }
    var showUnlockConfirm by remember { mutableStateOf(false) }

    fun saveTimer() {
        PlatformStore.writeLong("plannedMinutes", plannedMinutes.toLong())
        PlatformStore.writeLong("remainingSeconds", remainingSeconds.toLong())
        PlatformStore.writeString("timerStatus", timerStatus.name)
        PlatformStore.writeLong("runningUntil", runningUntil)
    }
    fun addStudyCoins(minutes: Int, completed: Boolean) {
        val award = minutes.coerceAtLeast(0) + if (completed) 10 else 0
        coins += award
        totalMinutes += minutes.coerceAtLeast(0)
        PlatformStore.writeLong("coins", coins.toLong())
        PlatformStore.writeLong("studyMinutes", totalMinutes.toLong())
        PlatformStore.writeLong("earnedTotal", (PlatformStore.readLong("earnedTotal", 0L) + award).coerceAtLeast(0))
    }
    fun finish(completed: Boolean) {
        val worked = if (completed) plannedMinutes else ((plannedMinutes * 60 - remainingSeconds) / 60).coerceAtLeast(0)
        addStudyCoins(worked, completed)
        timerStatus = TimerStatus.IDLE
        remainingSeconds = 0
        runningUntil = 0
        PlatformStore.writeLong("lockMode", 0L)
        saveTimer()
    }
    fun startStudy() {
        if (timerStatus == TimerStatus.IDLE) remainingSeconds = plannedMinutes * 60
        runningUntil = nowMillis() + remainingSeconds * 1000L
        timerStatus = TimerStatus.RUNNING
        PlatformStore.writeLong("lockMode", 1L)
        saveTimer()
    }
    fun buyUnlock() {
        if (coins < unlockCost || (blockedApps.isBlank() && selectedAppCount == 0)) return
        coins = (coins - unlockCost).coerceAtLeast(0)
        unlockUntil = nowMillis() + unlockMinutes * 60_000L
        PlatformStore.writeLong("coins", coins.toLong())
        PlatformStore.writeLong("unlockUntil", unlockUntil)
        PlatformStore.writeLong("spendTotal", PlatformStore.readLong("spendTotal", 0L) + unlockCost)
        PlatformStore.writeLong("unlockPurchaseCount", PlatformStore.readLong("unlockPurchaseCount", 0L) + 1L)
    }

    LaunchedEffect(timerStatus) {
        if (timerStatus == TimerStatus.RUNNING) {
            while (timerStatus == TimerStatus.RUNNING) {
                delay(1000)
                remainingSeconds = ((runningUntil - nowMillis() + 999L) / 1000L).toInt().coerceAtLeast(0)
                PlatformStore.writeLong("remainingSeconds", remainingSeconds.toLong())
                if (remainingSeconds == 0 && timerStatus == TimerStatus.RUNNING) finish(completed = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (timerStatus == TimerStatus.RUNNING && runningUntil > 0) {
            remainingSeconds = ((runningUntil - nowMillis()) / 1000L).toInt().coerceAtLeast(0)
            if (remainingSeconds == 0) finish(completed = true) else saveTimer()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            unlockUntil = PlatformStore.readLong("unlockUntil", 0L)
            selectedAppCount = PlatformStore.readLong("selectedAppCount", 0L).toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FocusCoin", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Purple)
                Spacer(Modifier.weight(1f))
                Text("🪙 $coins", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Page.entries.forEach { tab ->
                    val active = page == tab
                    Button(
                        onClick = { page = tab },
                        colors = ButtonDefaults.buttonColors(containerColor = if (active) Purple else MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(tab.label, color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                when (page) {
                    Page.HOME -> HomePage(coins, totalMinutes, attendanceCount, lastAttendance == todayKey(), weeklyGoal, timerStatus, remainingSeconds, plannedMinutes, onStart = ::startStudy, onAttendance = { showAttendanceConfirm = true })
                    Page.LOCK -> LockInfoPage(
                        blockedApps = blockedApps,
                        coins = coins,
                        unlockCost = unlockCost,
                        unlockMinutes = unlockMinutes,
                        unlockUntil = unlockUntil,
                        hasTargets = blockedApps.isNotBlank() || selectedAppCount > 0,
                        isIos = PlatformStore.isIos(),
                        onBlockedAppsChange = { blockedApps = it; PlatformStore.writeString("blockedApps", it) },
                        onBuyUnlock = { if (coins >= unlockCost && (blockedApps.isNotBlank() || selectedAppCount > 0)) showUnlockConfirm = true },
                        onStudy = ::startStudy
                    )
                    Page.REPORT -> ReportPage(totalMinutes, attendanceCount)
                    Page.SETTINGS -> SettingsPage(
                        darkMode, setDarkMode, weeklyGoal,
                        { weeklyGoal = it; PlatformStore.writeLong("weeklyGoal", it.toLong()) },
                        unlockCost, { unlockCost = it; PlatformStore.writeLong("unlockCost", it.toLong()) },
                        unlockMinutes, { unlockMinutes = it; PlatformStore.writeLong("unlockMinutes", it.toLong()) }
                    )
                }
            }
            if (timerStatus == TimerStatus.IDLE) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    listOf(25, 50, 90).forEach { minutes ->
                        TextButton(onClick = { plannedMinutes = minutes; remainingSeconds = minutes * 60; saveTimer() }) {
                            Text("${minutes}분", color = if (plannedMinutes == minutes) Purple else Muted, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedTextField(
                        value = if (plannedMinutes !in listOf(25, 50, 90)) plannedMinutes.toString() else "",
                        onValueChange = { value -> value.toIntOrNull()?.let { plannedMinutes = it.coerceIn(1, 240); remainingSeconds = plannedMinutes * 60; saveTimer() } },
                        label = { Text("직접 분") },
                        singleLine = true,
                        modifier = Modifier.width(108.dp)
                    )
                }
                Button(onClick = ::startStudy, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(20.dp)) {
                    Text("공부 시작", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (timerStatus == TimerStatus.RUNNING) { timerStatus = TimerStatus.PAUSED; runningUntil = 0L }
                        else { timerStatus = TimerStatus.RUNNING; runningUntil = nowMillis() + remainingSeconds * 1000L; PlatformStore.writeLong("lockMode", 1L) }
                        saveTimer()
                    }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text(if (timerStatus == TimerStatus.RUNNING) "일시정지" else "계속하기") }
                    OutlinedButton(onClick = { showAbandon = true }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("포기") }
                }
            }
        }
    }

    if (showAbandon) AlertDialog(
        onDismissRequest = { showAbandon = false },
        title = { Text("집중 세션을 끝낼까요?") },
        text = { Text("완료한 시간만큼 코인을 받고, 완주 보너스는 지급되지 않아요.") },
        confirmButton = { TextButton(onClick = { showAbandon = false; finish(completed = false) }) { Text("세션 끝내기") } },
        dismissButton = { TextButton(onClick = { showAbandon = false }) { Text("계속 공부") } }
    )

    if (showAttendanceConfirm) AlertDialog(
        onDismissRequest = { showAttendanceConfirm = false },
        title = { Text("오늘 출석을 구매할까요?") },
        text = { Text("보유 코인에서 150코인을 사용해 오늘 출석을 기록합니다.") },
        confirmButton = { TextButton(onClick = {
            showAttendanceConfirm = false
            if (coins >= 150 && lastAttendance != todayKey()) {
                coins = (coins - 150).coerceAtLeast(0)
                attendanceCount++
                lastAttendance = todayKey()
                PlatformStore.writeLong("coins", coins.toLong())
                PlatformStore.writeLong("attendanceCount", attendanceCount.toLong())
                PlatformStore.writeString("attendanceDate", lastAttendance)
                PlatformStore.writeLong("spendTotal", PlatformStore.readLong("spendTotal", 0L) + 150L)
            }
        }) { Text("150코인 사용") } },
        dismissButton = { TextButton(onClick = { showAttendanceConfirm = false }) { Text("취소") } }
    )

    if (showUnlockConfirm) AlertDialog(
        onDismissRequest = { showUnlockConfirm = false },
        title = { Text("앱 이용권을 구매할까요?") },
        text = { Text("선택한 잠금 앱을 ${unlockMinutes}분 동안 이용합니다. $unlockCost코인이 차감돼요.") },
        confirmButton = { TextButton(onClick = { showUnlockConfirm = false; buyUnlock() }) { Text("코인으로 해제") } },
        dismissButton = { TextButton(onClick = { showUnlockConfirm = false }) { Text("취소") } }
    )
}

@Composable
private fun HomePage(coins: Int, minutes: Int, attendance: Int, checkedInToday: Boolean, weeklyGoal: Int, status: TimerStatus, remainingSeconds: Int, plannedMinutes: Int, onStart: () -> Unit, onAttendance: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1ECFA)), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("나의 집중", color = Muted)
                Text("$coins 코인", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("공부 모드 ${when(status) { TimerStatus.IDLE -> "비활성"; TimerStatus.RUNNING -> "진행 중"; TimerStatus.PAUSED -> "일시정지" }}", color = Purple)
            }
        }
        if (status != TimerStatus.IDLE) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (status == TimerStatus.RUNNING) "집중 세션 진행 중" else "세션 일시정지", color = Muted)
                    Box(Modifier.size(176.dp), contentAlignment = Alignment.Center) {
                        val progress = (1f - remainingSeconds / (plannedMinutes * 60f)).coerceIn(0f, 1f)
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 12.dp.toPx()
                            drawArc(Color(0xFFD9D1E7), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                            drawArc(Purple, -90f, 360f * progress, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                        Text(fmt(remainingSeconds), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                    Text("완료하면 최대 ${plannedMinutes + 10}코인을 얻어요.", color = Muted)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("공부 시간", "${minutes / 60}시간 ${minutes % 60}분", Modifier.weight(1f))
            StatCard("이번 주 목표", "$weeklyGoal분", Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("출석 티어", fontWeight = FontWeight.Bold)
                val (icon, title, next) = tier(attendance)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("누적 $attendance회${if (next == null) " · 최고 티어" else " · 다음 티어까지 ${next - attendance}회"}", color = Muted)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onAttendance, enabled = !checkedInToday) { Text(if (checkedInToday) "오늘 출석 완료" else "출석 구매") }
                }
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("공부 시작") }
        Text("오늘의 한 걸음이 목표에 가까워지게 해요.", color = Muted)
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, color = Muted); Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
    }
}

@Composable
private fun LockInfoPage(
    blockedApps: String,
    coins: Int,
    unlockCost: Int,
    unlockMinutes: Int,
    unlockUntil: Long,
    hasTargets: Boolean,
    isIos: Boolean,
    onBlockedAppsChange: (String) -> Unit,
    onBuyUnlock: () -> Unit,
    onStudy: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("잠글 앱", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (!isIos) OutlinedTextField(
                value = blockedApps,
                onValueChange = onBlockedAppsChange,
                label = { Text("Windows 앱 실행 파일 이름") },
                placeholder = { Text("예: robloxplayerbeta.exe, steam.exe") },
                supportingText = { Text("쉼표로 구분해 직접 선택하세요. 이름만 기기에 저장합니다.") },
                modifier = Modifier.fillMaxWidth()
            )
        else Text("iOS 앱은 화면 아래 ‘잠글 앱 선택’에서 선택하세요.", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4DE)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔒 앱 잠금 연결이 필요해요", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Windows는 선택한 실행 파일만 감지해 FocusCoin 안내 창을 앞으로 표시합니다. iOS는 아래 ‘잠글 앱 선택’에서 Screen Time 앱 권한을 켜야 합니다.", color = Ink)
            }
        }
        val remaining = ((unlockUntil - nowMillis()) / 60_000L).toInt().coerceAtLeast(0)
        if (remaining > 0) Text("선택한 앱 사용권 ${remaining}분 남음", color = Purple, fontWeight = FontWeight.Bold)
        else Text("보유 코인 $coins · ${unlockMinutes}분 이용권 $unlockCost코인", color = Muted)
        Button(onClick = onBuyUnlock, enabled = hasTargets && coins >= unlockCost, modifier = Modifier.fillMaxWidth()) {
            Text(if (coins < unlockCost) "${unlockCost - coins}코인 부족" else "선택한 앱 ${unlockMinutes}분 해제 · $unlockCost코인")
        }
        if (coins < unlockCost) TextButton(onClick = onStudy) { Text("공부 시작해 코인 모으기") }
        if (!hasTargets) Text("먼저 잠글 앱을 선택하세요.", color = Muted)
        Text("개인정보 보호", fontWeight = FontWeight.Bold)
        Text("FocusCoin은 키 입력, 비밀번호, 메시지, 화면 내용은 수집하지 않아요. 집중 기록과 코인 잔액은 이 기기에만 저장합니다.", color = Muted)
    }
}

@Composable
private fun ReportPage(totalMinutes: Int, attendance: Int) {
    val earned = PlatformStore.readLong("earnedTotal", 0L).toInt()
    val spent = PlatformStore.readLong("spendTotal", 0L).toInt()
    val unlockPurchases = PlatformStore.readLong("unlockPurchaseCount", 0L).toInt()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("리포트", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("총 공부 시간", "${totalMinutes / 60}시간 ${totalMinutes % 60}분", Modifier.weight(1f))
            StatCard("출석 구매", "$attendance회", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("획득 코인", "$earned", Modifier.weight(1f))
            StatCard("사용 코인", "$spent", Modifier.weight(1f))
        }
        StatCard("앱 사용권 구매", "${unlockPurchases}회", Modifier.fillMaxWidth())
        Text("기록은 이 기기에만 저장됩니다.", color = Muted)
    }
}

@Composable
private fun SettingsPage(
    dark: Boolean,
    setDark: (Boolean) -> Unit,
    goal: Int,
    setGoal: (Int) -> Unit,
    unlockCost: Int,
    setUnlockCost: (Int) -> Unit,
    unlockMinutes: Int,
    setUnlockMinutes: (Int) -> Unit
) {
    var goalText by remember(goal) { mutableStateOf(goal.toString()) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("다크 모드", modifier = Modifier.weight(1f))
            TextButton(onClick = { setDark(!dark) }) { Text(if (dark) "켜짐" else "꺼짐") }
        }
        OutlinedTextField(value = goalText, onValueChange = { value -> goalText = value; value.toIntOrNull()?.let(setGoal) }, label = { Text("주간 공부 목표 (분)") }, singleLine = true)
        var priceText by remember(unlockCost) { mutableStateOf(unlockCost.toString()) }
        var durationText by remember(unlockMinutes) { mutableStateOf(unlockMinutes.toString()) }
        OutlinedTextField(value = priceText, onValueChange = { value -> priceText = value; value.toIntOrNull()?.let { setUnlockCost(it.coerceAtLeast(1)) } }, label = { Text("앱 해제 코인") }, singleLine = true)
        OutlinedTextField(value = durationText, onValueChange = { value -> durationText = value; value.toIntOrNull()?.let { setUnlockMinutes(it.coerceIn(1, 1440)) } }, label = { Text("앱 해제 시간 (분)") }, singleLine = true)
        Text("분당 1코인 · 완주 보너스 10코인 · 하루 출석 구매 150코인", color = Muted)
        Text("설정과 기록은 이 기기에 저장되며 계정이나 서버로 전송되지 않습니다.", color = Muted, fontSize = 13.sp)
    }
}

private fun tier(count: Int): Triple<String, String, Int?> = when {
    count >= 60 -> Triple("💠", "포커스 코인", null)
    count >= 30 -> Triple("🪙", "코인", 60)
    count >= 15 -> Triple("🟤", "구리", 30)
    count >= 5 -> Triple("🪨", "바위", 15)
    else -> Triple("🪨", "자갈", 5)
}
