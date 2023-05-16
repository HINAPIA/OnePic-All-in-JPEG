package com.example.demo.view

import com.example.demo.app.ImageTool
import com.example.demo.view.AiContainerSingleton.aiContainer
import com.goldenratio.onepic.PictureModule.AiContainer
import com.madgag.gif.fmsware.AnimatedGifEncoder
import com.madgag.gif.fmsware.GifDecoder
import com.sun.javafx.scene.layout.region.Margins
import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.animation.TranslateTransition
import javafx.embed.swing.SwingFXUtils
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Duration
import tornadofx.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO


class CenterView : View() {

    val aiContainer : AiContainer = AiContainerSingleton.aiContainer
    //val mainImageView : MainImageView by inject()
    val editView : EditView =EditView(this)
    val subImagesView : SubImagesView = SubImagesView(this)
    val mainImageView: ImageView =ImageView()

   // val analysisButton = Button()
    var analysisButton : ImageView = ImageView()
    var analysisLabel : Label = Label()

    val rightImageView: ImageView = ImageView()
    val gifImageVeiew : ImageView = ImageView()
    var isAnalsys : Boolean = false

    val imageSourcePath = "src/main/kotlin/com/example/demo/resource/"

    lateinit var preAnalsImage : Image
    lateinit var analsImage : Image

    var imageOrientation : Int = 0
    var imageTool = ImageTool()
    var fileImageView = ImageView()
    var fileNameLabel = Label()

    override val root = stackpane{
        // '분석하기' 버튼
        addAnalysisButton()
        StackPane.setAlignment(analysisButton, Pos.BOTTOM_RIGHT)
        StackPane.setMargin(analysisButton, Insets(0.0, 70.0, 100.0, 50.0))
        // '분석 중 Text
        analysisLabel.apply{
            text = "분석 중. . ."
            style{
                textFill = c("#FFFFFF") // 글자 색상 흰색
                font = Font.font("Inter", FontWeight.BOLD, 25.0)
            }
            StackPane.setAlignment(this, Pos.CENTER)
            StackPane.setMargin(this, Insets(150.0, 0.0, 0.0, 00.0))
            isVisible = false
        }

        // gifImageView
        gifImageVeiew.fitWidth = 100.0
        gifImageVeiew.isPreserveRatio = true
        StackPane.setMargin(gifImageVeiew, Insets(0.0, 0.0, 180.0, 00.0))

        //file name label
        fileNameLabel.apply{
            text = "김유진.jpeg"
            style{
                textFill = c("#FFFFFF") // 글자 색상 흰색
                font = Font.font("Inter", FontWeight.BOLD, 20.0)
            }
            StackPane.setAlignment(this, Pos.CENTER)
            //StackPane.setMargin(this, Insets(150.0, 0.0, 0.0, 00.0))
            isVisible = false
        }


        // subImageView 위치 조정
        subImagesView.root.maxWidth = 900.0
        subImagesView.root.maxHeight = 180.0
        subImagesView.root.isVisible = false
        StackPane.setMargin(subImagesView.root, Insets(0.0, 0.0, 20.0, 0.0))
        StackPane.setAlignment(subImagesView.root, Pos.BOTTOM_CENTER)

        //editView 위치 조정
        editView.root.setMaxSize(315.0, 530.0)
        editView.root.setMinSize(315.0, 530.0)
        editView.root.isVisible = false
        StackPane.setAlignment(editView.root, Pos.CENTER_RIGHT)
        StackPane.setMargin(editView.root, Insets(00.0, 60.0, 80.0, 0.0))

        fileImageView.apply{
            image =  Image(File(imageSourcePath+ "file.png").toURI().toURL().toExternalForm())
            isVisible = false
        }

        // Main image View
        children.add(mainImageView)
        children.add(fileImageView)
        children.add(gifImageVeiew)
        children.add(fileNameLabel)
        children.add(analysisButton)
        children.add(subImagesView.root)
        children.add(editView.root)
        children.add(analysisLabel)


        style {
            backgroundColor = MultiValue(arrayOf(c("#232323")))
        }


        // 이미지가 로드되면 fitWidth와 fitHeight를 설정
        mainImageView.imageProperty().addListener { _, _, newImage ->
            if (newImage != null) {
                mainImageView.isPreserveRatio = true
//                mainImageView.fitWidthProperty().bind(primaryStage.widthProperty().multiply(0.5));
//
              //  mainImageView.fitHeight = Region.USE_COMPUTED_SIZE

                style {
                    backgroundColor = MultiValue(arrayOf(c("#232323")))
                }
            }
        }
    }
    fun setMainImage(image : Image, rotation : Int){

        mainImageView.translateX = 0.0; mainImageView.translateY = 20.0
        fileImageView.translateX = 0.0; fileImageView.translateY = 20.0
        fileNameLabel.translateX = 0.0; fileNameLabel.translateY = 20.0
        analysisButton.isVisible = true

        subImagesView.clear()
        editView.clear()

        var newImage = imageTool.rotaionImage(image, rotation)
        mainImageView.image = newImage
        mainImageView.fitWidthProperty().bind(primaryStage.widthProperty().multiply(0.5));

       // StackPane.setAlignment(mainImageView, Pos.CENTER)
        // 사진의 비율을 유지하도록 계산하여 설정
        var aspectRatio = mainImageView.image.width / mainImageView.image.height
        mainImageView.fitHeight = mainImageView.fitWidth / aspectRatio

        // 파일 표시
        fileImageView.isVisible = true
        fileImageView.fitWidthProperty().bind(mainImageView.fitWidthProperty().divide(4))
        aspectRatio = fileImageView.image.width / fileImageView.image.height
        fileImageView.fitHeight = fileImageView.fitWidth / aspectRatio
        fileImageView.isPreserveRatio = true // 이미지의 비율을 유지하도록 설정
        StackPane.setMargin(fileImageView, Insets(0.0,0.0 , mainImageView.fitHeight - fileImageView.fitHeight,mainImageView.fitWidth/2+fileImageView.fitWidth+5))

        fileNameLabel.isVisible = true
        StackPane.setMargin(fileNameLabel, Insets(0.0,0.0 , mainImageView.fitHeight + 60,0.0))
    }

