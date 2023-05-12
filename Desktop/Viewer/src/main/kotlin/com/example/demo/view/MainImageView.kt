package com.example.demo.view

import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import javafx.scene.layout.BorderPane.setMargin
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import tornadofx.*
import java.awt.dnd.DragSourceDropEvent
import java.awt.image.BufferedImage


class MainImageView : View(){
    val imageView:ImageView = ImageView()
    val subImagesView : SubImagesView by inject()
    val metaVBox : VBox = VBox()
    override val root = borderpane {

        center{
            vbox{
                children.add(imageView)
                setPrefSize(900.0,700.0)
                style{
                   // backgroundColor = MultiValue(arrayOf(Color.web("#000000")))
                }
                setAlignment(Pos.CENTER)
           }
        }

//        bottom{
//            vbox{
//                children.add(subImagesView.root)
//                setPrefSize(900.0, 200.0)
//                style{
//                    border = Border(BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths(2.0, 0.0, 0.0, 0.0)))
//                    //backgroundColor = MultiValue(arrayOf(Color.web("#FFFFFF")))
//                }
//                setAlignment(Pos.CENTER)
//            }
//
//
//        }


       // subImagesView.root.setPrefSize(900.0, 300.0)


        // imageView의 위치를 조정

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

                val leftPadding = (primaryStage.width - imageView.fitWidth) / 2
                imageView.style {
                    padding = box(0.0.px, Dimension((primaryStage.width/2)-(imageView.fitWidth)/2, Dimension.LinearUnits.px),
                        0.0.px, Dimension((primaryStage.width/2)-(imageView.fitWidth)/2, Dimension.LinearUnits.px))

                }
                println("primaryStage.width-imageView.fitWidth :${(primaryStage.width/2)-(imageView.fitWidth)/2}")
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