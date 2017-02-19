package com.example.demo.view

import tornadofx.View
import tornadofx.addClass
import tornadofx.hbox
import tornadofx.label

class MainView : View("Hello TornadoFX Application") {
    override val root = hbox {
        label(title) {
            addClass(heading)
        }
    }
}