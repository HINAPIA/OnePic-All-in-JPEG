package com.example.demo.view

import com.example.demo.app.CustomColor
import com.example.demo.app.ImageTool
import com.goldenratio.onepic.PictureModule.AiContainer
import javafx.animation.*
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tornadofx.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class CenterView (imageViewer : ImageViewer) : View(){

    val aiContainer : AiContainer = AiContainerSingleton.aiContainer
    //val mainImageView : MainImageView by inject()
    val editView : EditView =EditView(this)
    val subImagesView : SubImagesView = SubImagesView(this)
    val mainImageView: ImageView =ImageView()

   // val analysisButton = Button()
    var analysisButton : ImageView = ImageView()
    var analysisLabels : VBox = VBox()
    var backgroudView : VBox = VBox()
    var analysisContent : Label = Label()

    val rightImageView: ImageView = ImageView()
    val gifImageVeiew : ImageView = ImageView()
    var isAnalsys : Boolean = false

    val imageSourcePath = "src/main/kotlin/com/example/demo/resource/"
    val fontSourcePath = "src/main/kotlin/com/example/demo/resource/font/"

    lateinit var preAnalsImage : Image
    lateinit var analsImage : Image

    var imageOrientation : Int = 0
    var imageTool = ImageTool()
    var fileImageView = ImageView()
    var fileNameLabel = Label()
    var label = Label()
    var detailView : VBox = VBox()

    var textContentLabel = Label()
    var textContentStackPane= StackPane()

    var animationTime = 0.5

    var calenderLabel : Label = Label()
    var imageLabel : Label = Label()
    var loacionLabel : Label = Label()
    var homeImage : ImageView = ImageView()

    var infoList : ArrayList<String> = arrayListOf()

    var logoImageView = ImageView()
    var textLogoView =ImageView()
    var selectedFileVIew = ImageView()
    var reSelectView : ImageView = ImageView()

    var titleLabel : Label = Label()
    var formatLabel : Label = Label()

    override val root = stackpane{

        style {
            backgroundColor = MultiValue(arrayOf(c(CustomColor.background)))
        }
        formatLabel.apply {
            text = "All in JPEG"
            textAlignment = TextAlignment.CENTER
            isVisible = false
            style{
                textFill = c(CustomColor.white) // 글자 색상 흰색
                font = Font.font("Abhaya Libre", FontWeight.BOLD, 27.0)
                lineSpacing = 2.0
            }
        }
        StackPane.setAlignment(formatLabel, Pos.CENTER_RIGHT)
        StackPane.setMargin(formatLabel, Insets(460.0, 80.0, 0.0, 10.0))

        // title
        titleLabel.apply {
            text = "All in JPEG PC Viewer"
            textAlignment = TextAlignment.CENTER
            style{
                textFill = c(CustomColor.point) // 글자 색상 흰색
                font = Font.font("Abhaya Libre", FontWeight.BOLD, 28.0)
                lineSpacing = 2.0
            }
        }
        StackPane.setAlignment(titleLabel, Pos.TOP_LEFT)
        StackPane.setMargin(titleLabel, Insets(1.0, 0.0, 0.0, 10.0))


        // iamge
        logoImageView.apply{
            fitWidth = 250.0
            isPreserveRatio = true

        }
        StackPane.setMargin(logoImageView, Insets(0.0, 0.0, 100.0, 0.0))

        textLogoView.apply {
            fitWidth = 400.0
            isPreserveRatio = true
        }
        StackPane.setMargin(textLogoView, Insets(350.0, 0.0, 100.0, 0.0))

        // 파일 선택 버튼
        selectedFileVIew.apply {
            val imageUrl =File(imageSourcePath+ "selectedFile.png").toURI().toURL()
            if(imageUrl != null){
                var images = Image(imageUrl.toExternalForm())
                image = images
            }
            fitWidth = 140.0
            isPreserveRatio = true
            // isVisible = false

            setOnMouseClicked {
                var result = imageViewer.chooseImageFile()
                if(result){
                    isVisible = false
                    formatLabel.isVisible = false
                    logoImageView.isVisible = false
                    textLogoView.isVisible = false
                    homeImage.isVisible = true
                    reSelectView.isVisible = true
                    reSelectView.isVisible = true
                    style {
                        backgroundColor = MultiValue(arrayOf(c(CustomColor.background)))
                    }
                }
            }
        }
        StackPane.setMargin(selectedFileVIew, Insets(350.0 + 180, 0.0, 0.0, 0.0))

        // 상세보기
        setDetailView()
        StackPane.setAlignment(detailView, Pos.TOP_RIGHT)
        StackPane.setMargin(detailView, Insets(45.0, 95.0, 0.0, 50.0))

        // home 버튼
        homeImage = ImageView(Image(File(imageSourcePath +"homeIcon.png").toURI().toURL().toExternalForm()))
        homeImage.apply {
            fitWidth = 50.0 // 이미지의 가로 크기를 50으로 지정
            isPreserveRatio = true // 이미지의 비율을 유지하도록 설정
            setOnMouseClicked { e ->
                initComponent()
                style {
                    backgroundColor = MultiValue(arrayOf(c(CustomColor.purple)))
                }
                mainImageView.isVisible = false
                analysisButton.isVisible = false
                fileImageView.isVisible = false
                formatLabel.isVisible = false

                playLogoGif()
            }
        }
        StackPane.setAlignment(homeImage, Pos.TOP_RIGHT)
        StackPane.setMargin(homeImage, Insets(20.0, 70.0, 0.0, 100.0))

        // 다시 선택 버튼
        reSelectView = ImageView(Image(File(imageSourcePath +"selectedFile2.png").toURI().toURL().toExternalForm()))
        reSelectView.apply {
            fitWidth = 50.0 // 이미지의 가로 크기를 50으로 지정
            isPreserveRatio = true // 이미지의 비율을 유지하도록 설정
            isVisible = false
            setOnMouseClicked { e ->
                var result = imageViewer.chooseImageFile()
                if(result){
                    initComponent()
                    //isVisible = false
                    formatLabel.isVisible = false
                    logoImageView.isVisible = false
                    textLogoView.isVisible = false
                    homeImage.isVisible = true
                    isVisible = true
                }
            }
        }
        StackPane.setAlignment(reSelectView, Pos.TOP_RIGHT)
        StackPane.setMargin(reSelectView, Insets(20.0, 130.0, 0.0, 80.0))

        // '분석하기' 버튼
        addAnalysisButton()
        StackPane.setAlignment(analysisButton, Pos.BOTTOM_RIGHT)
        StackPane.setMargin(analysisButton, Insets(0.0, 70.0, 100.0, 50.0))

        // '분석 중 Text
        analysisLabels.apply{
            maxWidth = 600.0
            StackPane.setAlignment(this, Pos.CENTER)
            StackPane.setMargin(this, Insets(150.0, 0.0, 200.0, 00.0))
            isVisible = false
            alignment = Pos.CENTER
            label{
                text = "JPEG 파일 분석 중. . ."
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.BOLD, 24.0)
                }
            }
            analysisContent = label{
                text = ""
                textAlignment = TextAlignment.CENTER
                style{
                    textFill = c("#FFFFFF") // 글자 색상 흰색
                    font = Font.font("Inter", FontWeight.BOLD, 15.0)
                    lineSpacing = 2.0
                }
            }
        }

        // gifImageView
        gifImageVeiew.fitWidth = 100.0
        gifImageVeiew.isPreserveRatio = true
        StackPane.setMargin(gifImageVeiew, Insets(0.0, 0.0, 230.0, 00.0))

