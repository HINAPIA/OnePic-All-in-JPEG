package com.example.demo.view

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.FileChooser
import tornadofx.*

//class MainView : View("Hello TornadoFX") {
//    val mainImageView : ImageViewer by inject()
//    override val root = hbox {
//        mainImageView
//    }
//}

class ImageViewer : View() {
    val mainImageView : MainImageView by inject()
    val editView : EditView by inject()
    val subImagesView : SubImagesView by inject()
    override val root = borderpane {
        //메뉴바
        top = menubar{
            menu("File"){
                item("Opne").action{
                    val selectedFile = chooseFile("Select file to open",
                        arrayOf(FileChooser.ExtensionFilter("Image Files",  "*.jpg", "*.jpeg")))
                        .firstOrNull()
                    // 선택된 파일에 대한 처리
                    if (selectedFile != null) {
                    val image = Image(selectedFile.toURI().toString())
                    //val imageView = center as ImageView

                    mainImageView.setImage(image)

                }
                }
            }
        }
        // 이미지를 띄어주는 뷰
        left = mainImageView.root
        right = editView.root
        bottom = subImagesView.root

        setPrefSize(1000.0, 800.0) // 전체 크기를 900x600으로 지정

//        // left
//        mainImageView.root.prefWidthProperty().bind(widthProperty().multiply(0.73))
//        mainImageView.root.prefHeightProperty().bind(heightProperty().multiply(0.784))
//        //right
//        editView.root.prefWidthProperty().bind(widthProperty().multiply(0.27))
//        editView.root.prefHeightProperty().bind(heightProperty().multiply(0.784))
//        //bottom
//        subImagesView.root.prefWidthProperty().bind(widthProperty().multiply(1))
//        subImagesView.root.prefHeightProperty().bind(heightProperty().multiply(0.216))

// left
        mainImageView.root.setPrefSize(730.0, 800.0)
// right
        editView.root.setPrefSize(270.0, 800.0)
// bottom
        subImagesView.root.setPrefSize(1.0, 200.0)


    }

}
