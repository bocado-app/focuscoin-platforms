package com.focuscoin.platforms

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.ptr.IntByReference

actual object ForegroundProcess {
    actual fun executableName(): String? = runCatching {
        val window: HWND = User32.INSTANCE.GetForegroundWindow() ?: return null
        val pid = IntByReference()
        User32.INSTANCE.GetWindowThreadProcessId(window, pid)
        if (pid.value == 0) return null
        val handle: HANDLE = Kernel32.INSTANCE.OpenProcess(0x1000, false, pid.value) ?: return null
        try {
            val chars = CharArray(2048)
            val size = IntByReference(chars.size)
            if (!Kernel32.INSTANCE.QueryFullProcessImageName(handle, 0, chars, size)) return null
            String(chars, 0, size.value).substringAfterLast('\\').lowercase()
        } finally {
            Kernel32.INSTANCE.CloseHandle(handle)
        }
    }.getOrNull()
}
