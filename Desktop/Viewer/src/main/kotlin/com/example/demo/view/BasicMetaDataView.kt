package com.example.demo.view

import com.example.demo.app.CustomColor
import com.example.demo.app.ImageTool
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import javafx.geometry.Pos
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import tornadofx.*


class BasicMetaDataView() : View(){

    var textContent = VBox()
    var audioContent =  VBox()

    val lablesVbox : VBox = VBox()

    var imageContentViewList : ArrayList<VBox> = arrayListOf()
    override val root = vbox{

        setMaxSize(280.0, 180.0)
//        padding = insets(0,0,0,10)
//        spacing = 10.0
//
//        vboxConstraints {
//            alignment = Pos.CENTER // 오른쪽 정렬
//        }
        style{
            background = Background(BackgroundFill(c("#1A1A1A"), null, null))
            font = Font.font("Inter", FontWeight.BOLD, 10.0)
            borderWidth += box(2.px)
            borderColor += box(c(CustomColor.point))
        }
    }


    fun update(infoList : ArrayList<String>){
        //header.settingHeaderInfo()
        infoList.forEach{ println(it)}

        runLater {
            root.clear()
            val lablesVbox = vbox{
                padding = insets(15)
                spacing = 7.0

                add(createHbox("Image Width", infoList.get(4), 90.0))
                add(createHbox("Image Height", infoList.get(3), 85.0))
                add(createHbox("Created At", infoList.get(0), 30.0))
                add(createHbox("Rotation", infoList.get(2), 73.0))
                add(createHbox("Location", infoList.get(1), 75.0))


                style{
                   // background = Background(BackgroundFill(c("#1A1A1A"), null, null))
                    background = Background(BackgroundFill(c("#1A1A1A"), null, null))

                    //  textFill = c("#000000") // 글자 색상 흰색
                }
            }
            root.add(lablesVbox)
        }

    }

    fun getSpacing(key : String, value: String) : Double{
        val width = 70.0
        var key = ""
        var value =""
        return width - (key.length + value.length )*10
    }

//    fun audioContentView() : VBox {
//        ///var audioContentInfo = header.audioContentInfo
//        return vbox{
//            add(createTitleHbox("Audio Content"))
//            vbox{
//                padding = insets(0,0,0,20)
//                spacing = 5.0
//                add(createHbox("Size","${((audioContentInfo.datasize).toDouble()/100).toInt()}kb", 120.0))
//            }
//            spacing = 5.0
//        }
//    }


    fun createTitleHbox(key: String) : HBox {
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
    fun createHbox(key: String, value : String, _spacing : Double) : HBox {
        return hbox {
            val label1 = label{

                text = key
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.BOLD, 11.0)
                }
            }
            val label2 = label {
                text = value
                isWrapText = true
                maxWidth = 150.0
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.MEDIUM, 10.0)
                }
                hboxConstraints {
                    alignment = Pos.CENTER_RIGHT // 오른쪽 정렬
                }
            }

            spacing = _spacing

        }
    }
}