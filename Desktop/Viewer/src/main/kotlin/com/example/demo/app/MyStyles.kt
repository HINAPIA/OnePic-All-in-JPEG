package com.example.demo.app

import javafx.scene.text.FontWeight
import tornadofx.*

class MyStyles : Stylesheet() {
    companion object {
        val heading by cssclass()
        val tabPane by cssclass()
        val tab by cssclass()
    }
    init {
        // CSS 파일을 로드합니다.
        tabPane{
            backgroundColor += c(CustomColor.background)

        }
            tab{
                backgroundColor += c(CustomColor.deepBackground)
                textFill = c(CustomColor.point)
            }


        label and heading {
            padding = box(10.px)
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
        }
    }
}