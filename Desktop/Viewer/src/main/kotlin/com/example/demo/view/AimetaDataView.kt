package com.example.demo.view

import com.example.demo.app.CustomColor
import com.goldenratio.camerax.PictureModule.Info.ImageContentInfo
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.ImageContent
import javafx.geometry.Insets
import javafx.scene.layout.*
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
        maxWidth = 280.0
        //setMaxSize(280.0, 480.0)
        padding = insets(0,0,0,10)
        spacing = 5.0
            style{
                background = Background(BackgroundFill(c("#1A1A1A"), null, null))
                font = Font.font("Inter", FontWeight.BOLD, 10.0)
//                borderWidth += box(2.px)
//                borderColor += box(c(CustomColor.point))
            }
    }
    fun selectImageConentView(index : Int){
        println("index : ${index}")
        if(index < imageContentViewList.size && imageContentViewList.size != 0){
            val imageView = imageContentViewList.get(index)
            // vbox 내부에 있는 모든 text 컨트롤의 색상을 파란색으로 변경
            imageView.lookupAll(".label").forEach { text ->
                text.style {
                    textFill = c(CustomColor.point) // 파란색으로 변경
                }
            }
        }
    }
    fun selectTextConentView(){
        textContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c(CustomColor.point) // 파란색으로 변경
            }
        }
    }
    fun selectAudioConentView(){
        audioContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c(CustomColor.point) // 파란색으로 변경
            }
        }
    }
    fun unSelectImageConentView(index : Int){
        if(index < imageContentViewList.size && imageContentViewList.size != 0){
            val imageView = imageContentViewList.get(index)
            imageView.lookupAll(".label").forEach { text ->
                text.style {
                    textFill = c("#FFFFFF") // 흰색으로 변경
                }
            }
        }
    }
    fun unSelectContentView(){
        audioContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c("#FFFFFF") // 흰색으로 변경
            }
        }
        textContent.lookupAll(".label").forEach { text ->
            text.style {
                textFill = c("#FFFFFF") // 흰색으로 변경
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

                add(createHbox("Format", "ALL in JPEG", 90.0))
                add(createHbox("Data Field Length", "100", 70.0))
                //add(createHbox("Indentity", "All-in", 70 +50.0))

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
            add(createTitleHbox("Audio Content"))
            vbox{
                padding = insets(0,0,0,20)
                spacing = 5.0
                add(createHbox("Size","${((audioContentInfo.datasize).toDouble()/1000).toInt()}kb", 120.0))
            }
            spacing = 5.0
        }
    }
    fun textContentView() : VBox{
        var textContentInfo = header.textContentInfo
        return vbox{
            add(createTitleHbox("Text Content"))
            vbox{
                padding = insets(0,0,0,20)
                spacing = 5.0
                add(createHbox("Count", textContentInfo.textCount.toString(), 130.0))
                //var textInfo = textContentInfo.textInfoList.get(0)
                for(i in 0..textContentInfo.textCount -1){
                    var textInfo = textContentInfo.textInfoList.get(0)
                    var breakString = textInfo.data.chunked(8).firstOrNull() ?: ""
                    add(createHbox("Content",breakString, 80.0))
                    break
                }
            }
            spacing = 5.0
        }
    }
    fun imageContentView(): VBox{
        var imageContentInfo = header.imageContentInfo
        imageContentViewList.clear()
        return vbox{
            add(createTitleHbox("Image Content"))
            vbox{
                padding = insets(0,0,0,20)
                spacing = 5.0
                add(createHbox("Count", imageContentInfo.imageCount.toString(), 130.0))

                for(i in 0..imageContentInfo.imageCount - 1){
                    separator()
                    var imageInfo = imageContentInfo.imageInfoList.get(i)
                    var imageContentView = vbox {
                        //hbox
                        add(createImageTitleHbox("Image ${i+1}"))
                        // 1개의 image info
                         vbox {
                            padding = insets(0,0,0,20)
                            add(createHbox("Size","${((imageInfo.dataSize).toDouble()/1000).toInt()}kb", 85.0))
                            var attribute = ContentAttribute.fromCode(imageInfo.attribute)
                            add(createHbox("Attribute", attribute.toString(), 70.0))
                            spacing = 5.0

                            style{
                                textFill = c("#257CFF") // 글자 색상 파란색
                            }
                        }
                        spacing = 5.0
                    }
                    imageContentViewList.add(imageContentView)
                }
            }
            spacing = 5.0
        }
    }

    fun createImageTitleHbox(key: String) : HBox{
        return hbox {
            label{
                text = key
                style{
                    textFill = c(CustomColor.point) // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.EXTRA_BOLD, 10.0)
                }
            }
        }
    }
    fun createTitleHbox(key: String) : HBox{
        return hbox {
            label{
                text = key
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.EXTRA_BOLD, 10.0)
                }
            }
        }
    }
    fun createHbox(key: String, value : String, _spacing : Double) : HBox{
        return hbox {
            label{
                text = key
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.BOLD, 10.0)
                }
            }
            label {
                text = value
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.MEDIUM, 10.0)
                }
            }

            spacing = _spacing

        }
    }
}