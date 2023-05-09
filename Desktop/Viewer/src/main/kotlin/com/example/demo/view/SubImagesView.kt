package com.example.demo.view

import com.goldenratio.onepic.PictureModule.AiContainer
import com.goldenratio.onepic.PictureModule.Contents.Picture
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import tornadofx.*

class SubImagesView : View(){
    var pictureListChangeListener : PictureListChangeListener
    private val pictureList: ObservableList<Picture> = FXCollections.observableArrayList()
    private val picturesView: ObservableList<ImageView> = FXCollections.observableArrayList()
    val AiContainer : AiContainer = AiContainerSingleton.aiContainer
    override val root = stackpane {
        val picturesHBox = hbox{
            spacing = 10.0
            padding = Insets(0.0, 10.0, 0.0, 10.0)
        }

        style {
            backgroundColor = MultiValue(arrayOf(Color.web("#313131")))
        }
    }

    init {
        //registering the listener
        pictureListChangeListener = PictureListChangeListener(root)
        pictureList.addListener(pictureListChangeListener)
    }
    fun setPictureList(_pictureList: ArrayList<Picture>) {
        pictureList.removeListener(pictureListChangeListener) // 변경 리스너 제거
        println("갱신 전 : ${_pictureList[0].size}")
        pictureList.setAll(_pictureList)
        println("갱신 완료 : ${pictureList[0].size}")
        pictureList.addListener(pictureListChangeListener) // 변경 리스너 다시 등록
        pictureList.setAll(_pictureList)
    }

    fun viewClear(){
        root.clear()
    }
}


class PictureListChangeListener(private val root: Pane) : ListChangeListener<Picture> {
    private val picturesView = mutableListOf<ImageView>()
    override fun onChanged(change: ListChangeListener.Change<out Picture>?) {
        while (change?.next() == true) {
            if (change.wasAdded()) {
                picturesView.clear()
                for (i in change.from until change.to) {
                    val imageView = ImageView()
                    val pictureByte = AiContainerSingleton.aiContainer.imageContent.getJpegBytes(change.list[i])
                    imageView.image = Image(pictureByte.inputStream())

                    // 프레임 width의 1/6 크기로 설정
                    imageView.fitWidth = root.scene?.width?.div(7.0) ?: 0.0

                    // 사진의 비율을 유지하도록 계산하여 설정
                    val aspectRatio = imageView.image.width / imageView.image.height
                    imageView.fitHeight = imageView.fitWidth / aspectRatio

                    picturesView.add(imageView)
                }

                runLater {
                    root.clear()
                    val picturesHBox = root.hbox {
                        val sp = root.scene?.width?.div(7.0)!! / 8.0
                        spacing = sp
                        padding = Insets(0.0, sp, 0.0, sp)
                        children.clear()
                        for (i in 0..picturesView.size - 1) {
                            children.add(picturesView[i])
                            alignment = Pos.CENTER // StackPane의 자식 노드들을 중앙으로 정렬
                        }
                    }
                    root.add(picturesHBox)
                    picturesHBox.layoutX = (root.width - picturesHBox.width) / 2
                    root.requestLayout()
                }
            }
        }
    }
}
