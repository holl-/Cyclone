package player

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.Supplier


//
//
//// TODO can these classes be replaced by bindings?
//
//private open class SynchronizedDoubleProperty (
//        val cloud: Cloud,
//        private val name: String,
//        private val bean: Any,
//        val getter: DoubleSupplier,
//        val setter: DoubleConsumer
//) : DoubleProperty()
//{
//    private var lastValue = 0.0
//    private val changeListeners: MutableList<ChangeListener<in Number?>> = CopyOnWriteArrayList()
//    private val invalidationListeners: MutableList<InvalidationListener> = CopyOnWriteArrayList()
//
//
//    init {
//        invalidated()
//        register()
//    }
//
//    private fun register() {
//        distributed.addDataChangeListener({ e -> Platform.runLater { invalidated() } })
//    }
//
//    fun invalidated() {
//        val newValue = getter.asDouble
//        if (newValue == lastValue) return
//        val oldValue = lastValue
//        lastValue = newValue
//        fireChangedInvalidated(oldValue, newValue)
//    }
//
//    protected fun fireChangedInvalidated(oldValue: Double, newValue: Double) {
//        for (l in changeListeners) {
//            l.changed(this, oldValue, newValue)
//        }
//        for (l in invalidationListeners) {
//            l.invalidated(this)
//        }
//    }
//
//    override fun bind(observable: ObservableValue<out Number?>) {
//        throw UnsupportedOperationException()
//    }
//
//    override fun unbind() {
//        return
//    }
//
//    override fun isBound(): Boolean {
//        return false
//    }
//
//    override fun getBean(): Any {
//        return bean
//    }
//
//    override fun getName(): String {
//        return name
//    }
//
//    override fun addListener(listener: ChangeListener<in Number?>) {
//        changeListeners.add(listener)
//    }
//
//    override fun removeListener(listener: ChangeListener<in Number?>) {
//        changeListeners.remove(listener)
//    }
//
//    override fun addListener(listener: InvalidationListener) {
//        invalidationListeners.add(listener)
//    }
//
//    override fun removeListener(listener: InvalidationListener) {
//        invalidationListeners.remove(listener)
//    }
//
//    override fun get(): Double {
//        return lastValue
//    }
//
//    override fun set(value: Double) {
//        if (value != lastValue) setter.accept(value)
//    }
//}
//
//
class CustomBooleanProperty(
        dependencies: Iterable<Observable>,
        private val getter: BooleanSupplier,
        private val setter: Consumer<Boolean>
) : BooleanProperty()
{
    private var lastValue = false
    private val changeListeners: MutableList<ChangeListener<in Boolean>> = CopyOnWriteArrayList()
    private val invalidationListeners: MutableList<InvalidationListener> = CopyOnWriteArrayList()

    init {
        invalidate()
        for(dependency in dependencies) {
            dependency.addListener { invalidate() }
        }
    }

    private fun invalidate() {
        val newValue = getter.asBoolean
        if (newValue == lastValue) return
        val oldValue = lastValue
        lastValue = newValue
        fireChangedInvalidated(oldValue, newValue)
    }

    private fun fireChangedInvalidated(oldValue: Boolean, newValue: Boolean) {
        for (l in changeListeners) {
            l.changed(this, oldValue, newValue)
        }
        for (l in invalidationListeners) {
            l.invalidated(this)
        }
    }

    override fun bind(observable: ObservableValue<out Boolean>) {
        throw UnsupportedOperationException()
    }

    override fun unbind() {
        return
    }

    override fun isBound(): Boolean {
        return false
    }

    override fun getBean(): Any? {
        return null
    }

    override fun getName(): String? {
        return null
    }

    override fun addListener(listener: ChangeListener<in Boolean>) {
        changeListeners.add(listener)
    }

    override fun removeListener(listener: ChangeListener<in Boolean>) {
        changeListeners.remove(listener)
    }

    override fun addListener(listener: InvalidationListener) {
        invalidationListeners.add(listener)
    }

    override fun removeListener(listener: InvalidationListener) {
        invalidationListeners.remove(listener)
    }

    override fun get(): Boolean {
        return lastValue
    }

    override fun set(value: Boolean) {
        if (value != lastValue) setter.accept(value)
    }
}
//
//private class DistributedReadOnlyStringProperty(private val name: String, private val bean: Any, distributed: Distributed, getter: Supplier<String>) : ReadOnlyStringProperty() {
//    private val distributed: Distributed
//    private val getter: Supplier<String>
//    private var lastValue: String? = null
//    private val changeListeners: MutableList<ChangeListener<in String>> = CopyOnWriteArrayList()
//    private val invalidationListeners: MutableList<InvalidationListener> = CopyOnWriteArrayList()
//    private fun register() {
//        distributed.addDataChangeListener({ e -> Platform.runLater { invalidated() } })
//    }
//
//    protected fun invalidated() {
//        val newValue = getter.get()
//        if (newValue === lastValue) return
//        val oldValue = lastValue
//        lastValue = newValue
//        fireChangedInvalidated(oldValue, newValue)
//    }
//
//    protected fun fireChangedInvalidated(oldValue: String?, newValue: String?) {
//        for (l in changeListeners) {
//            l.changed(this, oldValue, newValue)
//        }
//        for (l in invalidationListeners) {
//            l.invalidated(this)
//        }
//    }
//
//    override fun getBean(): Any {
//        return bean
//    }
//
//    override fun getName(): String {
//        return name
//    }
//
//    override fun addListener(listener: ChangeListener<in String>) {
//        changeListeners.add(listener)
//    }
//
//    override fun removeListener(listener: ChangeListener<in String>) {
//        changeListeners.remove(listener)
//    }
//
//    override fun addListener(listener: InvalidationListener) {
//        invalidationListeners.add(listener)
//    }
//
//    override fun removeListener(listener: InvalidationListener) {
//        invalidationListeners.remove(listener)
//    }
//
//    override fun get(): String {
//        return lastValue!!
//    }
//
//    init {
//        this.distributed = distributed
//        this.getter = getter
//        invalidated()
//        register()
//    }
//}
//
class CustomObjectProperty<T>(
        dependencies: Iterable<Observable>,
        private val getter: Supplier<T?>,
        private val setter: Consumer<T?>
) : ObjectProperty<T?>(), Property<T?>
{
    private var lastValue: T? = null
    private val changeListeners: MutableList<ChangeListener<in T>> = CopyOnWriteArrayList()
    private val invalidationListeners: MutableList<InvalidationListener> = CopyOnWriteArrayList()

    init {
        invalidate()
        for(dependency in dependencies) {
            dependency.addListener { invalidate() }
        }
    }

    fun invalidate() {
        val newValue = getter.get()
        if (newValue == lastValue) return
        val oldValue = lastValue
        lastValue = newValue
        fireChangedInvalidated(oldValue, newValue)
    }

    private fun fireChangedInvalidated(oldValue: T?, newValue: T?) {
        for (l in changeListeners) {
            l.changed(this, oldValue, newValue)
        }
        for (l in invalidationListeners) {
            l.invalidated(this)
        }
    }

    override fun bind(observable: ObservableValue<out T?>) {
        throw UnsupportedOperationException()
    }

    override fun unbind() {
        return
    }

    override fun isBound(): Boolean {
        return false
    }

    override fun getBean(): Any? {
        return null
    }

    override fun getName(): String? {
        return null
    }

    override fun addListener(listener: ChangeListener<in T?>) {
        changeListeners.add(listener)
    }

    override fun removeListener(listener: ChangeListener<in T?>) {
        changeListeners.remove(listener)
    }

    override fun addListener(listener: InvalidationListener) {
        invalidationListeners.add(listener)
    }

    override fun removeListener(listener: InvalidationListener) {
        invalidationListeners.remove(listener)
    }

    override fun get(): T? {
        return lastValue
    }

    override fun set(value: T?) {
        if (lastValue != value) setter.accept(value)
    }
}


class CastToBooleanProperty(val property: Property<Boolean?>) : BooleanPropertyBase()
{
    init {
        bindBidirectional(property)
    }

    override fun getName(): String {
        return property.name
    }

    override fun getBean(): Any? {
        return null
    }

    protected fun finalize() {
        unbindBidirectional(property)
    }
}

class CastToDoubleProperty(val property: Property<Number?>) : DoublePropertyBase()
{
    init {
        bindBidirectional(property)
    }

    override fun getName(): String {
        return property.name
    }

    override fun getBean(): Any? {
        return null
    }

    protected fun finalize() {
        unbindBidirectional(property)
    }
}


class CastToStringProperty(val property: Property<String?>) : StringPropertyBase()
{
    init {
        bindBidirectional(property)
    }

    override fun getName(): String {
        return property.name
    }

    override fun getBean(): Any? {
        return null
    }

    protected fun finalize() {
        unbindBidirectional(property)
    }
}
