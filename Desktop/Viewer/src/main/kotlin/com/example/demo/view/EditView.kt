package com.example.demo.view

import com.goldenratio.onepic.AudioModule.AudioResolver
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.scene.shape.Box
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import tornadofx.*
import tornadofx.Stylesheet.Companion.scrollPane
import java.io.File
import javax.swing.text.LabelView


class EditView (val centerView : CenterView) : View(){
    // text
    private val textImageView: ImageView = ImageView()
    private val textContentLabel : Label = Label("")

    // aduio
    private  val audioImageView : ImageView = ImageView()
    //private val audioResolver : AudioResolver = AudioResolver()
    private val audioPlayStartImageView : ImageView = ImageView()

    // Ai meta
    private var aiMetaDataImageView : ImageView = ImageView()
    private val aiTextField : Label = Label("")
    private lateinit var aiScrollPane : ScrollPane
    private val aimetaDataView = AimetaDataView(centerView)


    override val root = vbox {
        val textImageUrl = File("src/main/kotlin/com/example/demo/resource/textImage.png").toURI().toURL()
        if(textImageUrl != null){
            val image = Image(textImageUrl.toExternalForm())
            textImageView.image = image
        }

        hbox{
            spacing = 5.0
            add(aiMetaDataImageView)
            aiMetaDataImageView.isPreserveRatio = true
            aiMetaDataImageView.fitHeight = 30.0
            aiMetaDataImageView.fitWidth = 95.0

            label{
                text = "All-in Meta Data"
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.EXTRA_BOLD, 14.0)

                }
            }
        }


        aiScrollPane =  scrollpane {
            padding = insets(10)
            content = stackpane{
                add(aimetaDataView.root)
            }
            // 수직 스크롤
            vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

            //315X480
            // 스크롤 팬의 크기 지정
            setMinSize(300.0, 480.0)
            setMaxSize(300.0, 480.0)
            style{
                backgroundColor = MultiValue(arrayOf(Color.web("#1A1A1A")))
            }
        }
         imageFileLoad()
//315X530
            // Ai MeataData
          //  vbox {

//
//                aiScrollPane = ScrollPane()
//                add(aiScrollPane)


//                aiScrollPane.apply{
//                    content = stackpane{
//                        add(aimetaDataView.root)
//                    }
//                    // 수직 스크롤
//                    vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
//                    hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
//
//                    // 스크롤 팬의 크기 지정
//                    prefHeightProperty().bind(this@stackpane.heightProperty().divide(3))
//                    prefWidthProperty().bind(this@stackpane.widthProperty())
//                   // lookup(".viewport").style = "-fx-background-color: #1A1A1A;"
//                    style{
//                        backgroundColor = MultiValue(arrayOf(Color.web("#1A1A1A")))
//                    }
//                }

                //lookup(".viewport").style = "-fx-background-color: #1A1A1A;"
                Platform.runLater {
                    val viewport = aiScrollPane.lookup(".viewport")
                    viewport?.setStyle("-fx-background-color: #1A1A1A;")
                    aiScrollPane.lookup(".scroll-bar:vertical").style = "-fx-background-color: #302F2F;"
                    val scrollBarSkin = lookup(".scroll-bar:vertical .thumb") as? Region
                    // scroll bar 스킨이 null이 아닌 경우, 스크롤 바 막대의 색상을 검은색으로 설정
                    scrollBarSkin?.style = "-fx-background-color: black;"
                }
         //   }

        style {
            backgroundColor = MultiValue(arrayOf(c("#232323")))
//              borderWidth += box(2.px)
//              borderColor += box(c("#000000"))
        }
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

    fun clear(){
        runLater {
            root.isVisible = false
            aimetaDataView.root.clear()
        }
    }

    fun update(){
        aimetaDataView.update()
    }

    fun focusView(type:String, index : Int){
        when(type){
            "image" -> aimetaDataView.selectImageConentView(index)
            "text" -> aimetaDataView.selectTextConentView()
            "audio" ->aimetaDataView.selectAudioConentView()
        }

    }
    fun unfocusView(type:String, index : Int){
        when(type){
            "image" -> aimetaDataView.unSelectImageConentView(index)
            "text" -> aimetaDataView.unSelectContentView()
            "audio" -> aimetaDataView.unSelectContentView()

        }

    }
    fun imageFileLoad(){
        // AUDIO 텍스트 그림
        val audioImageUrl =  File("src/main/kotlin/com/example/demo/resource/audioImage.png").toURI().toURL()
        if(audioImageUrl != null){
            val image = Image(audioImageUrl.toExternalForm())
            audioImageView.image = image
        }

        // 재생 시작 그림
        val audioPlayStartImageUrl =File("src/main/kotlin/com/example/demo/resource/playStart.png").toURI().toURL()
        if(audioPlayStartImageUrl != null){
            val image = Image(audioPlayStartImageUrl.toExternalForm())
            audioPlayStartImageView.image = image
        }

        // Ai meta Label
        val aiMeataImageUrl =File("src/main/kotlin/com/example/demo/resource/anal.png").toURI().toURL()
        if(aiMeataImageUrl != null){
            var image = Image(aiMeataImageUrl.toExternalForm())
            aiMetaDataImageView.image = image
        }

    }

    //            label("Ebedded Data"){
