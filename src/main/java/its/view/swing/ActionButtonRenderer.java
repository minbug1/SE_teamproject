package its.view.swing;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ActionButtonRenderer implements TableCellRenderer {

    private final JButton button = new JButton("Action");

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        button.setText(value == null ? "Action" : value.toString());
        return button;
    }
}
