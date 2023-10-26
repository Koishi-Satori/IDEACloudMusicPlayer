package top.kkoishi.ideacloudmusicplayer.ui

import com.intellij.openapi.ui.popup.*
import com.intellij.util.ui.components.JBComponent
import javax.swing.Icon

class ComponentListPopupStep(
    private val title: String,
    private val components: MutableList<Pair<String, JBComponent<*>>>,
    private val finalRunnable: Runnable,
    private val onChosen: (JBComponent<*>) -> Unit,
) :
    ListPopupStep<JBComponent<*>> {
    override fun getTitle(): String = title

    override fun canceled() {
    }

    override fun isMnemonicsNavigationEnabled(): Boolean = false

    override fun getMnemonicNavigationFilter(): MnemonicNavigationFilter<JBComponent<*>>? = null

    override fun isSpeedSearchEnabled(): Boolean = false

    override fun getSpeedSearchFilter(): SpeedSearchFilter<JBComponent<*>>? = null

    override fun isAutoSelectionEnabled(): Boolean = false

    override fun getFinalRunnable(): Runnable = finalRunnable

    override fun getValues(): MutableList<JBComponent<*>> = components.map { it.second }.toMutableList()

    override fun getDefaultOptionIndex(): Int = 0

    override fun getSeparatorAbove(value: JBComponent<*>?): ListSeparator = ListSeparator()

    override fun getTextFor(value: JBComponent<*>?): String {
        if (value == null)
            return ""
        return components
            .filter { it.second == value }.ifEmpty { return "" }
            .first()
            .first
    }

    override fun getIconFor(value: JBComponent<*>?): Icon? = null

    override fun isSelectable(value: JBComponent<*>?): Boolean = true

    override fun hasSubstep(selectedValue: JBComponent<*>?): Boolean = false

    override fun onChosen(selectedValue: JBComponent<*>?, finalChoice: Boolean): PopupStep<*> {
        if (selectedValue != null)
            onChosen(selectedValue)
        return PopupStep.FINAL_CHOICE
    }
}