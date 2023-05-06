package com.example.demo.app

import com.example.demo.view.ImageViewer
import tornadofx.*

class MyApp: App(ImageViewer::class, Styles::class)

fun main(args: Array<String>) {
    launch<MyApp>(args)
}