package com.example.demo.view

import com.goldenratio.camerax.PictureModule.Info.ImageContentInfo
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.ImageContent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Box
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import tornadofx.*

class AimetaDataView (val centerView : CenterView) : View() {

    private var aiContainer = AiContainerSingleton.aiContainer
    private var header = aiContainer.header

     var textContent = VBox()
     var audioContent =  VBox()

    var imageContentViewList : ArrayList<VBox> = arrayListOf()
    override val root = vbox{

        padding = insets(0,0,0,10)
        spacing = 5.0
            style{
                background = Background(BackgroundFill(c("#1A1A1A"), null, null))
               // textFill = c("#FFFFFF") // 글자 색상 흰색
                font = Font.font("Inter", FontWeight.NORMAL, 11.0)
            }

    }
    fun selectImageConentView(index : Int){
        println("index : ${index}")
        if(index < imageContentViewList.size && imageContentViewList.size != 0){
            val imageView = imageContentViewList.get(index)
            // vbox 내부에 있는 모든 text 컨트롤의 색상을 파란색으로 변경
            imageView.lookupAll(".label").forEach { text ->
                text.style {
                    textFill = c("#257CFF") // 파란색으로 변경
                }
            }
        }
    }
    fun selectTextConentView(){
        textContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c("#31D655") // 파란색으로 변경
            }
        }
    }
    fun selectAudioConentView(){
        audioContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c("#EA2424") // 파란색으로 변경
            }
        }
    }
    fun unSelectImageConentView(index : Int){
        if(index < imageContentViewList.size && imageContentViewList.size != 0){
            val imageView = imageContentViewList.get(index)
            imageView.lookupAll(".label").forEach { text ->
                text.style {
                    textFill = c("#FFFFFF") // 파란색으로 변경
                }
            }
        }
    }
    fun unSelectContentView(){
        audioContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c("#FFFFFF") // 파란색으로 변경
            }
        }
        textContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c("#FFFFFF") // 파란색으로 변경
            }
        }
    }
    fun update(){
        header.settingHeaderInfo()
        runLater {
            root.clear()
            val vbox = vbox{
                padding = insets(10)
                spacing = 5.0

                add(createHbox("Data Field Length", "100", 70.0))
                add(createHbox("Indentity", "All-in", 70 +50.0))
                separator()

                add(imageContentView())
                separator()
                textContent = textContentView()
                textContent.setOnMouseEntered { e ->
                    selectTextConentView()
                    centerView.reverseFocusView("text", 0)
                }
                textContent.setOnMouseExited { e ->
                    unSelectContentView()
                    centerView.reverseUnfocusView("text", 0)
                }
                add(textContent)


                separator()
                audioContent = audioContentView()
                audioContent.setOnMouseEntered { e ->
                    selectAudioConentView()
                    centerView.reverseFocusView("audio", 0)
                }
                audioContent.setOnMouseExited { e ->
                    unSelectContentView()
                    centerView.reverseUnfocusView("audio", 0)
                }
                add(audioContent)
//                add(createHbox("Image Content", "100", 30.0))
//                add(createHbox("    Count", "100", 30.0))
                style{
                    background = Background(BackgroundFill(c("#1A1A1A"), null, null))
                  //  textFill = c("#000000") // 글자 색상 흰색

                }
            }
            root.add(vbox)
        }

    }

    fun getSpacing(key : String, value: String) : Double{
        val width = 70.0
        var key = ""
        var value =""
        return width - (key.length + value.length )*10
    }

    fun audioContentView() : VBox{
        var audioContentInfo = header.audioContentInfo
        return vbox{
            add(createHbox("Audio Content", "", 00.0))
            vbox{
                padding = insets(0,0,0,20)
                spacing = 5.0
                add(createHbox("Size","${((audioContentInfo.datasize)/1000.0).toInt().toDouble()}kb", 120.0))
            }
            spacing = 5.0
        }
    }

    fun textContentView() : VBox{
        var textContentInfo = header.textContentInfo
        return vbox{
            add(createHbox("Text Content", "", 0.0))
            vbox{
                padding = insets(0,0,0,20)
                spacing = 5.0
                add(createHbox("Count", textContentInfo.textCount.toString(), 130.0))

                for(i in 0..textContentInfo.textCount - 1){
                    var textInfo = textContentInfo.textInfoList.get(i)
                    vbox {
                        add(createHbox("#${i+1} Text","", 80.0))
                        // 1개의 image info
                        vbox {
                            padding = insets(0,0,0,20)
                            add(createHbox("Content",textInfo.data, 80.0))
                            spacing = 5.0
                        }

                    }
                }
            }
            spacing = 5.0
        }
    }
    fun imageContentView(): VBox{
        var imageContentInfo = header.imageContentInfo
        imageContentViewList.clear()
        return vbox{
            add(createHbox("Image Content", "",0.0))
            vbox{
                padding = insets(0,0,0,20)
                spacing = 5.0
                add(createHbox("Count", imageContentInfo.imageCount.toString(), 130.0))

                for(i in 0..imageContentInfo.imageCount - 1){
                    separator()
                    var imageInfo = imageContentInfo.imageInfoList.get(i)
                    var imageContentView = vbox {
                        //hbox
                        add(createHbox("#${i+1} Image","", 80.0))
                        // 1개의 image info
                         vbox {
                            padding = insets(0,0,0,20)
                            add(createHbox("Size","${(((imageInfo.dataSize)/(1000.0)).toInt()).toDouble()}kb", 80.0))
                            var attribute = ContentAttribute.fromCode(imageInfo.attribute)
                            add(createHbox("Attribute", attribute.toString(), 80.0))
                            spacing = 5.0

                            style{
                                textFill = c("#257CFF") // 글자 색상 파란색
                            }
                        }
                    }
                    imageContentViewList.add(imageContentView)
                }
            }
            spacing = 5.0
        }
    }

    fun createHbox(key: String, value : String, _spacing : Double) : HBox{
        return hbox {
            label{
                text = key
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.NORMAL, 11.0)
                }
            }
            label {
                text = value
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.MEDIUM, 11.0)
                }
            }

            spacing = _spacing

        }
    }
}