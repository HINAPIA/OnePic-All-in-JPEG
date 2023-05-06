package com.example.demo.view

import javafx.scene.paint.Color
import javafx.scene.shape.Box
import tornadofx.*

class EditView : View(){
    override val root = stackpane {

          style {
              backgroundColor = MultiValue(arrayOf(c("#232323")))
              borderWidth += box(2.px)
              borderColor += box(c("#000000"))
        }
    }
}