//                stackpaneConstraints { // stackpaneConstraints를 사용하여 위치와 크기 설정
//                    //alignment = Pos.CENTER
//                    // 전체 너비로
//                    prefWidthProperty().bind(this@stackpane.widthProperty() as ObservableValue<out Number>)
//                    prefHeightProperty().bind(this@stackpane.heightProperty().divide(14)) // 부모 팬의 1/14 크기로 설정
//                }
//                style {
//                    backgroundColor =  MultiValue(arrayOf(c("#5DCBFA")))// 배경색 파란색
//                    padding = box(5.px) // 내부 여백 설정
//                    textFill = Color.WHITE // 글자 색상 흰색
//                    font = Font.font("Arial", FontWeight.BOLD, 10.0)
//                }
//            }
//            spacing = (10).toDouble()
//
//            // Text - image
//            vbox {
//                add(textImageView)
//                textImageView.fitHeight = 17.0
//                textImageView.fitWidth = 53.0
//                add(textContentLabel)
//                spacing = (10).toDouble()
//                padding = insets(10)
//                stackpaneConstraints {
//                    textContentLabel.prefHeightProperty().bind(this@stackpane.heightProperty().divide(6))
//                    textContentLabel.prefWidthProperty().bind(this@stackpane.heightProperty())
//                    textContentLabel.background = Background(BackgroundFill(c("#1A1A1A"), null, null))
//                    //textContentLabel.text = "dksidfjsld k"
//                }
//            }
//
//            // Audio
//            vbox{
//                add(audioImageView)
//                audioImageView.fitHeight = 21.0
//                audioImageView.fitWidth = 63.0
//                //if(AudioResolver.isOn){
//                vbox {
//                    //audioResolver.mediaView.fitWidthProperty().bind(this@stackpane.widthProperty()-30)
//                   // audioResolver.mediaView.fitHeight = 50.0
//                    //TODO("파일 선택 한번 하면 나타나도록)
////                    audioResolver.prepare()
////                    audioResolver.play()
////
////                    add(audioResolver.mediaView)
////                    audioResolver.mediaView.fitWidth = 100.0 // 부모 노드의 너비에서 30을 빼서 MediaView의 크기를 설정합니다.
////                    audioResolver.mediaView.fitHeight = 50.0 // MediaView의 높이를 50으로 설정합니다
//
//                    add(audioPlayStartImageView)
//                    audioPlayStartImageView.setOnMouseClicked {
//                        audioResolver.play()
//                    }
//                    audioPlayStartImageView.fitHeight = 30.0
//                    audioPlayStartImageView.fitWidth = 30.0
//                    this@vbox.alignment = Pos.CENTER
//
//
//                }
//
//                stackpaneConstraints {
//                  //  audioResolver.mediaView.fitHeightProperty().bind(this@stackpane.heightProperty().divide(6))
//                  //  audioResolver.mediaView.fitWidthProperty().bind(this@stackpane.widthProperty()/2)
//
//                  //  audioPlayStartImageView.fitWidthProperty().bind(this@stackpane.widthProperty().divide(10))
//                  //  audioPlayStartImageView.fitHeightProperty().bind(this@stackpane.heightProperty().divide(10))
//                //audioResolver.mediaView.background = Background(BackgroundFill(c("#1A1A1A"), null, null))
//                    //textContentLabel.text = "dksidfjsld k"
//
//                }
//                padding = insets(10)
//            }


}