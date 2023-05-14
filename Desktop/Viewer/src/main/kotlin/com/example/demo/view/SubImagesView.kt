package com.example.demo.view

import com.goldenratio.onepic.PictureModule.AiContainer
import com.goldenratio.onepic.PictureModule.Contents.Picture
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ScrollBar
import javafx.scene.control.ScrollPane
import javafx.scene.effect.DropShadow
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.layout.HBox.setMargin
import javafx.scene.paint.Color
import tornadofx.*
import tornadofx.Stylesheet.Companion.content
import tornadofx.Stylesheet.Companion.root
import tornadofx.WizardStyles.Companion.content

class SubImagesView : View(){
    var pictureListChangeListener : PictureListChangeListener
    private val pictureList: ObservableList<Picture> = FXCollections.observableArrayList()
    private var picturesHBox: HBox = HBox()
    val AiContainer : AiContainer = AiContainerSingleton.aiContainer
    var picturesPane : HBox = HBox()
    var picturesScrollPane : ScrollPane= ScrollPane()

    override val root = hbox {
        // 사진들을 담은 검은 박스
//        picturesHBox= hbox {
//            setPrefSize(540.0, 130.0)
//            maxHeight = 130.0
//            maxWidth = 540.0
//            setMargin(this@hbox, Insets(25.0, 14.0, 0.0, 14.0))
//            style{
//                paddingAll = 10.0
//                background = Background(BackgroundFill(Color.web("#020202"), CornerRadii(15.0), Insets.EMPTY))
//                effect = DropShadow(10.0, 0.0, 5.0, javafx.scene.paint.Color.GRAY).apply {
//                    blurType = javafx.scene.effect.BlurType.ONE_PASS_BOX
//                }
//            }
//        }
        picturesScrollPane = scrollpane {
            content = picturesPane
            //setfSize(540.0, 130.0)
            setMinSize(540.0, 130.0)
            setMaxSize(540.0, 130.0)
            setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
            setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
            setMargin(this@scrollpane, Insets(25.0, 14.0, 0.0, 14.0))

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
//
        }

         picturesPane.apply{
            setMinSize(540.0, 130.0)
             //setMaxSize(540.0, 130.0)

             padding = Insets(10.0)
            style{
                paddingAll = 10.0
                background = Background(BackgroundFill(Color.web("#020202"), CornerRadii(15.0), Insets.EMPTY))
                effect = DropShadow(10.0, 0.0, 5.0, javafx.scene.paint.Color.GRAY).apply {
                    blurType = javafx.scene.effect.BlurType.ONE_PASS_BOX
                }
            }
             setMargin(this@apply, Insets(0.0, 20.0, 0.0, 20.0))
             //alignment = Pos.CENTER // StackPane의 자식 노드들을 중앙으로 정렬
         }
//        picturesHBox = hbox {
//            setPrefSize(540.0, 130.0)
//            maxHeight = 130.0
//            maxWidth = 540.0
//            style{
//                paddingAll = 10.0
//                background = Background(BackgroundFill(Color.web("#020202"), CornerRadii(15.0), Insets.EMPTY))
//                effect = DropShadow(10.0, 0.0, 5.0, javafx.scene.paint.Color.GRAY).apply {
//                    blurType = javafx.scene.effect.BlurType.ONE_PASS_BOX
//                }
//            }
           // setMargin(this@hbox, Insets(25.0, 14.0, 0.0, 14.0))
//

      //  }


        // 흰색 박스
        style {
            background = Background(BackgroundFill(Color.web("#F3F3F3"), CornerRadii(10.0), Insets.EMPTY))
            setMargin(this@hbox, Insets(25.0, 14.0, 0.0, 14.0))
            // backgroundColor = MultiValue(arrayOf(Color.web("#F3F3F3")))

        }
    }

    init {
        //registering the listener
        println("init()")
        pictureListChangeListener = PictureListChangeListener(root, picturesPane)
        pictureList.addListener(pictureListChangeListener)
    }
    fun setPictureList(_pictureList: ArrayList<Picture>) {
        println("setPictureList 호출")
        pictureList.removeListener(pictureListChangeListener) // 변경 리스너 제거
        pictureList.setAll(_pictureList)
        pictureList.addListener(pictureListChangeListener) // 변경 리스너 다시 등록
        pictureList.setAll(_pictureList)
    }

    fun viewClear(){
        root.clear()
    }



    class PictureListChangeListener(private val root : Pane, private val picturesPane: HBox) : ListChangeListener<Picture> {
        private val picturesView = mutableListOf<ImageView>()
        override fun onChanged(change: ListChangeListener.Change<out Picture>?) {
            while (change?.next() == true) {
                // 분석한 사진들로 ImageVIew채우기
                if (change.wasAdded()) {
                    picturesView.clear()
                    for (i in change.from until change.to) {
                        val imageView = ImageView()
                        val pictureByte = AiContainerSingleton.aiContainer.imageContent.getJpegBytes(change.list[i])
                        imageView.image = Image(pictureByte.inputStream())

                        // 프레임 width의 1/6 크기로 설정
                        // imageView.fitWidth = root.scene?.width?.div(8.0) ?: 0.0
                        imageView.fitWidth = 120.0

                        // 사진의 비율을 유지하도록 계산하여 설정
                        val aspectRatio = imageView.image.width / imageView.image.height
                        imageView.fitHeight = imageView.fitWidth / aspectRatio

                        picturesView.add(imageView)
                    }

                    runLater {
                        // 검은 창에 이미지 띄우기
                        picturesPane.children.clear()
                        picturesPane.apply {
                            for (i in 0..picturesView.size - 1) {
                                picturesPane.children.add(picturesView[i])
                            }
                            alignment = Pos.CENTER
                            spacing = 30.0
                            padding = Insets(10.0)
                            setMargin(this@apply, Insets(0.0, 20.0, 0.0, 20.0))

                        }
                        root.requestLayout()
                    }


                }
            }
        }
    }

}
