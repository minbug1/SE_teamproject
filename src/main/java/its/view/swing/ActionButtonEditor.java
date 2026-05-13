package its.view.swing;

import java.util.EventObject;

import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import its.model.User;

public class ActionButtonEditor implements TableCellEditor {

    private User currentUser;

    public ActionButtonEditor(User currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value,
            boolean isSelected, int row, int column) {
        // 버튼 에디터 컴포넌트 생성 및 이벤트 처리 로직 구현 (추후 추가)
        return null;
    }

    @Override
    public Object getCellEditorValue() {
        // 편집된 값 반환 로직 구현 (추후 추가)
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isCellEditable'");
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'shouldSelectCell'");
    }

    @Override
    public boolean stopCellEditing() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stopCellEditing'");
    }

    @Override
    public void cancelCellEditing() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cancelCellEditing'");
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addCellEditorListener'");
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeCellEditorListener'");
    }

}
