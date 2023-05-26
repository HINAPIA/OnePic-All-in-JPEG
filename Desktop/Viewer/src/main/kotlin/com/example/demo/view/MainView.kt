package com.example.demo.view

import com.example.demo.app.ImageTool
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.PictureModule.AiContainer
import javafx.scene.control.Label
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
    val centerView : CenterView by inject()

    val aiContainer : AiContainer = AiContainerSingleton.aiContainer
    val loadResolver : LoadResolver = LoadResolver()


    override val root = borderpane {
        //메뉴바
        top = menubar {
            menu("File") {
                item("Opne").action {
                    AudioResolver.isOn = true
                    val selectedFile = chooseFile(
                        "Select file to open",
                        arrayOf(FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg"))
                    )
                        .firstOrNull()
                    //centerView.subImagesView.viewClear()
                    // 선택된 파일에 대한 처리
                    if (selectedFile != null) {
                        val fileName = selectedFile?.name ?: ""
                        centerView.setFileName(fileName)
                        var byteArray = Files.readAllBytes(selectedFile.toPath())
                        CoroutineScope(Dispatchers.Default).launch {
                            aiContainer.imageContent.init()
                            loadResolver.createMCContainer(aiContainer, byteArray)
                            while (!aiContainer.imageContent.checkPictureList) {
                                Thread.sleep(100)
                            }
                            var firstImageBytes =
                                aiContainer.imageContent.getJpegBytes(aiContainer.imageContent.pictureList[0])

                            //회전 정보 갱신
                            var orientation = imageTool.getOrientation(firstImageBytes)
                            aiContainer.imageContent.orientation = orientation

                            // 상세 정보 갱신
                            // 현재 사진의 상세 정보 얻어오기
                            var stringList : ArrayList<String> = imageTool.getDetailInfo(firstImageBytes)
                            stringList.add(fileName)
                            centerView.updateDetailView(stringList)

                            val image = Image(selectedFile.toURI().toString())

                            // Main Image 바꾸기
                            centerView.setMainImage(image, orientation)
                            centerView.prepareAudio()
                           // centerView.subImagesView.setPictureList(aiContainer.imageContent.pictureList)
                           // centerView.editView.update()
                        }
                    }

                }
            }

            center = centerView.root
            centerView.root.setPrefSize(990.0, 1000.0)

        }

        setPrefSize(990.0, 1030.0) // 전체 크기를 1200x900으로 지정



        // BorderPane의 크기를 조정할 때 left, right 팬의 너비를 조절합니다.

    }



}
