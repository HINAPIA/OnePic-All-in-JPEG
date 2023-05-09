package com.example.demo.view

import javafx.embed.swing.SwingFXUtils
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
import java.awt.image.BufferedImage


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

        // 이미지가 로드되면 fitWidth와 fitHeight를 설정
        imageView.imageProperty().addListener { _, _, newImage ->
            if(newImage != null){
                imageView.fitWidth = Math.min(newImage.width, primaryStage.width * 0.6)
                imageView.fitHeight = Math.min(newImage.height, primaryStage.height * 0.6)
                imageView.isPreserveRatio = true
            }
        }
    }

    fun setImage(image : Image, rotation : Int){
        var angle : Int = 360 - rotation
        var bufferedImage = SwingFXUtils.fromFXImage(image, null)
        var newBufferedImage = rotateImageClockwise(bufferedImage, angle.toDouble())
        var newImage = SwingFXUtils.toFXImage(newBufferedImage, null)
        imageView.image = newImage
    }


    fun rotateImageClockwise(image: BufferedImage, angle: Double): BufferedImage {
        val radians = Math.toRadians(angle)
        val rotatedWidth = Math.abs(Math.sin(radians) * image.height) + Math.abs(Math.cos(radians) * image.width)
        val rotatedHeight = Math.abs(Math.sin(radians) * image.width) + Math.abs(Math.cos(radians) * image.height)
        val rotatedImage = BufferedImage(rotatedWidth.toInt(), rotatedHeight.toInt(), image.type)
        val graphics2D = rotatedImage.createGraphics()
        graphics2D.translate((rotatedWidth - image.width) / 2.0, (rotatedHeight - image.height) / 2.0)
        graphics2D.rotate(radians, image.width / 2.0, image.height / 2.0)
        graphics2D.drawRenderedImage(image, null)
        graphics2D.dispose()
        return rotatedImage
    }
}