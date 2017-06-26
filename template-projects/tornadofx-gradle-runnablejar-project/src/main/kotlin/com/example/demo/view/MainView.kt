package com.example.demo.view

import tornadofx.*

class MainView : View("Hello TornadoFX") {
    override val root = hbox {
        label(title) {
            addClass(heading)
        }
    }
}