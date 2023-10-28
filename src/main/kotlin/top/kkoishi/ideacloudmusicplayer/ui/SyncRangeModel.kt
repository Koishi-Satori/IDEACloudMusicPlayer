package top.kkoishi.ideacloudmusicplayer.ui

import top.kkoishi.ideacloudmusicplayer.Players
import javax.swing.BoundedRangeModel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.EventListenerList
import kotlin.math.absoluteValue

class SyncRangeModel : BoundedRangeModel {
    /**
     * Only one `ChangeEvent` is needed per model instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this".
     */
    @Transient
    private var changeEvent: ChangeEvent? = null

    /**
     * The listeners waiting for model changes.
     */
    private var listenerList = EventListenerList()

    private val players = Players.getInstance()

    override fun getMinimum(): Int = 0

    /**
     * This method is useless.
     */
    @Deprecated(message = "min can not be changed.")
    override fun setMinimum(newMinimum: Int) {
        // do nothing
    }

    override fun getMaximum(): Int {
        val length = players.length()
        return if (length == -1L)
            0
        else
            length.toInt().absoluteValue
    }

    /**
     * This method is useless.
     */
    @Deprecated(message = "max can not be changed.")
    override fun setMaximum(newMaximum: Int) {
        // do nothing
    }

    override fun getValue(): Int {
        val value = players.progress()
        return if (value.isNaN())
            0
        else
            value.toInt().absoluteValue
    }

    /**
     * This method is useless.
     */
    @Deprecated(message = "value can not be changed.")
    override fun setValue(newValue: Int) {
        // do nothing
    }

    override fun setValueIsAdjusting(b: Boolean) {
    }

    override fun getValueIsAdjusting(): Boolean = players.isAdjusting()

    override fun getExtent(): Int = 0

    override fun setExtent(newExtent: Int) {
    }

    override fun setRangeProperties(value: Int, extent: Int, min: Int, max: Int, adjusting: Boolean) {
        fireStateChanged()
    }

    override fun addChangeListener(x: ChangeListener?) {
        listenerList.add(ChangeListener::class.java, x)
    }

    override fun removeChangeListener(x: ChangeListener?) {
        listenerList.remove(ChangeListener::class.java, x)
    }

    private fun fireStateChanged() {
        val listeners = listenerList.listenerList
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === ChangeListener::class.java) {
                if (changeEvent == null) {
                    changeEvent = ChangeEvent(this)
                }
                (listeners[i + 1] as ChangeListener).stateChanged(changeEvent)
            }
            i -= 2
        }
    }
}