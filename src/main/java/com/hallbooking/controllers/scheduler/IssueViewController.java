package com.hallbooking.controllers.scheduler;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Issue;
import com.hallbooking.models.IssueAssignment;
import com.hallbooking.models.Notification;
import com.hallbooking.services.IssueService;
import com.hallbooking.services.NotificationService;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class IssueViewController extends BaseController implements Initializable {

    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<IssueAssignment>           assignTable;
    @FXML private TableColumn<IssueAssignment, String> colAssignId, colIssueId, colHall,
                                                        colTitle, colAssignedAt,
                                                        colStatus, colCompleted;

    @FXML private Label    lblIssueId, lblHall, lblCustomer, lblAssignedAt;
    @FXML private TextArea lblDescription;

    /** Manager's customer-facing note (read-only for scheduler) */
    @FXML private TextArea lblCustomerNote;
    /** Manager's private instructions to this scheduler (read-only) */
    @FXML private TextArea lblSchedulerInstructions;
    /** Scheduler's resolution note sent back to the manager */
    @FXML private TextArea fieldResolution;

    // Notifications panel (new assignments)
    @FXML private javafx.scene.layout.VBox  notifPanel;
    @FXML private Label                     lblNotifBadge;
    @FXML private ListView<String>          notifList;

    private final IssueService        issueService = new IssueService();
    private final NotificationService notifService = new NotificationService();

    private ObservableList<IssueAssignment> allAssignments;
    private IssueAssignment selectedAssignment;
    private String myStaffId;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        myStaffId = SessionManager.getInstance().getCurrentStaff().getStaffId();
        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Completed"));
        statusFilter.getSelectionModel().selectFirst();
        setupTable();
        loadAssignments();
        loadNotifications();
    }

    // ── Notifications ────────────────────────────────────────────────

    private void loadNotifications() {
        if (notifList == null) return;
        List<Notification> unread = notifService.getUnreadForRecipient(myStaffId);
        long count = unread.size();

        if (lblNotifBadge != null) {
            if (count > 0) {
                lblNotifBadge.setText(count + " new");
                lblNotifBadge.setVisible(true);
                lblNotifBadge.setManaged(true);
            } else {
                lblNotifBadge.setText("");
                lblNotifBadge.setVisible(false);
                lblNotifBadge.setManaged(false);
            }
        }

        if (notifPanel != null) {
            notifPanel.setVisible(count > 0);
            notifPanel.setManaged(count > 0);
        }

        ObservableList<String> items = FXCollections.observableArrayList(
                unread.stream().map(n -> "🔔 " + n.getMessage()).collect(Collectors.toList()));
        notifList.setItems(items);
    }

    @FXML
    public void handleDismissNotifications() {
        notifService.markAllRead(myStaffId);
        loadNotifications();
    }

    // ── Table ────────────────────────────────────────────────────────

    private void setupTable() {
        colAssignId .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAssignId()));
        colIssueId  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIssueId()));
        colHall     .setCellValueFactory(c -> {
            Issue issue = issueService.getIssueById(c.getValue().getIssueId());
            return new SimpleStringProperty(issue != null ? issue.getHallId() : "—");
        });
        colTitle    .setCellValueFactory(c -> {
            Issue issue = issueService.getIssueById(c.getValue().getIssueId());
            return new SimpleStringProperty(issue != null ? issue.getTitle() : "—");
        });
        colAssignedAt.setCellValueFactory(c -> new SimpleStringProperty(
                DateTimeUtils.toDisplayFormat(c.getValue().getAssignedAt())));
        colStatus   .setCellValueFactory(c -> new SimpleStringProperty(
                "NULL".equals(c.getValue().getCompletedAt()) ? "Pending" : "Completed"));
        colCompleted.setCellValueFactory(c -> new SimpleStringProperty(
                "NULL".equals(c.getValue().getCompletedAt()) ? "—"
                        : DateTimeUtils.toDisplayFormat(c.getValue().getCompletedAt())));
    }

    private void loadAssignments() {
        List<IssueAssignment> mine = issueService.getAssignmentsByStaff(myStaffId);
        allAssignments = FXCollections.observableArrayList(mine);
        applyFilter();
    }

    @FXML
    public void applyFilter() {
        String f = statusFilter.getValue();
        if (f == null || f.equals("All"))     assignTable.setItems(allAssignments);
        else if (f.equals("Pending"))         assignTable.setItems(allAssignments.filtered(
                a -> "NULL".equals(a.getCompletedAt())));
        else                                  assignTable.setItems(allAssignments.filtered(
                a -> !"NULL".equals(a.getCompletedAt())));
    }

    @FXML
    public void onRowSelected() {
        selectedAssignment = assignTable.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null) return;

        Issue issue = issueService.getIssueById(selectedAssignment.getIssueId());
        if (issue != null) {
            lblIssueId  .setText(issue.getIssueId() + "  (" + issue.getStatus() + ")");
            lblHall     .setText(issue.getHallId());
            lblCustomer .setText(issue.getCustomerId());
            lblDescription.setText(issue.getDescription());
        }
        lblAssignedAt.setText(DateTimeUtils.toDisplayFormat(selectedAssignment.getAssignedAt()));

        // Show manager's customer-facing note (read-only context)
        if (lblCustomerNote != null)
            lblCustomerNote.setText(selectedAssignment.getCustomerNote());

        // Show manager's private instructions to scheduler
        if (lblSchedulerInstructions != null)
            lblSchedulerInstructions.setText(selectedAssignment.getSchedulerNote());

        // If already completed, show the resolution note that was sent
        fieldResolution.clear();
        if (!"NULL".equals(selectedAssignment.getCompletedAt())) {
            // schedulerNote now holds the resolution that was sent to manager
            fieldResolution.setText(selectedAssignment.getSchedulerNote());
            fieldResolution.setEditable(false);
        } else {
            fieldResolution.setEditable(true);
        }

        clearError();
    }

    @FXML
    public void handleMarkDone() {
        clearError();
        if (selectedAssignment == null)
            { showError("Please select an assignment."); return; }
        if (!"NULL".equals(selectedAssignment.getCompletedAt()))
            { showError("Already completed."); return; }
        String resolution = fieldResolution.getText().trim();
        if (resolution.isEmpty())
            { showError("Please enter a resolution note before marking as done."); return; }

        issueService.completeAssignment(selectedAssignment.getAssignId(), resolution);
        issueService.updateIssueStatus(selectedAssignment.getIssueId(), "DONE");

        MainApp.showInfo("Done",
                "Issue " + selectedAssignment.getIssueId()
                + " marked as resolved. The manager has been notified.");
        handleClear();
        loadAssignments();
    }

    @FXML
    public void handleClear() {
        lblIssueId.setText("—");
        lblHall.setText("—");
        lblCustomer.setText("—");
        lblAssignedAt.setText("—");
        lblDescription.clear();
        if (lblCustomerNote         != null) lblCustomerNote.clear();
        if (lblSchedulerInstructions != null) lblSchedulerInstructions.clear();
        fieldResolution.clear();
        fieldResolution.setEditable(true);
        clearError();
        assignTable.getSelectionModel().clearSelection();
        selectedAssignment = null;
    }

    @FXML public void goToDashboard()      { MainApp.switchScene("/fxml/scheduler/dashboard.fxml",      980, 640); }
    @FXML public void goToHallManagement() { MainApp.switchScene("/fxml/scheduler/hall_management.fxml", 980, 640); }
    @FXML public void goToAvailability()   { MainApp.switchScene("/fxml/scheduler/availability.fxml",   980, 640); }
    @FXML public void goToMaintenance()    { MainApp.switchScene("/fxml/scheduler/maintenance.fxml",    980, 640); }
    @FXML public void goToIssues()         { /* already here */ }
}
