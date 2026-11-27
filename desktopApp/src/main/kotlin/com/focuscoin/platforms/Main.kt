package com.focuscoin.platforms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay

fun main() = application {
    var blockedAttempt by remember { mutableStateOf(false) }
    Window(onCloseRequest = ::exitApplication, title = "FocusCoin", alwaysOnTop = blockedAttempt) {
        LaunchedEffect(Unit) {
            while (true) {
                val targets = PlatformStore.readString("blockedApps", "")
                    .split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
                val unlocked = PlatformStore.readLong("unlockUntil", 0L) > System.currentTimeMillis()
                if (targets.isEmpty() || unlocked) blockedAttempt = false
                else if (!blockedAttempt && ForegroundProcess.executableName() in targets) blockedAttempt = true
                if (blockedAttempt) java.awt.Window.getWindows().firstOrNull { it.isVisible }?.let { window ->
                    runCatching { window.isAlwaysOnTop = true; window.toFront(); window.requestFocus() }
                }
                delay(800)
            }
        }
        Box(Modifier.fillMaxSize()) {
            FocusCoinApp()
            if (blockedAttempt) DesktopLockNotice(onPurchase = { blockedAttempt = false })
        }
    }
}

@Composable
private fun DesktopLockNotice(onPurchase: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xAA17131F)), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFFAF7FF), shadowElevation = 12.dp) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🔒", fontSize = 42.sp)
                Text("지금은 집중 시간입니다.", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF342B45))
                Text("이 앱은 공부 모드가 끝난 뒤 이용할 수 있어요.", color = Color(0xFF625A6E))
                Text("공부를 완료하면 코인을 얻을 수 있어요.", color = Color(0xFF6650A4))
                androidx.compose.material3.TextButton(onClick = onPurchase) { Text("코인으로 이용권 구매하기") }
            }
        }
    }
}
