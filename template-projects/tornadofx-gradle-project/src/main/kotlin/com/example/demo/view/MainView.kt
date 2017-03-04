package com.example.demo.view

import tornadofx.*

class MainView : View("Hello TornadoFX Application") {
    override val root = hbox {
        label(title) {
            addClass(heading)
        }
    }
}