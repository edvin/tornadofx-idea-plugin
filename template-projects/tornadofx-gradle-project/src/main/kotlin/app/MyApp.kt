package app

import app.Styles
import view.MainView
import tornadofx.App
import tornadofx.importStylesheet

class MyApp: App() {
    override val primaryView = MainView::class

    init {
        importStylesheet(Styles::class)
    }
}