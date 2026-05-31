package its.view.javafx;

import its.controller.IssueController;
import its.model.User;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ActionButtonCellFactory
        implements Callback<TableColumn<MainView.IssueRow, Void>, TableCell<MainView.IssueRow, Void>> {

    private final User currentUser;
    private final IssueController issueController;
    private final Runnable refreshCallback;

    public ActionButtonCellFactory(User currentUser, IssueController issueController, Runnable refreshCallback) {
        this.currentUser = currentUser;
        this.issueController = issueController;
        this.refreshCallback = refreshCallback;
    }

    @Override
    public TableCell<MainView.IssueRow, Void> call(TableColumn<MainView.IssueRow, Void> column) {
        return new ActionButtonCell(currentUser, issueController, refreshCallback);
    }
}