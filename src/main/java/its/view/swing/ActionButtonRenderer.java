package its.view.swing;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ActionButtonRenderer implements TableCellRenderer {

    private static final String ACTION_MENU_TEXT = "\u22EF";

    private final JButton button = new JButton(ACTION_MENU_TEXT);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        button.setText(ACTION_MENU_TEXT);
        return button;
    }
}