//        backgroudView.apply {
//            setMaxSize(330.0, 330.0)
//            isVisible = false
//            style{
//                // 둥글게
//                paddingAll = 5.0
//                background = Background(BackgroundFill(Color.web("#000000BE"), CornerRadii(10.0), Insets.EMPTY))
//            }
//        }

        //file name label
        fileNameLabel.apply{
            text = "김유진.jpeg"
            style{
                textFill = c("#FFFFFF") // 글자 색상 흰색
                font = Font.font("Inter", FontWeight.BOLD, 20.0)
            }
            StackPane.setAlignment(this, Pos.CENTER)
            isVisible = false
        }

        // subImageView 위치 조정
        subImagesView.root.maxWidth = 900.0
        subImagesView.root.maxHeight = 180.0
        subImagesView.root.isVisible = false
        StackPane.setMargin(subImagesView.root, Insets(0.0, 0.0, 40.0, 0.0))
        StackPane.setAlignment(subImagesView.root, Pos.BOTTOM_CENTER)

        //editView 위치 조정
        editView.root.setMaxSize(315.0, 530.0)
        editView.root.setMinSize(315.0, 530.0)
        editView.root.isVisible = false
        StackPane.setAlignment(editView.root, Pos.CENTER_RIGHT)
        StackPane.setMargin(editView.root, Insets(00.0, 60.0, 100.0, 0.0))
        //StackPane.setMargin(editView.root, Insets(100.0, 60.0, 0.0, 0.0))

        fileImageView.apply{
            image =  Image(File(imageSourcePath+ "file.png").toURI().toURL().toExternalForm())
            isVisible = false
        }

        textContentLabel.apply{
            effect = DropShadow()
            text = ""
            style{
                textFill = c("#FFFFFF") // 글자 색상 흰색
                font = Font.font("Inter", FontWeight.BOLD, 15.0)
            }
        }

        // text Content Label(Stack Pane)
        textContentStackPane.apply{
            setMinSize(360.0, 140.0)
            maxWidth = 450.0
            //setMaxSize(360.0, 140.0)
            padding = insets(10)
            add(textContentLabel)
            isVisible = false
        }

        // Main image View
        children.add(selectedFileVIew)
        children.add(logoImageView)
        children.add(textLogoView)
        children.add(mainImageView)
        children.add(fileImageView)
        children.add(gifImageVeiew)
        children.add(fileNameLabel)
        children.add(analysisButton)
        children.add(subImagesView.root)
        children.add(editView.root)
        children.add(analysisLabels)
        children.add(textContentStackPane)
        children.add(reSelectView)
       // children.add(detailView)
        children.add(homeImage)
        children.add(titleLabel)
        children.add(formatLabel)




        // 이미지가 로드되면 fitWidth와 fitHeight를 설정
        mainImageView.imageProperty().addListener { _, _, newImage ->
            if (newImage != null) {
                mainImageView.isPreserveRatio = true

                style {
                    backgroundColor = MultiValue(arrayOf(c("#232323")))
                }
            }
        }
    }


    fun textContentStackPaneToggle(){
        if(textContentStackPane.isVisible){
            textContentStackPane.isVisible = false
        }else{
            textContentStackPane.isVisible = true
        }
    }
    fun setMainChage(_image : Image){
        mainImageView.image = _image
        mainImageView.fitWidthProperty().bind(primaryStage.widthProperty().multiply(0.5));

        // 사진의 비율을 유지하도록 계산하여 설정
        var aspectRatio = mainImageView.image.width / mainImageView.image.height
        mainImageView.fitHeight = mainImageView.fitWidth / aspectRatio

    }

    fun updateDetailView(stringList : ArrayList<String>){
        runLater {
            label.text = "상세 보기"
            if(stringList.size >= 3){
                calenderLabel.text = stringList.get(0)
                loacionLabel.text = stringList.get(1)
                imageLabel.text = stringList.get(2)
            }
        }
    }


    fun initComponent(){
        mainImageView.translateX = 0.0; mainImageView.translateY = 20.0
        fileImageView.translateX = 0.0; fileImageView.translateY = 20.0
        fileNameLabel.translateX = 0.0; fileNameLabel.translateY = 20.0
        textContentStackPane.translateX = 0.0; textContentStackPane.translateY = 20.0
        analysisButton.isVisible = true
        reSelectView.isVisible = true

        textContentStackPane.isVisible = false

        fileNameLabel.isVisible = false

        subImagesView.clear()
        editView.clear()
    }
    fun setMainImage(image : Image, rotation : Int){

        initComponent()
        mainImageView.isVisible = true

        var newImage = imageTool.rotaionImage(image, rotation)
        setMainChage(newImage)
        // 사진의 비율을 유지하도록 계산하여 설정
        var aspectRatio = mainImageView.image.width / mainImageView.image.height
        //mainImageView.fitHeight = mainImageView.fitWidth / aspectRatio

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
        if(!isAnalsys){
            isAnalsys = true
            analysisButton.isVisible = false
            analysisContent.text =""
            analyzingImageAnimation()
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

        analysisButton.isVisible = false

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

    // 분석 중일 때
    fun analyzing(){

        //text 바꾸기
        if(aiContainer.textContent.textCount > 0){
            subImagesView.chageText(aiContainer.textContent.textList[0].data)
            var textData = aiContainer.textContent.textList[0].data
            val stringBuilder = StringBuilder()
            val chunkSize = 20

            for (i in 0 until textData.length step chunkSize) {
                val endIndex = kotlin.math.min(i + chunkSize, textData.length)
                stringBuilder.append(textData.substring(i, endIndex))
                stringBuilder.append("\n")
            }
            textContentLabel.text = stringBuilder.toString()

        } else{
            subImagesView.chageText("")
            textContentLabel.text = ""
        }
        if(aiContainer.audioContent.audio == null){
            subImagesView.setAudioTextLabel("")
        }
        // 이미지 리스트 뷰 바꾸기
        subImagesView.setPictureList(aiContainer.imageContent.pictureList)

        subImagesView.root.isVisible = true
        analysisLabels.isVisible = true
        analysisContent.isVisible = true
    }

    private fun getDetailInfo(): ArrayList<String> {
        var stringList = arrayListOf<String>()

        return stringList
    }

    fun playLogoGif(){
        logoImageView.isVisible = true
        textLogoView.isVisible = false
        homeImage.isVisible = false
        reSelectView.isVisible = false

        CoroutineScope(Dispatchers.IO).launch {
            var inputStream = FileInputStream(imageSourcePath+ "logo2.gif")
            var gifFrames = getGifFrames(inputStream)
            inputStream.close()
            val timeline = Timeline()
            for (i in gifFrames.indices) {
                val image = gifFrames[i]
                val keyFrame = KeyFrame(Duration.millis(150.0*i), EventHandler {
                    logoImageView.image = image
                    logoImageView.style{
                        borderWidth += box(5.px)
                        // 상하좌우 모서리 모두 10px 둥글게 처리
                        borderRadius = multi(box(10.px))
                    }
                })
                timeline.keyFrames.add(keyFrame)
            }
            timeline.cycleCount = 1
            timeline.play()
            //tex_lLogo
            delay(4000)
            textLogoView.isVisible = true
            inputStream = FileInputStream(imageSourcePath+ "text_logo.gif")
            gifFrames = getGifFrames(inputStream)
            inputStream.close()
            val timeline2 = Timeline()
            for (i in gifFrames.indices) {
                val image = gifFrames[i]
                val keyFrame = KeyFrame(Duration.millis(50.0*i), EventHandler {
                    textLogoView.image = image
                })
                timeline2.keyFrames.add(keyFrame)
            }
            timeline2.cycleCount = 1
            timeline2.play()

            timeline2.setOnFinished {
                //homeImage.isVisible = true
                selectedFileVIew.isVisible = true
            }
        }
    }
    fun analyzingImageAnimation(){
        // 돋보기 움짤 재생
        gifImageVeiew.isVisible = true
        val inputStream = FileInputStream(imageSourcePath+ "giphy.gif")
        val gifFrames = getGifFrames(inputStream)
        inputStream.close()

        CoroutineScope(Dispatchers.Default).launch {
            // main Iamge 위로 올라가기
            turnTopAnimation()
        }
        var timeline = Timeline()
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)

            timeline = Timeline()
            for (i in gifFrames.indices) {
                val image = gifFrames[i]
                val keyFrame = KeyFrame(Duration.millis(40.0 * i), EventHandler {
                    gifImageVeiew.image = image
                    analysisButton.setImage(analsImage)
                })
                timeline.keyFrames.add(keyFrame)
            }

            var allTime = 2 + animationTime*(+ AiContainerSingleton.aiContainer.imageContent.pictureCount)

            timeline.cycleCount = (allTime/2.5).roundToInt() + 1
            println(timeline.cycleCount)

            timeline.play()
            // 돋보기 재생 끝

            val list = arrayListOf<String>()
            list.add("사진 ${AiContainerSingleton.aiContainer.imageContent.pictureCount}개 발견!")
            if(AiContainerSingleton.aiContainer.textContent.textCount > 0)
                list.add("텍스트 ${AiContainerSingleton.aiContainer.textContent.textCount}개 발견!")
            if(AiContainerSingleton.aiContainer.audioContent.audio != null)
                list.add("오디오 1개 발견!")

            runLater {
                analysisContent.text =""

                val timeline2 = Timeline()
                var count = (timeline.cycleCount*2.5)/list.size -1
                for(i in 0..list.size -1){
                    println("추가 ${list.get(i)}")
                    val keyFrame = KeyFrame(Duration.seconds(((i+1)*count +1)), {
                        //val newText = list.get(i)
                        StackPane.setMargin(analysisLabels, Insets(150.0, 0.0, 203.0 - 27*(i+1), 00.0))
                        analysisContent.text += list.get(i)+"\n"
                    })
                    timeline2.keyFrames.add(keyFrame)
                }
                timeline2.cycleCount = 1
                timeline2.play()
            }
            // 분석 애니메이션이 끝났을 때
            timeline.setOnFinished {
                finishedAnalysis()
            }

        }


    }

    fun prepareAudio(){// 오디오 준비시키기
        subImagesView.prepareAudio()
    }


    //분석이 끝났을 때
    fun finishedAnalysis(){
        gifImageVeiew.isVisible = false
        analysisLabels.isVisible = false
        isAnalsys = false
        analysisButton.setImage(preAnalsImage)
        // animation
        turnLeftAnimation()
        editView.root.isVisible = true
        formatLabel.isVisible = true

        var isAllInJPEG = false
        formatLabel.apply {
            if (aiContainer.imageContent.pictureList.size > 1 || aiContainer.textContent.textCount > 0 || aiContainer.audioContent.audio != null) {
                text = "All In JPEG"
                isAllInJPEG = true
            } else {
                text = "Basic JPEG"
                isAllInJPEG = false
            }
        }
        editView.update(infoList,isAllInJPEG)
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

    fun turnTopAnimation(){
        println("이미지 올리기")
        val transition1 = TranslateTransition(Duration.seconds(1.5), mainImageView)
        transition1.byY = -(mainImageView.translateY+80) // 왼쪽으로 100픽셀 이동
        transition1.play()

        val transition2 = TranslateTransition(Duration.seconds(1.5), fileImageView)
        transition2.byY = -(fileImageView.translateY+80) // 왼쪽으로 100픽셀 이동
        transition2.play()

        val transition3 = TranslateTransition(Duration.seconds(1.5), fileNameLabel)
        transition3.byY = -(fileNameLabel.translateY+80) // 왼쪽으로 100픽셀 이동
        transition3.play()

        val transition4 = TranslateTransition(Duration.seconds(1.5), textContentStackPane)
        transition4.byY = -(textContentStackPane.translateY+80) // 왼쪽으로 100픽셀 이동
        transition4.play()

    }

    fun turnLeftAnimation(){
        val transition = TranslateTransition(Duration.seconds(1.0), mainImageView)
        transition.byX = +(mainImageView.translateX-170) // 왼쪽으로 100픽셀 이동
        transition.play()

        val transition2 = TranslateTransition(Duration.seconds(1.0), fileImageView)
        transition2.byX = +(mainImageView.translateX-170) // 왼쪽으로 100픽셀 이동
        transition2.play()

//        val transition3 = TranslateTransition(Duration.seconds(1.0), fileNameLabel)
//        transition3.byX = +(mainImageView.translateX-170) // 왼쪽으로 100픽셀 이동
//        transition3.play()

        val transition4 = TranslateTransition(Duration.seconds(1.0), textContentStackPane)
        transition4.byX = +(mainImageView.translateX-170) // 왼쪽으로 100픽셀 이동
        transition4.play()

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


    fun setDetailView(){

        detailView.apply {
            // spacing = 10.0
            setPrefSize(230.0, 160.0)
            setMaxSize(230.0, 160.0)
            style{
                // 둥글게
                paddingAll = 5.0
                background = Background(BackgroundFill(Color.web("#EAEAEADD"), CornerRadii(15.0), Insets.EMPTY))
            }
            add(
                hbox {
                    padding = insets(10,0,0,0)
                    spacing = 5.0
                    // 아이콘 이미지
                    vbox {
                        spacing = 20.0
                        padding = insets(10,3)
                        add(ImageView(Image(File(imageSourcePath +"calender.png").toURI().toURL().toExternalForm())).apply {
                            fitWidth = 25.0
                            isPreserveRatio = true
                        })
                        add(ImageView(Image(File(imageSourcePath +"image.png").toURI().toURL().toExternalForm())).apply {
                            fitWidth = 25.0
                            isPreserveRatio = true
                        })
                        add(ImageView(Image(File(imageSourcePath +"location.png").toURI().toURL().toExternalForm())).apply {
                            fitWidth = 25.0
                            isPreserveRatio = true
                        })
                    }
                    vbox {
                        spacing = 26.0
                        padding = insets(5,10,0,0)
                        calenderLabel =  label {
                            text = ""
                            style{
                                textFill = c("#000000") // 글자 색상 흰색
                                font = Font.font("Inter", FontWeight.BOLD, 11.0)
                            }
                        }
                        imageLabel =  label {
                            text = ""
                            style{
                                textFill = c("#000000") // 글자 색상 흰색
                                font = Font.font("Inter", FontWeight.BOLD, 11.0)
                            }
                        }
                        loacionLabel = label {
                            text = ""
                            style{
                                textFill = c("#000000") // 글자 색상 흰색
                                font = Font.font("Inter", FontWeight.BOLD, 11.0)
                            }
                        }
                    }
                }
            )
        }
    }
}