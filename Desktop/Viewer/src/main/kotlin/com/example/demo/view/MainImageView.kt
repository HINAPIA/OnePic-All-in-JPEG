package com.example.demo.view

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.TransferMode
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.BorderPane.setMargin
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import tornadofx.*
import java.awt.dnd.DragSourceDropEvent


class MainImageView : View(){
    private val imageView:ImageView = ImageView()

    override val root = stackpane {
        children.add(imageView)
        // imageView의 위치를 조정
        setAlignment(Pos.CENTER)
       // setMargin(imageView, Insets(10.0))
        style {
            backgroundColor = MultiValue(arrayOf(Color.web("#232323")))
        }

        // 이미지가 로드되면 fitWidth와 fitHeight를 설정합니다.
        imageView.imageProperty().addListener { _, _, newImage ->
            if(newImage != null){
                imageView.fitWidth = Math.min(newImage.width, primaryStage.width * 0.6)
                imageView.fitHeight = Math.min(newImage.height, primaryStage.height * 0.6)
                imageView.isPreserveRatio = true
            }
        }
    }

    fun setImage(image : Image){
        imageView.image = image
    }

}