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

class AimetaDataView : View() {

    private var aiContainer = AiContainerSingleton.aiContainer
    private var header = aiContainer.header
    override val root = vbox{

        padding = insets(0,0,0,10)
        spacing = 5.0
//        add(createHbox("Data Field Length", "100", 30.0))
//        add(createHbox("Image Content", "100", 30.0))
//        add(createHbox("    Count", "100", 30.0))
//        add(createHbox("Text Content", "100", 30.0))
//
//        add(createHbox("Audio Content", "100", 30.0))
//        add(createHbox("    Size", "100", 30.0))
//
//


            style{
                background = Background(BackgroundFill(c("#1A1A1A"), null, null))
                textFill = c("#FFFFFF") // 글자 색상 흰색
                font = Font.font("Arial", FontWeight.NORMAL, 11.0)
            }

//        prefWidth = 600.0
//        prefHeight = 400.0
    }

    fun update(){
        var width = 70
        var key = ""
        var value = ""
        header.settingHeaderInfo()
        runLater {
            root.clear()
            val vbox = vbox{
                padding = insets(10)
                spacing = 5.0

                add(createHbox("- Data Field Length", "100", 70.0))
                add(createHbox("- Indentity", "All-in", 70 +50.0))

                add(imageContentView())
                add(textContentView())
                add(audioContentView())
//                add(createHbox("Image Content", "100", 30.0))
//                add(createHbox("    Count", "100", 30.0))

                style{
                   // background = Background(BackgroundFill(c("#FFFFFF"), null, null))
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
            add(createHbox("- Audio Content", "", 00.0))
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
            add(createHbox("- Text Content", "", 0.0))
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
        return vbox{
            add(createHbox("- Image Content", "",0.0))
            vbox{
                padding = insets(0,0,0,20)
                spacing = 5.0
                add(createHbox("Count", imageContentInfo.imageCount.toString(), 130.0))

                for(i in 0..imageContentInfo.imageCount - 1){
                    var imageInfo = imageContentInfo.imageInfoList.get(i)
                    vbox {
                        add(createHbox("#${i+1} Image","", 80.0))
                        // 1개의 image info
                        vbox {
                            padding = insets(0,0,0,20)
                            add(createHbox("Size","${((imageInfo.dataSize)/1000.0.toInt()).toDouble()}kb", 80.0))
                            var attribute = ContentAttribute.fromCode(imageInfo.attribute)
                            add(createHbox("Attribute", attribute.toString(), 80.0))
                            spacing = 5.0
                        }

                    }
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
                    font = Font.font("Arial", FontWeight.NORMAL, 10.0)
                }
            }
            label {
                text = value
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Arial", FontWeight.MEDIUM, 10.0)
                }
            }

            spacing = _spacing

        }
    }
}