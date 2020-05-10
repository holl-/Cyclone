package player.fx.control

import javafx.scene.Node
import javafx.stage.Window


class WindowDrag(val window: Window, val node: Node)
{
    private var startX: Double? = null
    private var startY: Double? = null

    init {

        node.setOnMousePressed { e ->
            startX = e.screenX
            startY = e.screenY
        }

        node.setOnMouseDragged { e ->
            if (startX != null) {
                val deltaX = e.screenX - startX!!
                val deltaY = e.screenY - startY!!
                window.x = window.x + deltaX
                window.y = window.y + deltaY
            }
            startX = e.screenX
            startY = e.screenY
        }
    }
}