package com.example.demo.view

import com.example.demo.app.ImageTool
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.PictureModule.AiContainer
import javafx.scene.image.Image
import javafx.stage.FileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import tornadofx.*
import java.io.File
import java.nio.file.Files

//class MainView : View("Hello TornadoFX") {
//    val mainImageView : ImageViewer by inject()
//    override val root = hbox {
//        mainImageView
//    }
//}
object AiContainerSingleton {
    val aiContainer = AiContainer()
}
class ImageViewer : View() {
    // ViewModel
    val imageTool = ImageTool()


    val aiContainer : AiContainer = AiContainerSingleton.aiContainer
    val loadResolver : LoadResolver = LoadResolver()
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
                    subImagesView.viewClear()
                    // 선택된 파일에 대한 처리
                    if (selectedFile != null) {
                        var byteArray = Files.readAllBytes(selectedFile.toPath())
                        CoroutineScope(Dispatchers.Default).launch{
                            aiContainer.imageContent.init()
                            loadResolver.createMCContainer(aiContainer, byteArray)
                            while (!aiContainer.imageContent.checkPictureList){
                                Thread.sleep(100)
                            }
                            var firstImageBytes = aiContainer.imageContent.getJpegBytes(aiContainer.imageContent.pictureList[0])

                            var orientation = imageTool.getOrientation(firstImageBytes)
                            val image = Image(selectedFile.toURI().toString())
                            mainImageView.setImage(image, orientation)
                            subImagesView.setPictureList(aiContainer.imageContent.pictureList)
                        }




                    }
                }
            }
        }
        // 이미지를 띄어주는 뷰
        left = mainImageView.root
        right = editView.root
        bottom = subImagesView.root

        setPrefSize(1000.0, 800.0) // 전체 크기를 900x600으로 지정



        // left
        mainImageView.root.setPrefSize(730.0, 800.0)
        // right
        editView.root.setPrefSize(270.0, 800.0)
        // bottom
        subImagesView.root.setPrefSize(1.0, 200.0)


    }

}
