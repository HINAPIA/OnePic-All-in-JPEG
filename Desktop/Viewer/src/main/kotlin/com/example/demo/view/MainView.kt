package com.example.demo.view

import com.example.demo.app.ImageTool
import com.goldenratio.onepic.PictureModule.AiContainer
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.LoadModule.LoadResolver
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.FileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tornadofx.*
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
    val centerView : CenterView  = CenterView(this)

    val aiContainer : AiContainer = AiContainerSingleton.aiContainer
    val loadResolver : LoadResolver = LoadResolver()

    init{
        title = "All in JPEG PC Viewer"
    }
    override val root = borderpane {
        //메뉴바
//        top = menubar {
//            menu("File") {
//                item("Opne").action {
//
//
//                }
//            }
//
//
//        }
        center = centerView.root
        centerView.root.setPrefSize(990.0, 1000.0)

        Platform.runLater{
            centerView.playLogoGif()
        }
        setPrefSize(990.0, 1030.0) // 전체 크기를 1200x900으로 지정
        // BorderPane의 크기를 조정할 때 left, right 팬의 너비를 조절합니다.

    }


    fun  chooseImageFile() : Boolean{
        AudioResolver.isOn = true
        val selectedFile = chooseFile(
            "Select file to open",
            arrayOf(FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg"))
        )
            .firstOrNull()
        // 선택된 파일에 대한 처리
        if (selectedFile != null) {
            val fileName = selectedFile?.name ?: ""
            centerView.setFileName(fileName)
            var byteArray = Files.readAllBytes(selectedFile.toPath())
            CoroutineScope(Dispatchers.Default).launch {
                aiContainer.imageContent.init()
                loadResolver.createAiContainer(aiContainer, byteArray)
                while (!aiContainer.imageContent.checkPictureList) {
                    Thread.sleep(100)
                }
                var firstImageBytes =
                    aiContainer.imageContent.getJpegBytes(aiContainer.imageContent.pictureList[0])

                //회전 정보 갱신
                var orientation = imageTool.getOrientation(firstImageBytes)
                aiContainer.imageContent.orientation = orientation

                val image = Image(selectedFile.toURI().toString())

                // Main Image 바꾸기
                centerView.setMainImage(image, orientation)
                if(aiContainer.audioContent.audio != null)
                    centerView.prepareAudio()

                // 상세 정보 갱신
                var stringList : ArrayList<String> = imageTool.getDetailInfo(firstImageBytes)
                stringList.add("Rotate ${orientation} CW")
                stringList.add(image.width.toInt().toString())
                stringList.add(image.height.toInt().toString())
                centerView.infoList = stringList

                centerView.updateDetailView(stringList)
                // centerView.subImagesView.setPictureList(aiContainer.imageContent.pictureList)
                // centerView.editView.update()
            }
            return true
        }
        return false
    }
}