    fun startAnalsys() {
       // root.children.
        if(!isAnalsys){
            isAnalsys = true
            startAnimation()
            analyzing()
        }

    }

    fun setFileName(fileName : String){
        fileNameLabel.text = fileName
    }
    fun addAnalysisButton(){
        preAnalsImage =  Image(File(imageSourcePath+ "preAnals.png").toURI().toURL().toExternalForm())
        analsImage =  Image(File(imageSourcePath +"Anals.png").toURI().toURL().toExternalForm())

        // 분석 버튼
        analysisButton = ImageView(preAnalsImage)
        analysisButton.fitWidth = 100.0 // 이미지의 가로 크기를 50으로 지정
        analysisButton.isPreserveRatio = true // 이미지의 비율을 유지하도록 설정

        analysisButton.setOnMouseEntered { e -> analysisButton.setImage(analsImage) }
        analysisButton.setOnMouseExited { e -> analysisButton.setImage(preAnalsImage) }
        analysisButton.setOnMouseClicked { e ->
            // 분석 시작
            startAnalsys()
        }
        // analysisButton x, y 지정
        analysisButton.layoutX = 790.0
        analysisButton.layoutY = 780.0
    }
    fun startAnimation(){
        analysisButton.isVisible = false
        analyzingImageAnimation()

    }

    fun turnTopAnimation(){
        println("이미지 올리기")
        val transition1 = TranslateTransition(Duration.seconds(1.0), mainImageView)
        transition1.byY = -(mainImageView.translateY+80) // 왼쪽으로 100픽셀 이동
        transition1.play()

        val transition2 = TranslateTransition(Duration.seconds(1.0), fileImageView)
        transition2.byY = -(fileImageView.translateY+80) // 왼쪽으로 100픽셀 이동
        transition2.play()

        val transition3 = TranslateTransition(Duration.seconds(1.0), fileNameLabel)
        transition3.byY = -(fileNameLabel.translateY+80) // 왼쪽으로 100픽셀 이동
        transition3.play()

    }

