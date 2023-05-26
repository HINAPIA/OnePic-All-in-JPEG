package com.example.demo.view

import com.example.demo.app.ImageTool
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.PictureModule.AiContainer
import com.goldenratio.onepic.PictureModule.Contents.Picture
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.ScaleTransition
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.layout.HBox.setMargin
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tornadofx.*
import tornadofx.Stylesheet.Companion.content
import tornadofx.Stylesheet.Companion.root
import tornadofx.WizardStyles.Companion.content
import java.beans.EventHandler
import java.io.File

class SubImagesView(val centerView : CenterView) : View() {
    var pictureListChangeListener : PictureListChangeListener
    private val pictureList: ObservableList<Picture> = FXCollections.observableArrayList()
    private var picturesHBox: HBox = HBox()
    val AiContainer : AiContainer = AiContainerSingleton.aiContainer
    var picturesPane : HBox = HBox()
    var picturesScrollPane : ScrollPane= ScrollPane()

    lateinit var textView : StackPane
    var textLabel : Label = Label()
    lateinit var audioView : StackPane
    var audioTextLabel : Label = Label()

    var subImageView = ImageView()
    var audioResolver = AudioResolver()

    val imageSourcePath = "src/main/kotlin/com/example/demo/resource/"
    val audioSourcePath = "src/main/kotlin/com/example/demo/resource/audio/"

    var animationTime = 0.5
    var mediaPlayer : MediaPlayer? = null

    override val root = stackpane {
        audioResolver.subImagesView = this@SubImagesView
        // 흰색
        imageview {
            var baseImage =  Image(File(imageSourcePath +"base.png").toURI().toURL().toExternalForm())
            image = baseImage
            fitWidth = 830.0 // 이미지의 가로 크기를 50으로 지정
            isPreserveRatio = true // 이미지의 비율을 유지하도록 설정
        }
        // Text, Auido
        hbox {
            setMinSize(220.0, 130.0)
            setMaxSize(220.0, 130.0)
            vbox{
                spacing = 20.0
                // Text
                textView = addTextView()
                textView.setOnMouseEntered { e ->
                    centerView.focusView("text", 0)
                    textView.style{
                        borderWidth += box(4.px)
                        borderColor += box(c("#31D655"))
                        borderRadius  += box(10.px)
                    }
                }
                textView.setOnMouseExited { e ->
                    centerView.unfocusView("text", 0)
                    textView.style{
                        borderWidth += box(0.px)
                        borderColor += box(c("#31D655"))
                    }
                }
                textView.setOnMouseClicked {
                    centerView.textContentStackPaneToggle()
                }
                add(textView)
                // Audio
                audioView = addAudioView()
                audioView.setOnMouseEntered { e ->
                    centerView.focusView("audio", 0)
                    audioView.style{
                        borderWidth += box(4.px)
                        borderColor += box(c("#EA2424"))
                        borderRadius += box(10.px)
                    }
                }
                audioView.setOnMouseExited { e ->
                    println("setOnMouseExited ${audioResolver.isPlaying}")
                    if(!audioResolver.isPlaying){
                        centerView.unfocusView("audio", 0)
                        audioView.style{
                            borderWidth += box(0.px)
                            borderColor += box(c("#EA2424"))
                        }
                    }

                }
                audioView.setOnMouseClicked { e->
                    audioResolver.play()
                    audioResolver.audioView = audioView
                }

                add(audioView)
                textView.isVisible = false
                audioView.isVisible = false
            }
            alignment = Pos.CENTER

            StackPane.setAlignment(this, Pos.CENTER_RIGHT)
            StackPane.setMargin(this, Insets(0.0, 100.0, 0.0, 10.0))
        }
        //images
        picturesScrollPane = scrollpane {
            content = picturesPane

            setMinSize(500.0, 130.0)
            setMaxSize(500.0, 130.0)
            setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
            setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
            style{
                paddingAll = 5.0
                background = Background(BackgroundFill(Color.web("#020202"), CornerRadii(15.0), Insets.EMPTY))
                effect = DropShadow(10.0, 0.0, 5.0, javafx.scene.paint.Color.GRAY).apply {
                    blurType = javafx.scene.effect.BlurType.ONE_PASS_BOX
                }
            }
            // 스크롤 팬이 렌더링된 후에 lookup을 호출하여 수평 스크롤바 커스터마이징
            Platform.runLater {
                val hBar = picturesScrollPane.lookup(".scroll-bar")
                picturesScrollPane.lookup(".scroll-bar").style = "-fx-background-color: #302F2F;"
                hBar?.style {
                    backgroundColor = multi(Color.BLACK)
                    prefWidth = 3.px
                    prefHeight = 100.px
                }
                hBar?.minHeight(0.0)
            }
            isVisible= false
            StackPane.setAlignment(this, Pos.CENTER_LEFT)
            StackPane.setMargin(this, Insets(0.0, 0.0, 0.0, 50.0))

        }


         picturesPane.apply{
             setMinSize(500.0, 130.0)
             maxHeight(130.0)
            style{
                backgroundColor = MultiValue(arrayOf(Color.web("#020202")))
                effect = DropShadow(10.0, 0.0, 5.0, javafx.scene.paint.Color.GRAY).apply {
                    blurType = javafx.scene.effect.BlurType.ONE_PASS_BOX
                }
            }
         }
    }

