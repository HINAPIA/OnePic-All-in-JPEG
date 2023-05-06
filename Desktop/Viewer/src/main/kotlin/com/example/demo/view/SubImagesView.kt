package com.example.demo.view

import javafx.scene.paint.Color
import tornadofx.*

class SubImagesView : View(){

    override val root = stackpane {

        style {
            backgroundColor = MultiValue(arrayOf(Color.web("#313131")))
        }
    }
}