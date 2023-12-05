# JPEG 확장을 통한 멀티콘텐츠 카메라 솔루션<br>(One PIC All in JPEG)
> One PIC All in JPEG은 멀티 콘텐츠를 담을 수 있는 새로운 형태의 All in JPEG 구현과<br>
> Multi Focus 촬영 및 다양한 편집 기능을 탑재한 카메라 솔루션이다.

<br><br>
<p align="center"><img src="https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/24552311-8041-41fa-85eb-c75cb659e9fe" width="90" height="90"/></p>


<div align = "center">
  
 ## OnePIC
  [![Generic badge](https://img.shields.io/badge/version-1.2.3-green.svg)](https://play.google.com/store/apps/details?id=com.goldenratio.onepic)

</div>

<p align="center">새로운 파일 포맷 All-in JPEG을 기반으로한<br>Multi Focus 촬영 및 다양한 편집이 가능한 카메라 앱</p>


## Installation
<a href="https://play.google.com/store/apps/details?id=com.goldenratio.onepic">Google Playstore</a><br>

## Screenshot
<!--
<p align="center"><img src="https://github.com/HINAPIA/OnePIC/assets/86238720/ebab0094-c5a1-4915-9f67-439ba1145bf2.png" width="580" height="350" /></p>
<p align="center"><img src="https://github.com/HINAPIA/OnePIC/assets/86238720/09365a10-67c5-4780-91be-ee1612dec8a3.png" width="700" height="400"/></p>
<p align="center"><img src="https://github.com/HINAPIA/OnePIC/assets/86238720/01ffa5ce-5ff0-4a6a-89fc-015c4b66b3cd.png" width="700" height="400"/></p> 
-->

![Screenshot1](https://github.com/HINAPIA/OnePIC/assets/109158497/228b07b2-c1cd-4654-8358-19fb88452a3b)<br>
![Screenshot2](https://github.com/HINAPIA/OnePIC/assets/109158497/46d28351-2936-4bcc-b67c-e9c0b0e56c83)<br>
![Screenshot3](https://github.com/HINAPIA/OnePIC/assets/109158497/2114d035-bcf6-44ca-a0e5-e7e98f3ff261)

<br><br>

## :pencil2: 작품 소개
### &nbsp;1.&nbsp;&nbsp;개발 배경
<div>
&nbsp;&nbsp;오늘날, 사진을 기반으로한 다양한 SNS의 활성화로 스마트폰 카메라 성능과 카메라 앱의 기술이 빠르게 발전하고 있다.  그럼에도 불구하고,  촬영자가 원하는 곳에 카메라의 초점을 맞추기 어렵다는 문제와 이미 촬영된 사진의 초점을 바꿀  수 없다는 문제 등  카메라의 초점과 관련된 문제는 여전히 해결되지 않고 있다.<br><br>
&nbsp;&nbsp;본 팀은  이러한 문제를 해결하기 위해, 사용자가 촬영 후  원하는 곳으로 초점을 변경할 수 있는 카메라 솔루션을 개발하였고, 이를 안드로이드 카메라 앱 OnePIC 으로 구현하였다. OnePIC 앱은 본 팀이 설계한 새로운 파일 포맷 All-in JPEG을 이용하여 거리별, 객체별 다초점 촬영과 사진의 초점 후처리, 베스트 사진 추천, 얼굴 블렌딩 등의 후처리 기능을 제공한다. 또한, All-in JPEG 파일을 데스크탑과 웹에서 볼 수 있는 전용 뷰어를 개발하였다. 
</div><br><br>

### &nbsp;2.&nbsp;&nbsp;솔루션 및 개발 내용 요약

- **다초점 촬영 카메라 기술 개발**<br>
&nbsp;한 번의 촬영으로 다초점 이미지를 촬영하는 다초점 촬영 카메라 기술을 개발하였다. 다초점 촬영은 사진을 촬영하는 시점에 거리별 혹은 객체별로 카메라 렌즈의 초점을 순식간에 이동시키며 촬영하여, 다초점 이미지를 촬영하는 기술이다.

- **다초점 이미지 저장 기술 All-in JPEG 개발**<br>
&nbsp;다초점 촬영으로 만들어진 다초점 이미지들을 한 장의 JPEG 파일에 담기 위해, 기존의 JPEG 포맷을 확장한 새로운 파일 포맷 All-in JPEG을 설계 및 구현하였다. All-in JPEG은 한 장의 JPEG 파일 안에 여러 개의 이미지를 담을 수 있는 새로운 파일 포맷으로, 기존 JPEG 파일과 호환성을 유지한다. 

- **초점 후처리 기술을 개발**<br>
&nbsp;촬영 후 사용자가 원하는 곳에 초점이 맞춰진 사진을 얻을 수 있는 초점 후처리 기술을 개발하였다. 다초점 촬영 카메라 기술로 촬영된 All-in JPEG 파일로부터, 사용자는 이 기술을 이용함으로써 터치를 통해 원하는 위치로 초점을 이동시켜, 초점이 맞춰진 사진을 별도의 JPEG 파일로 저장할 수 있다. 

- **All-in JPEG 전용 뷰어 구현**<br>
&nbsp;안드로이드뿐 아니라 데스크탑과 웹에서 All-in JPEG 파일을 볼 수 있는 전용 뷰어를 개발하였다. All-in JPEG 전용 데스크탑 뷰어는 윈도우, 리눅스, 맥 등 운영체제 구분 없이 실행되며, All-in JPEG 전용 웹 뷰어는 구글 크롬 브라우저의 확장 프로그램으로 개발하였다. 이들은 모두 All-in JPEG 파일 내부의 이미지, 오디오, 텍스트를 출력한다.

<br><br>

## :wrench: 시스템 아키텍처
<!-- p align="center"><img src="https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/8ca107dd-dacc-491a-844f-93606626ff00" height="500"/><br -->
![Architecture](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/8ca107dd-dacc-491a-844f-93606626ff00)

&nbsp;OnePIC 앱은 거리별, 객체별 다초점 촬영 및 베스트 사진 추천 등이 가능한 안드로이드 카메라 앱이며 카메라 모듈, All-in JPEG 모듈, 뷰어 모듈, 편집 모듈로 구성된다. 
All-in JPEG 전용 웹 뷰어는 크롬 브라우저의 확장 프로그램으로, 크롬 브라우저 환경이라면 어디에서든 실행할 수 있으며, All-in JPEG 파일에 들어있는 여러 이미지, 오디오, 텍스트를 웹 브라우저에 출력한다.
All-in JPEG 전용 데스크탑 뷰어는 코틀린으로 작성하여 윈도우, 리눅스, 맥 등 운영체제 구분 없이 실행되며, All-in JPEG 파일의 이미지와 오디오, 텍스트 등 내부 콘텐츠를 데스크탑에서 출력한다.<br><br>

<details>
<summary><b>OnePIC 앱</b></summary>
<div markdown="1">    
  
- **카메라 모듈**<br>
&nbsp;카메라 모듈은 카메라 하드웨어 제어를 통해 기본 촬영, 연속 촬영, 객체별 다초점 촬영, 거리별 다초점 촬영을 한다. Camera2 API를 이용하여 모든 카메라 촬영을 제어하고, 객체별 다초점 촬영의 
경우, Tensorflow Lite 라이브러리로 카메라에 잡힌 객체를 감지하여 객체별로 초점이 맞춰진 다초점 이미지를 촬영한다.<br>

- **All-in JPEG 모듈**<br>
&nbsp;All-in JPEG 모듈은 All-in JPEG 파일의 분석 및 적재, 카메라로 촬영된 사진으로부터 All-in JPEG 파일을 생성, 삭제, 편집, 저장하는 작업을 담당한다.<br>

- **뷰어 모듈**<br>
&nbsp;뷰어 모듈은 사용자가 OnePIC 앱의 갤러리에서 사진을 선택하면, 선택한 사진이 All-in JPEG 파일인지 식별한다. JPEG 파일인 경우, 한 장의 이미지만을 출력하며, All-in JPEG 파일인 경우, All-in JPEG을 분석하여 파일 내부의 이미지, 오디오, 텍스트를 사용자에게 보여준다.<br>

- **편집 모듈**<br>
&nbsp;편집 모듈은 다음 기능을 제어한다.<br>
&nbsp;&nbsp;&nbsp;&nbsp;- 초점 후처리: 사용자가 원하는 곳으로 사진의 초점을 이동시키는 기능<br>
&nbsp;&nbsp;&nbsp;&nbsp;- 베스트 사진 추천: 가장 잘 나온 사진을 추천하는 기능<br>
&nbsp;&nbsp;&nbsp;&nbsp;- 얼굴 블렌딩: 가장 잘 나온 얼굴을 합성하여 한 장의 사진을 제작하는 기능<br>
&nbsp;&nbsp;&nbsp;&nbsp;- 매직픽처 생성: 정적인 사진에 움직임을 주는 사진, 매직픽처 제작 기능<br>
 
편집 기능 중, 베스트 사진 추천 기능의 경우 본 팀이 고안한 BestPictureRanking 알고리즘으로 사진을 추천한다.<br><br>
</div>
</details>

<details>
<summary><b>All-in JPEG 전용 웹 뷰어</b></summary>
<div markdown="1">       
&nbsp;All-in JPEG 전용 웹 뷰어는 크롬 확장프로그램으로 구현하였으며, All-in JPEG 모듈과 뷰어 모듈의 2가지 모듈로 이루어진다. All-in JPEG 모듈은 All-in JPEG 파일을 분석하고, All-in JPEG 내부 콘텐츠와 APP1, APP3 메타데이터를 추출한다. 뷰어 모듈은 All-in JPEG 모듈로부터 추출된 All-in JPEG 파일의 내부 콘텐츠들과 메타데이터를 출력하여 사용자에게 보여준다. 
</div>
</details>

<details>
<summary><b>All-in JPEG 전용 데스크탑 뷰어</b></summary>
<div markdown="1">       
&nbsp;All-in JPEG 전용 데스크탑 뷰어는 TornadoFX 프레임워크를 활용하였으며, All-in JPEG 모듈과 뷰어 모듈의 2가지 모듈로 이루어진다. All-in JPEG 모듈은 데스크탑 뷰어로 업로드된 All-in JPEG 파일을 분석하고, All-in JPEG 내부 콘텐츠와 APP1, APP3 메타데이터를 추출한다. 뷰어 모듈은 All-in JPEG 모듈로부터 추출된 All-in JPEG 파일의 내부 콘텐츠들과 메타데이터를 출력하여 사용자에게 보여준다.
</div>
</details>


<br><br>

## :pushpin: All-in JPEG 구조
&nbsp;All-in JPEG은 기존의 JPEG 포맷을 확장하여 오디오와 텍스트는 물론 여러 이미지를 포함할 수 있는 새로운파일 포맷이다.
![Architecture](https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/d6a7b4f1-b9b4-4a47-97d3-b246626f9aa2)
<!-- p align="center"><img src="https://github.com/HINAPIA/OnePic-All-in-JPEG/assets/86238720/d6a7b4f1-b9b4-4a47-97d3-b246626f9aa2" width="auto" height="600"/ -->


<br>


## 기대 효과
- **세상에 없는 스마트폰 다초점 촬영 기술 개발**<br>
한 번의 촬영으로 여러 객체나 거리별로 초점이 맞춰진 다초점 촬영 기술을 새롭게 개발해 냈다. 이를 통해 추후 사진의 초점을 변경할 수 있는 초점 후처리 기능이 가능하며, 촬영 후에도 원하는 곳으로 초점을 변경할 수 있다.<br>

- **JPEG을 확장한 멀티 콘텐츠 파일 포맷 All-in JPEG 개발**<br>
기존 JPEG 포맷을 확장하여 한 장의 JPEG 파일에 여러 개의 이미지, 오디오, 텍스트를 담을 수 있는 새로운 파일 포맷 All-in JPEG을 개발해냈다. All-in JPEG 파일은 기존 JPEG 뷰어에서 보여지도록 호환성을 유지하여 개발했다.<br>

- **공개 소프트웨어 배포**<br>
스마트폰 다초점 촬영 기술과 멀티 콘텐츠를 담을 수 있는 All-in JPEG 포맷을 다루는 공개 소프트웨어를 개발하고 배포하여 안드로이드 카메라 기술 발전에 기여한다. 또한 많은 사용자가 손쉽게 다운받을 수 있도록 OnePIC 앱을 무료로 구글 플레이 스토어에 출시하여 촬영 이후에 초점을 변경할 수 없어 만족스러운 사진을 얻지 못했던 사용자들의 아쉬움을 해결한다.<br>

- **촬영자의 실력과 상관없이 누구나 좋은 사진 제작**<br>
OnePIC 앱의 다초점 촬영 기능과 얼굴 블렌딩 기능을 통해, 원하는 곳에 초점이 맞춰지고, 가장 잘 나온 얼굴들로 이루어진 사진을 제작할 수 있어 촬영자의 실력과 상관없이 누구나 좋은 사진을 얻을 수 있다.<br>

- **All-in JPEG 포맷으로 손쉬운 멀티 콘텐츠 공유**<br>
All-in JPEG 포맷을 이용하여 한 장의 JPEG 파일에 여러 개의 이미지, 오디오, 텍스트를 모두 담아 전송하면 멀티 콘텐츠 공유를 손쉽게 할 수 있다.<br>

<br>

## 활용 분야
- **카메라 앱으로 바로 활용 가능**<br>
&nbsp;OnePIC 카메라 앱을 구글 플레이 스토어에 출시 하여, 손쉽게 다운받아 바로 활용 가능하다. 실제 본인을 포함한 프로젝트 구성원들과 주변 사람들은 OnePIC 앱을 이용하여 사진을 찍고 있다.<br>

- **움직이는 사진으로 엔터테인먼트 요소 제공**<br>
&nbsp;JPEG 파일에 오디오나 영상 효과를 추가하여 정적인 사진이 아닌 생동감 있는 사진을 만들 수 있다. 생동감 있는 사진은 제품 홍보나 교육용 자료, 문화 예술 작품 등 다양한 분야에서 활용할 수 있다.<br>

- **All-in JPEG 포맷을 이용한 다양한 앱 개발**<br>
&nbsp;All-in JPEG 파일의 멀티 콘텐츠 특징을 이용하여 사진에 텍스트를 담아 사진 데이터만 관리하면 되는 일기 앱과 다이어리 앱을 개발할 수 있다. 또한 사진에 음성을 담아 기업의 제품 홍보 및 아티스트 작품에 대한 이해를 높일 수 있는 디지털 설명 앱 등 다양한 용도로 활용할 수 있다.<br>

<br><br>


### - 개발 도구
<img src="https://img.shields.io/badge/Android Studio-3DDC84?style=for-the-badge&logo=Android Studio&logoColor=white"/> <img src="https://img.shields.io/badge/opencv-6EC93F?style=for-the-badge&logo=opencv&logoColor=white"/> <img src="https://img.shields.io/badge/tensorflow lite-FFAA5B?style=for-the-badge&logo=tensorflow&logoColor=white"/> <img src="https://img.shields.io/badge/tornadoFX-000000?style=for-the-badge"/>



### - 개발 언어
![html5](https://img.shields.io/badge/HTML-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![Javascript](https://img.shields.io/badge/Javascript-F7DF1E?style=for-the-badge&logo=Javascript&logoColor=white)
<img src="https://img.shields.io/badge/Kotlin-4193D0?style=for-the-badge&logo=kotlin&logoColor=white"/>


<br/><br/>

## 🎈사용자 메뉴얼

- <b>객체별 초점 촬영
  <br/> <br/> 
  <img src=./report/img/userMenual1.png width="700">
  <br/><br/><br/>

- 거리별 초점 촬영
   <br/> <br/> 
   <img src=./report/img/userMenual2.png width="700">
  <br/><br/><br/>
  
- 초점 후처리
  <br/> 
  <img src=./report/img/userMenual3.png width="820">
  <br/><br/><br/>
  
- 베스트 사진 추천
  <br/> 
  <img src=./report/img/userMenual4.png width="700">
  <br/><br/><br/>
  
- 매직픽처
  <br/>
  <img src=./report/img/userMenual6.png width="820">
  <br/><br/><br/>


