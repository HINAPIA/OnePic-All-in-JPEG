package com.example.demo.view

import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.shape.Box
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import tornadofx.*
import java.io.File


class EditView : View(){
    private val textImageView: ImageView = ImageView()

    override val root = stackpane {
        vbox {
            label("Ebedded Data"){
                stackpaneConstraints { // stackpaneConstraints를 사용하여 위치와 크기 설정
                    //alignment = Pos.CENTER
                    // 전체 너비로
                    prefWidthProperty().bind(this@stackpane.widthProperty() as ObservableValue<out Number>)
                    prefHeightProperty().bind(this@stackpane.heightProperty().divide(14)) // 부모 팬의 1/14 크기로 설정
                }
                style {
                    backgroundColor =  MultiValue(arrayOf(c("#5DCBFA")))// 배경색 파란색
                    padding = box(5.px) // 내부 여백 설정
                    textFill = Color.WHITE // 글자 색상 흰색
                    font = Font.font("Arial", FontWeight.BOLD, 10.0)
                }
            }
            box {
                add(textImageView)
                style{
                    
                }
            }

            // topMargin 설정
            //add(textImageView)

            textfield {
                promptText = "Enter some text"
            }
//            val currentDirectory = System.getProperty("user.dir")
//            println("현재 작업 디렉토리는 $currentDirectory 입니다.")

        }

        val imageUrl = File("src/main/kotlin/com/example/demo/resource/textImage.png").toURI().toURL()
        if(imageUrl != null){
            val image = Image(imageUrl.toExternalForm())
            // 이미지 뷰에 이미지 설정
            textImageView.image = image
            // 이미지 뷰 크기에 맞게 이미지 조절


        }
        // 이미지 로드
          style{
            backgroundColor = MultiValue(arrayOf(c("#FFFFFF")))

        }

        // 이미지가 로드되면 fitWidth와 fitHeight를 설정
//        textImageView.imageProperty().addListener { _, _, newImage ->
//            // 이미지 파일 경로
//            //val imageUrl = javaClass.getResource("src/main/kotlin/com.example.demo/resource/textImage.png")
//            val imageUrl = javaClass.getResource("com/example/demo/resource/textImage.png")
//            // 이미지 로드
//            val image = Image(imageUrl.toExternalForm())
//            // 이미지 뷰에 이미지 설정
//            textImageView.image = image
//            // 이미지 뷰 크기에 맞게 이미지 조절
//            //textImageView.isPreserveRatio = true
//            textImageView.fitWidthProperty().bind(this@stackpane.widthProperty())
//            textImageView.fitHeightProperty().bind(this@stackpane.widthProperty().divide(10))
//            style{
//                backgroundColor = MultiValue(arrayOf(c("#FFFFFF")))
//            }
//
//        }
          style {
              backgroundColor = MultiValue(arrayOf(c("#232323")))
              borderWidth += box(2.px)
              borderColor += box(c("#000000"))
        }
    }
}