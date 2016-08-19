package com.example.demo.view

import com.example.demo.app.Styles.Companion.heading
import javafx.scene.layout.HBox
import tornadofx.View
import tornadofx.addClass
import tornadofx.label

class MainView : View() {
    override val root = HBox()

    init {
        title = "Hello TornadoFX Application"

        with (root) {
            label(title) {
                addClass(heading)
            }
        }
    }
}