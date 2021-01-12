package player.fx.control

import javafx.scene.shape.ArcTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.StrokeLineCap
import javafx.scene.transform.Rotate

class TimeTick(private val position: Double,
               val isMajor: Boolean,
               private val angleSpan: Double,
               relativeWidth: Double,
               cssClass: String) : Path()
{
    private var rotate: Rotate? = null

    init {
        strokeWidth = relativeWidth
        isMouseTransparent = true
        styleClass.add(cssClass)
        strokeLineCap = StrokeLineCap.BUTT
        transforms.add(Rotate().also { rotate = it })

        val sin2 = Math.sin(angleSpan / 2)
        val cos2 = Math.cos(angleSpan / 2)
        elements.clear()
        elements.add(MoveTo(-sin2, -cos2))
        elements.add(ArcTo(1.0, 1.0, 0.0, sin2, -cos2, false, true))
    }

    fun updatePosition(min: Double, max: Double) {
        val frac = (position - min) / (max - min)
        rotate!!.angle = frac * 360
    }
}