    init {
        pictureListChangeListener = PictureListChangeListener(centerView, picturesScrollPane, textView, audioView)
        pictureList.addListener(pictureListChangeListener)
    }

    fun addAudioView() : StackPane{
        return stackpane{
            setMinSize(200.0, 50.0)
            setMaxSize(200.0, 50.0)
            isVisible = false
            style{
                paddingAll = 10.0
                background = Background(BackgroundFill(Color.web("#020202"), CornerRadii(15.0), Insets.EMPTY))
                effect = DropShadow(10.0, 0.0, 5.0, javafx.scene.paint.Color.GRAY).apply {
                    blurType = javafx.scene.effect.BlurType.ONE_PASS_BOX
                }
            }
            imageview {
                var audioImage =  Image(File(imageSourcePath +"audio.png").toURI().toURL().toExternalForm())
                image = audioImage
                fitWidth = 22.0
                isPreserveRatio = true
                StackPane.setAlignment(this, Pos.CENTER_LEFT)
                StackPane.setMargin(this, Insets(0.0, 10.0,0.0,0.0))
            }
            audioTextLabel = label {
                text = "00 : 00"
                style {
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.BOLD, 11.0)

                }
            }
        }
    }

    fun setAudioTextLabel(text : String){
        runLater{
            audioTextLabel.text = text
        }
    }
    fun prepareAudio(){

        audioResolver.prepare()
    }
    fun addTextView() : StackPane{
        return stackpane{
            setMinSize(200.0, 50.0)
            setMaxSize(200.0, 50.0)
            isVisible = false
            style{
                paddingAll = 10.0
                background = Background(BackgroundFill(Color.web("#020202"), CornerRadii(15.0), Insets.EMPTY))
                effect = DropShadow(10.0, 0.0, 5.0, javafx.scene.paint.Color.GRAY).apply {
                    blurType = javafx.scene.effect.BlurType.ONE_PASS_BOX
                }
            }
            hbox {
                imageview {
                    var textImage =  Image(File(imageSourcePath +"text.png").toURI().toURL().toExternalForm())
                    image = textImage
                    fitWidth = 18.0
                    isPreserveRatio = true
                }
                textLabel =label{
                    text = "안녕하세요"
                    style{
                        textFill = c("#FFFFFF") // 글자 색상 흰색
                        font = Font.font("Inter", FontWeight.BOLD, 11.0)

                    }
                }
                alignment = Pos.CENTER_LEFT
                spacing = 10.0
            }

        }
    }
    fun chageText(newMessage : String){
        textLabel.text = newMessage
    }

    fun setPictureList(_pictureList: ArrayList<Picture>) {
        pictureList.removeListener(pictureListChangeListener) // 변경 리스너 제거
        pictureList.setAll(_pictureList)
        pictureList.addListener(pictureListChangeListener) // 변경 리스너 다시 등록
        pictureList.setAll(_pictureList)
    }

    fun clear(){
        runLater {
            picturesPane.children.clear()
            root.isVisible = false
            textView.isVisible = false
            audioView.isVisible = false
            audioTextLabel.text = "00 : 00"
        }
    }

    fun focusView(type:String, index : Int){
        when(type){
           // "image" -> selectImageConentView(index)
            "text" -> {
                textView.style{
                    borderWidth += box(4.px)
                    borderColor += box(c("#31D655"))
                    borderRadius  += box(10.px)

                }
            }
            "audio" -> {
                audioView.style{
                    borderWidth += box(4.px)
                    borderColor += box(c("#EA2424"))
                    borderRadius  += box(10.px)

                }
            }
        }
    }

    fun unfocusView(type:String, index : Int){
        when(type){
            //"image" -> unSelectImageConentView(index)
            "text" -> {
                textView.style{
                    borderWidth += box(0.px)
                    borderColor += box(c("#31D655"))
                    borderRadius  += box(10.px)

                }
            }
            "audio" -> {
                audioView.style{
                    borderWidth += box(0.px)
                    borderColor += box(c("#31D655"))
                    borderRadius  += box(10.px)

                }
            }
        }
    }

    class PictureListChangeListener(val centerView : CenterView, private val pictureScrollPane: ScrollPane, val textView : StackPane, val audioView : StackPane) : ListChangeListener<Picture> {
        private val picturesView = mutableListOf<ImageView>()
        var imageTool = ImageTool()
        var animationTime = 0.5

        fun addGrowingAnimation(){
            val scaleTransition = ScaleTransition(Duration.seconds(animationTime), audioView)
            scaleTransition.fromX = 0.0
            scaleTransition.fromY = 0.0
            scaleTransition.toX = 1.0
            scaleTransition.toY = 1.0
            scaleTransition.interpolator = Interpolator.EASE_OUT
            scaleTransition.play()
        }
        fun selectView(imageView: ImageView ){
            imageView.style{
                backgroundColor += c("yellow")
                borderWidth += box(10.px)
                borderColor += box(Color.BLUE)
            }
        }

        override fun onChanged(change: ListChangeListener.Change<out Picture>?) {
            var picturesPane = pictureScrollPane.content as HBox
            while (change?.next() == true) {
                // 분석한 사진들로 ImageVIew채우기
                if (change.wasAdded()) {
                    picturesView.clear()
                    for (i in change.from until change.to) {
                        val imageView : ImageView = ImageView()
                        val pictureByte = AiContainerSingleton.aiContainer.imageContent.getJpegBytes(change.list[i])

                        var orientation = AiContainerSingleton.aiContainer.imageContent.orientation
                        imageView.image = imageTool.rotaionImage(Image(pictureByte.inputStream()), orientation)
                        imageView.fitHeightProperty().bind(picturesPane.heightProperty().multiply(0.7))

                        // 사진의 비율을 유지하도록 계산하여 설정
                        val aspectRatio = imageView.image.height / imageView.image.width
                        imageView.fitWidth = imageView.fitHeight / aspectRatio
                        picturesView.add(imageView)

                        imageView.setOnMouseEntered { e ->
                            centerView.focusView("image", i)
                        }
                        imageView.setOnMouseExited { e ->
                            centerView.unfocusView("image", i)
                            imageView.style{
                                //borderWidth += box(0.px)
                                //borderColor += box(Color.BLUE)
                            }
                        }
                        imageView.setOnMouseClicked { e ->
                            centerView.setMainChage(imageView.image)
                        }
                    }
                    runLater {
                        // 검은 창에 이미지 띄우기
                        picturesPane.children.clear()
                        picturesPane.apply {
                            alignment = Pos.CENTER_LEFT
                            spacing = 20.0
                            padding = Insets(10.0, 10.0, 10.0, 10.0)
                            setMargin(this@apply, Insets(0.0, 10.0, 10.0, 20.0))

                            val timeline = Timeline()
                            // 이미지를 담는 검은 배경이 나타나는 애니메이션
                            val keyFrame = KeyFrame(Duration.seconds(1.toDouble()), {
                                pictureScrollPane.isVisible = true
                                val scaleTransition = ScaleTransition(Duration.seconds(animationTime), pictureScrollPane)
                                scaleTransition.fromX = 0.0;scaleTransition.fromY = 0.0
                                scaleTransition.toX = 1.0;scaleTransition.toY = 1.0
                                scaleTransition.interpolator = Interpolator.EASE_OUT
                                scaleTransition.play()
                            })
                            timeline.keyFrames.add(keyFrame)
                            // 이미지가 하나씩 나타나는 애니메이션
                            for (i in 0..picturesView.size - 1) {
                                // 애니메이션
                                val keyFrame = KeyFrame(Duration.seconds(animationTime*i +2.toDouble()), {
                                    val imageView = picturesView[i]
                                    imageView.apply {
                                        style {
                                            borderWidth += box(5.px)
                                            borderColor += box(Color.BLUE)
                                            backgroundColor += c("yellow")
                                            // 상하좌우 모서리 모두 10px 둥글게 처리
                                            borderRadius = multi(box(10.px)) }
                                        //paddingAll = 10.0
                                        //background = Background(BackgroundFill(Color.web("#020202"), CornerRadii(15.0), Insets.EMPTY))
                                    }
//
                                    picturesPane.children.add(imageView)
                                    // 점점 커지는 애니메이션
                                    addGrowingAnimation()
                                    val scaleTransition = ScaleTransition(Duration.seconds(1.0), picturesView[i])
                                    scaleTransition.fromX = 0.0;scaleTransition.fromY = 0.0
                                    scaleTransition.toX = 1.0;scaleTransition.toY = 1.0
                                    scaleTransition.interpolator = Interpolator.EASE_OUT
                                    scaleTransition.play()
                                })
                                timeline.keyFrames.add(keyFrame)
                            // picturesPane.children.add(picturesView[i])
                            }
                            timeline.play()

                            // 텍스트, 오디오 요소가 나타나는 애니메이션
                            val timeline2 = Timeline()
                            var keyFrame2 = KeyFrame(Duration.seconds((picturesView.size)*animationTime+2.toDouble()),{
                                textView.isVisible = true
                                // 점점 커지는 애니메이션
                                val scaleTransition = ScaleTransition(Duration.seconds(1.0), textView)
                                scaleTransition.fromX = 0.0;scaleTransition.fromY = 0.0
                                scaleTransition.toX = 1.0;scaleTransition.toY = 1.0
                                scaleTransition.interpolator = Interpolator.EASE_OUT
                                scaleTransition.play()
                            })

                            timeline2.keyFrames.add(keyFrame2)
                            keyFrame2 = KeyFrame(Duration.seconds((picturesView.size)*animationTime +3.toDouble()),{
                                audioView.isVisible = true
                                // 점점 커지는 애니메이션
                                val scaleTransition = ScaleTransition(Duration.seconds(1.0), audioView)
                                scaleTransition.fromX = 0.0;scaleTransition.fromY = 0.0
                                scaleTransition.toX = 1.0;scaleTransition.toY = 1.0
                                scaleTransition.interpolator = Interpolator.EASE_OUT
                                scaleTransition.play()
                            })
                            timeline2.keyFrames.add(keyFrame2)
                            timeline2.play()
                        }
                    }
                }
            }
        }
    }

}
