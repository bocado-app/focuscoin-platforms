# FocusCoin 플랫폼 앱

FocusCoin의 Windows 데스크톱 앱과 iOS 앱을 준비하는 Kotlin Multiplatform 프로젝트입니다. 타이머, 코인, 출석 티어, 앱 사용권은 기기에만 저장하며 로그인·서버·광고·결제 API를 사용하지 않습니다.

## 지금 포함된 기능

- 한국어 Compose UI, 25·50·90분 프리셋과 직접 시간 입력
- 시작, 일시정지, 이어하기, 포기, 목표 완주 처리
- 1분당 1코인, 완주 보너스 10코인, 150코인 일일 출석 구매
- 출석 구매 횟수에 따른 자갈·바위·구리·코인·포커스 코인 티어
- 앱별 사용권 코인과 시간 설정, 구매 확인과 거래 합계 표시
- 타이머·잔액·출석일·설정의 로컬 저장
- Windows: 직접 적은 실행 파일 이름만 감지하고 FocusCoin 안내 창을 표시
- iOS: FamilyActivityPicker에서 고른 앱·웹 사이트만 Managed Settings로 차단하고 Device Activity 확장에서 시간이 끝날 때 차단 상태를 다시 적용

현재 플랫폼 MVP에는 기존 Android 앱의 시험 플래너·공부 메모·긴급 해제·상세 리포트 등 전체 기능이 아직 포팅되지 않았습니다. Android 앱 저장소와 이 저장소는 별도로 유지됩니다.

## 개인정보

FocusCoin은 키 입력, 비밀번호, 메시지, 다른 앱의 화면 텍스트나 화면 캡처를 읽지 않습니다. Windows에서는 사용자가 지정한 실행 파일 이름만 확인합니다. iOS에서는 사용자가 선택한 앱 토큰만 Screen Time API로 전달하며, 앱 콘텐츠는 읽지 않습니다. 모든 기록은 로컬 Preferences 또는 iOS 기본 설정에 저장합니다.

Windows 잠금은 FocusCoin이 실행 중일 때 동작하는 자기관리용 안내 기능입니다. 사용자가 FocusCoin 자체를 종료하면 감지가 멈춥니다. 기기 보안이나 보호자 통제 기능으로 사용하면 안 됩니다.

## Windows 빌드 및 설치

필요한 도구: JDK 17 이상.

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :desktopApp:createDistributable
```

실행 패키지는 `desktopApp/build/compose/binaries/main/app/FocusCoin/`에 생성됩니다. 앱 폴더를 압축해 다른 Windows 기기로 옮기고 `FocusCoin.exe`를 실행할 수 있습니다. GitHub Actions의 Windows 패키지는 Release의 `FocusCoin-Windows.zip`으로 배포합니다. 현재 패키지는 게시자 코드 서명이 없어 Windows 보안 안내가 나타날 수 있습니다.

잠금 앱 화면에서 `robloxplayerbeta.exe, steam.exe`처럼 실행 파일 이름을 직접 입력하세요. 창 제목·입력 내용·앱 화면은 수집하지 않습니다.

## iOS 빌드 및 배포

iOS 앱은 `iosApp` SwiftUI 진입점에서 공유 Compose 화면을 표시하고, 앱 선택·차단은 Apple Family Controls와 Device Activity를 사용합니다. iOS 시뮬레이터 빌드는 macOS와 Xcode에서만 가능합니다.

GitHub Actions는 iOS 시뮬레이터용 미리보기 아티팩트만 만듭니다. 이 파일은 iPhone에 설치할 수 있는 IPA가 아닙니다. 실제 기기에서 쓰려면 Apple 개발자 계정의 서명 인증서와 프로비저닝 프로필이 필요합니다. 앱 잠금을 배포하려면 Apple이 앱과 Device Activity 확장에 Family Controls 배포 권한을 승인해야 합니다. 또한 `group.app.focuscoin.shared` App Group을 개발자 계정에 등록하고 두 타깃에 연결해야 합니다. 이 승인이 끝나기 전에는 iOS 실기기용 IPA를 Release에 올리지 않습니다.

Apple의 공식 안내:

- [Family Controls 배포 권한 요청](https://developer.apple.com/documentation/familycontrols/requesting-the-family-controls-entitlement)
- [앱 배포 준비와 서명](https://developer.apple.com/documentation/xcode/preparing-your-app-for-distribution)
- [Compose Multiplatform 플랫폼 지원](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)

## GitHub 배포

- `.github/workflows/platform-build.yml`: Windows 패키지와 iOS 시뮬레이터 미리보기를 빌드합니다.
- `.github/workflows/release.yml`: `v*` 태그를 만들면 Windows ZIP만 GitHub Release에 첨부합니다.
- iOS 실기기 IPA는 계정 인증서·프로비저닝 및 Family Controls 승인을 준비한 뒤 별도 배포 작업으로 연결해야 합니다.

릴리스 태그 예시:

```bash
git tag v0.1.0
git push origin v0.1.0
```

## 폴더 구조

```text
composeApp/                  공유 화면과 로컬 저장소
desktopApp/                  Windows 데스크톱 진입점과 패키지 설정
iosApp/iosApp/               SwiftUI 진입점과 Family Controls 앱 선택 화면
iosApp/DeviceActivityMonitor/ iOS 잠금 시간 종료 확장
.github/workflows/           Windows/iOS 빌드와 Windows Release
```
