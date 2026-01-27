# Onepic 프로젝트 실행 가이드

이 문서는 **OnePIC / OpenCV (libcxx_helper 사용)** 프로젝트를 새로 클론했을 때, 에러 없이 실행하기 위한 최소·정확한 절차를 정리한 가이드입니다.

---

## 1. 필수 환경

### OS

* Windows / macOS / Linux (Windows 기준으로 작성됨)

### Android Studio

* **Android Studio Electric Eel ~ Flamingo 계열 권장**
* Android Gradle Plugin **7.3.x ~ 7.4.x**

---

## 2. Android SDK / NDK / CMake 세팅 (중요)

### 2-1. SDK Manager 설정

`Android Studio → Settings → Android SDK → SDK Tools`

1. **Show Package Details** 체크
2. 아래 항목 설치

#### ✅ 필수

* **NDK (Side by side)** → `21.4.7075529 (r21e)`
* **CMake** → `3.10.2.4988404`

#### ❌ 설치되어 있어도 사용 안 함

* NDK 22 이상
* CMake 3.18 이상 / 4.x

---

## 3. 프로젝트 열기

1. Android Studio 실행
2. **Open** 선택
3. 프로젝트 루트 폴더 선택

   * `settings.gradle` 또는 최상위 `build.gradle`이 있는 위치

---

## 4. 최초 실행 전 정리 (필수)

기존 빌드 캐시가 있으면 충돌 날 수 있으므로 **한 번만** 정리

### 4-1. 폴더 삭제

프로젝트 루트에서 삭제:

* `.cxx/`
* `.externalNativeBuild/`
* `opencv/build/`

(Windows: 탐색기에서 직접 삭제)

---

## 5. 빌드 & 실행

### 5-1. Gradle Sync

* 상단 **Sync Now** 클릭

### 5-2. Clean & Rebuild

메뉴:

* `Build → Clean Project`
* `Build → Rebuild Project`

또는 터미널:

```bash
gradlew clean
```

---

## 6. 실행

1. 에뮬레이터 또는 실제 기기 선택
2. ▶ Run 버튼 클릭

---

## 7. 자주 발생하는 문제

### ❌ CXX1405 / libcxx_helper configure 실패

* NDK가 21이 아님
* CMake가 3.10.2가 아님

👉 SDK Tools에서 버전 재확인

---

### ❌ c++_shared 중복 링크 에러

에러 예:

```
multiple definition of c++_shared
```

👉 `arguments "-DANDROID_STL=c++_shared"` 제거 후 재빌드

---

## 8. 업그레이드 가이드

| 항목                     | 가능 여부          |
| ---------------------- | -------------- |
| compileSdk / targetSdk | ✔ 가능 (34까지 안전) |
| Android Gradle Plugin  | ⚠️ 7.4.x 까지 권장 |
| NDK                    | ❌ 업그레이드 금지     |
| CMake                  | ❌ 업그레이드 금지     |

---

## 9. 요약

* 이 프로젝트는 **NDK r21 + CMake 3.10.2에 고정된 OpenCV 네이티브 프로젝트**
* `local.properties` 없이도 재현 가능해야 정상
* SDK만 최신으로 유지하고, 네이티브 툴체인은 동결

---

문제 발생 시:

* 에러 로그 그대로 확인
* NDK / CMake 버전부터 의심
