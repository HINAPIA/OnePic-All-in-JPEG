package com.example.demo.view

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
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
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
    val editView : EditView by inject()
    val subImagesView : SubImagesView by inject()
    val mainImageView: ImageView =ImageView()

   // val analysisButton = Button()
    var analysisButton : ImageView = ImageView()

    val rightImageView: ImageView = ImageView()
    val gifImageVeiew : ImageView = ImageView()
    var isAnalsys : Boolean = false

    val imageSourcePath = "src/main/kotlin/com/example/demo/resource/"

    lateinit var preAnalsImage : Image
    lateinit var analsImage : Image
    override val root = stackpane{


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
        StackPane.setAlignment(analysisButton, Pos.BOTTOM_RIGHT)
        StackPane.setMargin(subImagesView.root, Insets(0.0, 0.0, 100.0, 50.0))


        // gifImageView
        gifImageVeiew.fitWidth = 100.0
        gifImageVeiew.isPreserveRatio = true

        style {
            //alignment = Pos.CENTER
            backgroundColor = MultiValue(arrayOf(c("#FFFFFF")))
        }

        // subImageView 위치 조정
        subImagesView.root.maxWidth = 830.0
        subImagesView.root.maxHeight = 180.0
        StackPane.setMargin(subImagesView.root, Insets(20.0, 0.0, 0.0, 0.0))
        StackPane.setAlignment(subImagesView.root, Pos.TOP_CENTER)


       // subImagesView.root.prefHeightProperty().bind(this.heightProperty().subtract(170))
        //


        // Main image View
        children.add(mainImageView)
        children.add(gifImageVeiew)

        children.add(analysisButton)

        children.add(subImagesView.root)

        style {
            backgroundColor = MultiValue(arrayOf(c("#232323")))
        }
        // Zoom image View
//        add(AnchorPane().apply {
//            add(rightImageView)
//            // Set constraints to align zoomImageView to the right of mainImageView
//            setTopAnchor(rightImageView, 50.0)
//            setBottomAnchor(rightImageView, 50.0)
//            setLeftAnchor(rightImageView, 0.0)
//            setRightAnchor(rightImageView, 50.0)
//        })

        // 이미지가 로드되면 fitWidth와 fitHeight를 설정
        mainImageView.imageProperty().addListener { _, _, newImage ->
            if (newImage != null) {
                mainImageView.isPreserveRatio = true
                mainImageView.fitWidthProperty().bind(primaryStage.widthProperty().multiply(0.5));
                mainImageView.fitHeight = Region.USE_COMPUTED_SIZE

                style {
                    backgroundColor = MultiValue(arrayOf(c("#232323")))
                }
            }
        }
    }
    fun setMainImage(image : Image, rotation : Int){
        var angle : Int = 360 - rotation
        var bufferedImage = SwingFXUtils.fromFXImage(image, null)
        var newBufferedImage = rotateImageClockwise(bufferedImage, angle.toDouble())
        var newImage = SwingFXUtils.toFXImage(newBufferedImage, null)
        mainImageView.image = newImage

    }

    fun startAnalsys() {
       // root.children.
        if(!isAnalsys){
            isAnalsys = true
            startAnimation()
        }

    }
    fun startAnimation(){
        analyzingImageAnimation()

    }

    fun turnLeftAnimation(imageView : ImageView){
        val transition = TranslateTransition(Duration.seconds(1.0), imageView)
        transition.byX = -(imageView.layoutX-50) // 왼쪽으로 100픽셀 이동
        transition.play()
    }
    fun analyzingImageAnimation(){
        gifImageVeiew.isVisible = true
        val inputStream = FileInputStream(imageSourcePath+ "giphy.gif")
        val gifFrames = getGifFrames(inputStream)
        inputStream.close()

        val timeline = Timeline()
        analyzing()
        for (i in gifFrames.indices) {
            val image = gifFrames[i]
            val keyFrame = KeyFrame(Duration.millis(40.0 * i), EventHandler {
                gifImageVeiew.image = image
                analysisButton.setImage(analsImage)
            })
            timeline.keyFrames.add(keyFrame)
        }
        timeline.cycleCount = 2
        timeline.play()

        // 분성 애니메이션이 끝났을 때
        timeline.setOnFinished {
            finishedAnalysis()
        }

    }
    fun analyzing(){
//        root.children.add(subImagesView.root)
//        StackPane.setAlignment(subImagesView.root, Pos.TOP_CENTER)
//        subImagesView.root.prefWidth(830.0)
//        subImagesView.root.prefHeight(150.0)
//        StackPane.setMargin(subImagesView.root, Insets(70.0, 0.0, 100.0, 50.0))
        subImagesView.setPictureList(aiContainer.imageContent.pictureList)


    }

    fun finishedAnalysis(){
        gifImageVeiew.isVisible = false
        isAnalsys = false
        analysisButton.setImage(preAnalsImage)
        // animation
        turnLeftAnimation(mainImageView)
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

    fun rotateImageClockwise(image: BufferedImage, angle: Double): BufferedImage {
        val radians = Math.toRadians(angle)
        val rotatedWidth = Math.abs(Math.sin(radians) * image.height) + Math.abs(Math.cos(radians) * image.width)
        val rotatedHeight = Math.abs(Math.sin(radians) * image.width) + Math.abs(Math.cos(radians) * image.height)
        val rotatedImage = BufferedImage(rotatedWidth.toInt(), rotatedHeight.toInt(), image.type)
        val graphics2D = rotatedImage.createGraphics()
        graphics2D.translate((rotatedWidth - image.width) / 2.0, (rotatedHeight - image.height) / 2.0)
        graphics2D.rotate(radians, image.width / 2.0, image.height / 2.0)
        graphics2D.drawRenderedImage(image, null)
        graphics2D.dispose()
        return rotatedImage
    }

}