package top.kkoishi.ideacloudmusicplayer.ui

import javax.swing.table.AbstractTableModel

abstract class DataTableModel(private val header: Array<String>, val dataSupplier: (Int, Int) -> String) :
    AbstractTableModel() {

    abstract fun dataRowCount(): Int

    override fun getRowCount(): Int = 1 + dataRowCount()

    override fun getColumnCount(): Int = header.size

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any =
        if (rowIndex == 0)
            header[columnIndex]
        else
            dataSupplier(rowIndex, columnIndex)

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
}