    fun turnLeftAnimation(){
        val transition = TranslateTransition(Duration.seconds(1.0), mainImageView)
        transition.byX = +(mainImageView.translateX-170) // 왼쪽으로 100픽셀 이동
        transition.play()

        val transition2 = TranslateTransition(Duration.seconds(1.0), fileImageView)
        transition2.byX = +(mainImageView.translateX-170) // 왼쪽으로 100픽셀 이동
        transition2.play()

        val transition3 = TranslateTransition(Duration.seconds(1.0), fileNameLabel)
        transition3.byX = +(mainImageView.translateX-170) // 왼쪽으로 100픽셀 이동
        transition3.play()
    }
    fun analyzingImageAnimation(){
        gifImageVeiew.isVisible = true
        val inputStream = FileInputStream(imageSourcePath+ "giphy.gif")
        val gifFrames = getGifFrames(inputStream)
        inputStream.close()

        // main Iamge 위로 올라가기
        turnTopAnimation()

        val timeline = Timeline()

        for (i in gifFrames.indices) {
            val image = gifFrames[i]
            val keyFrame = KeyFrame(Duration.millis(40.0 * i + 1000), EventHandler {
                gifImageVeiew.image = image
                analysisButton.setImage(analsImage)
            })
            timeline.keyFrames.add(keyFrame)
        }
        timeline.cycleCount = 3
        timeline.play()

        // 분석 애니메이션이 끝났을 때
        timeline.setOnFinished {
            finishedAnalysis()

        }

    }

    // 분석 중일 때
    fun analyzing(){
        subImagesView.root.isVisible = true
        analysisLabel.isVisible = true
        //text 바꾸기
        if(aiContainer.textContent.textCount >0){
            subImagesView.chageText(aiContainer.textContent.textList[0].data)
        }
        // 이미지 리스트 뷰 바꾸기
        subImagesView.setPictureList(aiContainer.imageContent.pictureList)
    }

    fun prepareAudio(){// 오디오 준비시키기
        subImagesView.prepareAudio()
    }


    //분석이 끝났을 때
    fun finishedAnalysis(){
        gifImageVeiew.isVisible = false
        analysisLabel.isVisible = false
        isAnalsys = false
        analysisButton.setImage(preAnalsImage)
        // animation
        turnLeftAnimation()
        editView.root.isVisible = true
        editView.update()
    }
    private fun getGifFrames(inputStream: FileInputStream): List<Image> {
        val gifFrames = mutableListOf<Image>()
        try {
            val reader = ImageIO.getImageReadersByFormatName("gif").next()
            val imageInputStream = ImageIO.createImageInputStream(inputStream)
            reader.setInput(imageInputStream)
            for (i in 0 until reader.getNumImages(true)){
                val frame = reader.read(i)
                val image = SwingFXUtils.toFXImage(frame as BufferedImage, null)
                gifFrames.add(image)
            }
            reader.dispose()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return gifFrames
    }


    // subImage뷰에서 요소를 선택하면 메타데이타 영역의 색이 바뀜
    fun focusView(type : String, index : Int){
        editView.focusView(type,index)
    }
    fun unfocusView(type : String, index : Int){
        editView.unfocusView(type,index)
    }

    // 메타데이터 영역에서 요소를 선택하면 subImage뷰에서 색이 바뀜
    fun reverseFocusView(type : String, index : Int){
        subImagesView.focusView(type,index)
    }
    fun reverseUnfocusView(type : String, index : Int){
        subImagesView.unfocusView(type,index)
    }

//    fun analsysUI(){
//        // Load the GIF image from file
//        val decoder = GifDecoder()
//        val inputStream: InputStream = FileInputStream(File(imageSourcePath+ "Magnifier.gif"))
//        //val inputStream: InputStream = FileInputStream(File(imageSourcePath+ "1.gif"))
//        decoder.read(inputStream)
//
//        // Create a Timeline to update the ImageView with the frames
//        val timeline = Timeline()
//        for (i in 0 until decoder.frameCount) {
//            val frame = decoder.getFrame(i)
//            val image: Image = SwingFXUtils.toFXImage(frame, null)
//           // gifImageVeiew.image = image
//            val delayTime = decoder.getDelay(i) * 0.02
//            val keyFrame = KeyFrame(Duration.seconds(delayTime), EventHandler {
//                println(i)
//                gifImageVeiew.image = image })
//            timeline.keyFrames.add(keyFrame)
//        }
//        timeline.cycleCount = Animation.INDEFINITE
//        // Start the Timeline
//        timeline.play()
//       // encoder.finish() // Finish encoding the GIF
//
//
//    }



}