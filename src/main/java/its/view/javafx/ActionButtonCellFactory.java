package its.view.javafx;

import its.model.User;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Swing의 ActionButtonRenderer에 해당.
 * TableColumn에 ActionButtonCell을 공급하는 팩토리.
 */
public class ActionButtonCellFactory
        implements Callback<TableColumn<MainView.IssueRow, Void>, TableCell<MainView.IssueRow, Void>> {

    private final User currentUser;
    private final Runnable refreshCallback;

    public ActionButtonCellFactory(User currentUser, Runnable refreshCallback) {
        this.currentUser = currentUser;
        this.refreshCallback = refreshCallback;
    }

    @Override
    public TableCell<MainView.IssueRow, Void> call(TableColumn<MainView.IssueRow, Void> column) {
        return new ActionButtonCell(currentUser, refreshCallback);
    }
}