import DeviceActivity
import FamilyControls
import ManagedSettings
import Foundation

private let sharedGroup = "group.app.focuscoin.shared"
private let settingsStore = ManagedSettingsStore()

final class FocusCoinDeviceActivityMonitor: DeviceActivityMonitor {
    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        let defaults = UserDefaults(suiteName: sharedGroup)
        let unlockUntil = defaults?.double(forKey: "unlockUntil") ?? 0
        if unlockUntil > Date().timeIntervalSince1970 * 1000 {
            settingsStore.shield.applications = nil
            settingsStore.shield.webDomains = nil
            settingsStore.shield.applicationCategories = nil
            settingsStore.shield.webDomainCategories = nil
            scheduleNextBoundary(unlockUntil)
            return
        }

        guard let json = defaults?.string(forKey: "selectedAppsJSON"),
              let data = Data(base64Encoded: json),
              let selection = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) else {
            settingsStore.shield.applications = nil
            return
        }
        settingsStore.shield.applications = selection.applicationTokens.isEmpty ? nil : selection.applicationTokens
        settingsStore.shield.webDomains = selection.webDomainTokens.isEmpty ? nil : selection.webDomainTokens
        settingsStore.shield.applicationCategories = selection.categoryTokens.isEmpty ? nil : .specific(selection.categoryTokens)
        settingsStore.shield.webDomainCategories = selection.categoryTokens.isEmpty ? nil : .specific(selection.categoryTokens)
    }

    private func scheduleNextBoundary(_ deadlineMillis: Double) {
        let endDate = Date(timeIntervalSince1970: deadlineMillis / 1000)
        let calendar = Calendar.current
        let start = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: Date())
        let end = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: endDate)
        let schedule = DeviceActivitySchedule(intervalStart: start, intervalEnd: end, repeats: false)
        Task {
            try? await DeviceActivityCenter().startMonitoring(DeviceActivityName("FocusCoinBoundary"), during: schedule)
        }
    }
}
