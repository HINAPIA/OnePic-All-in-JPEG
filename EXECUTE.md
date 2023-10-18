
# How to Install OnePIC

<br/>

> 본 문서는 안드로이드 버전 10 이상 환경에서의 OnePIC 설치 방법에 대해 기술합니다. <br/>

<br/>

**Docs Map** <br/>
1. [Project File 다운로드](#project-file-다운로드)
2. [OnePIC 앱 실행](#onepic-앱-실행)
3. [Desktop Viewer 실행](#desktop-viewer-실행)
4. [Web Viewer 실행](#web-viewer-실행)

<br/>
<br/>

## Project File 다운로드

자신의 Workspace에 프로젝트 파일을 다운로드합니다.
이 문서에서는 Workspace의 위치를 <b>E:\Workspace</b>로 가정합니다.
```
C:\ > cd C:/Workspace
C:\Workspace > git clone https://github.com/HINAPIA/OnePic-All-in-JPEG.git
```

<br/>
<br/>
<br/>

# OnePIC 앱 실행
<br/>

1. [AndroidStudio 설치](#1-androidstudio-설치)
2. [Android 기기 개발자 모드 설정](#2-android-기기-개발자-모드-설정)
3. [앱 실행](#3-앱-실행)
<br/>

### 1. AndroidStudio 설치
> <a href = "https://developer.android.com/studio/install?hl=ko">AndroidStudio Install Manual</a><br/> 
<br/>

github에서 다운 받은 안드로이드 프로젝트 OnePIC을 실행하기 위해서는 AndroidStudio가 필요합니다.

최신 버전의 Android 스튜디오를 <span><a href = "https://developer.android.com/studio">다운로드</a></span>합니다.
<br/> 
<br/>	
	![AndroidStudio install steps](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/109158497/c8e44ff5-cd4c-47f1-9005-658a6c2d923c)

<br/>
<br/>
<br/>

### 2. Android 기기 개발자 모드 설정 

다음 단계에 따라 안드로이드 기기의 개발자 모드를 설정합니다.
<br>

![Android setting](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/109158497/91d079b7-8422-49e4-9577-b9d1acdfd07f)

<br/>
<br/>
<br/>

### 3. 앱 실행

안드로이드 스튜디오에서 다운받은 프로젝트를 불러와 실행합니다. (프로젝트 경로: C:\Workspace\OnePIC-All-in-JPEG\OnePIC)
<br>

![Android setting](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/109158497/186c025f-f8cb-415d-ab14-3039a2a488ed)

<br/>
<br/>
<br/>

# Desktop Viewer 실행 
<br/>

1. [Java](#1-java-설치)
2. [Android](#2-android)
3. [Install TornadoFX plugin](#3-install-tornadofx-plugin)
<br/>

### 1. Java 

TornadoFX는 JDK 8 이후 버전부터 지원하지 않습니다. 반드시 JDK 8이하 버전을 사용해야 하며, 해당 가이드는 JDK 8 버전을 사용합니다.

 Oracle 사이트에서 [Java JDK Download 페이지](https://www.oracle.com/technetwork/java/javase/downloads/index.html)로 이동합니다. 그리고 사용중인 OS에 맞춰 다운로드를 실행합니다. 이후 JAVA 환경변수를 설정합니다.

![java](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/88374384/e797cf29-6fd1-4ff9-ac62-75c6a8607d51)

<br/>
<br/>
<br/>

### 2. Android

- **환경변수 ANDROID_HOME** 생성

  '새로 만들기'를 클릭하여 ANDROID_HOME 환경 변수를 등록합니다. 변수 값에는 SDK가 설치된 경로를 입력합니다. 

- **Path 추가**

  시스템 변수 Path에 다음 경로를 추가합니다.

  - %ANDROID-SDK%\tools

  - %ANDROID-SDK%\platform-tools

![android1](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/88374384/c1692e53-4691-476a-835f-54e7b5095ab3)

<br/>
<br/>
<br/>

### 3.Install TornadoFX plugin

본 프로젝트는 IntelliJ 통합환경에서 실행되며 다음 가이드에 따라 TornadoFX plugin을 설치합니다.

![그림1](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/88374384/04372bc7-ed17-4ded-b932-47cb6ac09a76)

<br/>
<br/>
<br/>

# Chrome extension 실행 
> 크롬 확장프로그램 실행은 `크롬 브라우저 환경` 에서만 가능합니다.

<br/>

### 1. 브라우저에서 확장 프로그램 관리 페이지 열기

**크롬 브라우저에서 chrome://extensions** 를 입력하여 확장 관리 페이지를 엽니다.
<img width="550" alt="chrome_extension" src="https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/9645bb3b-ee99-481d-aad4-8f0d14544a9d">

<br/>
<br/>
<br/>

### 2. 개발자 모드를 활성화 및 `압축 해제된 확장 프로그램을 로드합니다` 메뉴 선택

<img width="550" alt="chrome_extension" src="https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/1d2f5e3e-53ce-4e60-9031-e6a56a326493">

<br/>
<br/>
<br/>

### 3. 파일 탐색기에서 다운로드 받은 프로젝트의 `Chrome_extension` 선택

<img width="550" alt="chrome_extension" src="https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/6142997c-185b-4e5d-8b1b-928cb55c0731">


<br/>
<br/>
<br/>

### 4. 확장 프로그램 활성화

<img width="550" alt="extension_step4" src="https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/a5763cdf-d59f-453d-a87b-6c81f2fb3314">
