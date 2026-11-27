import SwiftUI
import FamilyControls
import ManagedSettings
import DeviceActivity
import ComposeApp

@main
struct FocusCoinApp: App {
    var body: some Scene {
        WindowGroup { FocusCoinShell() }
    }
}

struct FocusCoinShell: View {
    @AppStorage("lockMode") private var lockMode = 0
    @AppStorage("selectedAppsJSON") private var selectedAppsJSON = ""
    @AppStorage("selectedAppCount") private var selectedAppCount = 0
    @AppStorage("runningUntil") private var runningUntil = 0
    @AppStorage("unlockUntil") private var unlockUntil = 0
    @State private var pickerPresented = false
    @State private var selection = FamilyActivitySelection()
    @State private var authorizationMessage = "앱 잠금을 설정하려면 먼저 앱을 선택하세요."
    private let store = ManagedSettingsStore()
    private let sharedDefaults = UserDefaults(suiteName: "group.app.focuscoin.shared")

    var body: some View {
        VStack(spacing: 0) {
            FocusCoinComposeView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            HStack {
                Button("잠글 앱 선택") { chooseApps() }
                Spacer()
                Text(lockMode == 1 ? "집중 중" : "잠금 설정")
                    .font(.caption)
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            Text(authorizationMessage)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.bottom, 8)
        }
        .familyActivityPicker(isPresented: $pickerPresented, selection: $selection)
        .onAppear {
            restoreSelection()
            syncSharedData()
            applyShielding()
            scheduleMonitoring()
        }
        .onChange(of: selection) { _, newValue in
            saveSelection(newValue)
            syncSharedData()
            applyShielding()
        }
        .onChange(of: lockMode) { _, _ in
            syncSharedData()
            applyShielding()
            scheduleMonitoring()
        }
        .onChange(of: runningUntil) { _, _ in scheduleMonitoring() }
        .onChange(of: unlockUntil) { _, _ in
            syncSharedData()
            applyShielding()
            scheduleMonitoring()
        }
    }

    private func restoreSelection() {
        guard let data = Data(base64Encoded: selectedAppsJSON),
              let saved = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) else { return }
        selection = saved
    }

    private func saveSelection(_ value: FamilyActivitySelection) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        selectedAppsJSON = data.base64EncodedString()
        selectedAppCount = value.applicationTokens.count + value.categoryTokens.count + value.webDomainTokens.count
    }

    private func chooseApps() {
        Task {
            if AuthorizationCenter.shared.authorizationStatus != .approved {
                do {
                    try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
                } catch {
                    authorizationMessage = "스크린 타임 권한이 없어 앱 잠금이 꺼져 있습니다. 설정에서 다시 허용할 수 있어요."
                    return
                }
            }
            authorizationMessage = "선택한 앱만 기기 안에서 관리합니다. 앱 화면 내용은 읽지 않습니다."
            pickerPresented = true
        }
    }

    private func syncSharedData() {
        sharedDefaults?.set(selectedAppsJSON, forKey: "selectedAppsJSON")
        sharedDefaults?.set(selectedAppCount, forKey: "selectedAppCount")
        sharedDefaults?.set(lockMode, forKey: "lockMode")
        sharedDefaults?.set(runningUntil, forKey: "runningUntil")
        sharedDefaults?.set(unlockUntil, forKey: "unlockUntil")
    }

    private func applyShielding() {
        let unlocked = unlockUntil > Int(Date().timeIntervalSince1970 * 1000)
        store.shield.applications = !unlocked && !selection.applicationTokens.isEmpty ? selection.applicationTokens : nil
        store.shield.webDomains = !unlocked && !selection.webDomainTokens.isEmpty ? selection.webDomainTokens : nil
        store.shield.applicationCategories = !unlocked && !selection.categoryTokens.isEmpty ? .specific(selection.categoryTokens) : nil
        store.shield.webDomainCategories = !unlocked && !selection.categoryTokens.isEmpty ? .specific(selection.categoryTokens) : nil
    }

    private func scheduleMonitoring() {
        syncSharedData()
        let nowMillis = Int(Date().timeIntervalSince1970 * 1000)
        let deadlines = [runningUntil, unlockUntil].filter { $0 > nowMillis }
        guard let deadline = deadlines.min() else { return }
        let endDate = Date(timeIntervalSince1970: TimeInterval(deadline) / 1000)
        let calendar = Calendar.current
        let start = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: Date())
        let end = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: endDate)
        let schedule = DeviceActivitySchedule(intervalStart: start, intervalEnd: end, repeats: false)
        Task {
            try? await DeviceActivityCenter().startMonitoring(DeviceActivityName("FocusCoinBoundary"), during: schedule)
        }
    }
}

struct FocusCoinComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ controller: UIViewController, context: Context) { }
}
