package extensions

import cloud.Cloud
import javafx.scene.Node
import javafx.stage.Stage
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


abstract class CycloneExtension(val name: String, val description: String, val version: String, val canShow: Boolean)
{
    abstract fun activate(cloud: Cloud)
    abstract fun deactivate()

    abstract fun load(stream: ObjectInputStream)
    abstract fun save(stream: ObjectOutputStream)

    abstract fun show(stage: Stage)

    abstract fun settings(): Node?

}