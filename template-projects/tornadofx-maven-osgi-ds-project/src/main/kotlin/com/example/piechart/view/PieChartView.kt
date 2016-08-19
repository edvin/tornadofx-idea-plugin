package com.example.piechart.view

import org.osgi.service.component.annotations.Component
import tornadofx.View
import tornadofx.data
import tornadofx.find
import tornadofx.osgi.ViewProvider
import tornadofx.piechart

class PieChartView : View() {
    override val root = piechart("Imported Fruits") {
        data("Grapefruit", 12.0)
        data("Oranges", 25.0)
        data("Plums", 10.0)
        data("Pears", 22.0)
        data("Apples", 30.0)
    }

    // Export this view to the Dashboard in another bundle
    @Component class Registration : ViewProvider {
        override val discriminator = "dashboard"
        override fun getView() = find(PieChartView::class)
    }
}