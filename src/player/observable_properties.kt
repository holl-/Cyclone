package player

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.*
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import java.util.function.Supplier


class CustomObjectProperty<T>(
        private val dependencies: Iterable<Observable>,
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
        for (dependency in dependencies) {
            if (dependency is ReadOnlyProperty<*>) {
                return dependency.name
            }
        }
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


class FireLater<T>(val value: ObservableValue<T>, observerThread: (r: Runnable) -> Unit) : ObservableValue<T>
{
    private val changeListeners: MutableList<ChangeListener<in T>> = CopyOnWriteArrayList()
    private val invalidationListeners: MutableList<InvalidationListener> = CopyOnWriteArrayList()

    init {
        value.addListener { prop, old, new -> observerThread( Runnable {
            changeListeners.forEach { l -> l.changed(prop, old, new) }
        } ) }
        value.addListener { _ -> observerThread( Runnable {
            invalidationListeners.forEach { l -> l.invalidated(this) }
        } ) }
    }

    override fun removeListener(l: ChangeListener<in T>?) {
        changeListeners.remove(l)
    }

    override fun removeListener(l: InvalidationListener?) {
        invalidationListeners.remove(l)
    }

    override fun addListener(l: InvalidationListener?) {
        l?.let { invalidationListeners.add(l) }
    }

    override fun addListener(l: ChangeListener<in T>?) {
        l?.let { changeListeners.add(l) }
    }

    override fun getValue(): T {
        return value.value
    }

}