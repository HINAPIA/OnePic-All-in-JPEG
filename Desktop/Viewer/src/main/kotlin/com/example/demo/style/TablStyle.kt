package com.example.demo.style

import tornadofx.Stylesheet
import tornadofx.importStylesheet

class TablStyle : Stylesheet() {
    val cssPath = "src/main/kotlin/com/example/demo/resource/CSS/"

    init {
        // CSS 파일을 로드합니다.
        importStylesheet(cssPath+"tab.css")
    }
}