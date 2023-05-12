package com.example.demo.view

import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.layout.StackPane
import tornadofx.*

class CenterView : View() {

    val mainImageView : MainImageView by inject()
    val editView : EditView by inject()
    val subImagesView : SubImagesView by inject()

    override val root = vbox {

        hbox {
            stackpane{

                this.children.add(mainImageView.root)
            }
            // left
            //this.children.add(mainImageView.metaVBox)
            // right
           // vbox {
                children.add(editView.root)
                prefWidth(300.0)
                style {
                    alignment = Pos.CENTER_RIGHT
                }
          //  }
        }
        add(subImagesView.root)
        editView.root.setPrefSize(300.0, 700.0)
        subImagesView.root.setPrefSize(1200.0, 200.0)

        // center the image view
        StackPane.setAlignment(mainImageView.root, Pos.CENTER)
       // StackPane.setAlignment(editView.root, Pos.CENTER_RIGHT)
        //StackPane.setAlignment(subImagesView.root, Pos.BOTTOM_CENTER)
    }

}