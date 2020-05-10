package player.fx.control

import javafx.scene.control.ListCell
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import player.model.data.Speaker


class SpeakerCell : ListCell<Speaker>() {
    override fun updateItem(item: Speaker?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item != null) {
            text = if (item.peer.isLocal) item.name else "${item.name} - ${item.peer.name}"
            val fontWeight = if (item.isDefault) FontWeight.BOLD else FontWeight.NORMAL
            val fontPosture = if (item.peer.isLocal) FontPosture.REGULAR else FontPosture.ITALIC
            font = Font.font(font.family, fontWeight, fontPosture, font.size)
        } else {
            text = null
            graphic = null
        }
    }
}