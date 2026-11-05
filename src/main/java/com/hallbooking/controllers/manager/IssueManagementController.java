package com.hallbooking.controllers.manager;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Issue;
import com.hallbooking.models.IssueAssignment;
import com.hallbooking.models.Notification;
import com.hallbooking.models.Staff;
import com.hallbooking.services.IssueService;
import com.hallbooking.services.NotificationService;
import com.hallbooking.services.UserService;
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

public class IssueManagementController extends BaseController implements Initializable {

    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<Issue>           issueTable;
    @FXML private TableColumn<Issue, String> colId, colCust, colHall, colTitle,
                                             colDate, colStatus, colAssign, colPriority;

    @FXML private Label    lblIssueId, lblTitle, lblHall, lblCustomer;
    @FXML private TextArea lblDescription;
    @FXML private ComboBox<String> comboStaff, comboStatus, comboPriority;

    /** Note visible to the customer */
    @FXML private TextArea fieldCustomerNote;
    /** Private instructions sent to the scheduler */
    @FXML private TextArea fieldSchedulerNote;

    // Staff performance labels
    @FXML private Label lblPerfAssigned, lblPerfCompleted, lblPerfAvgHours;

    // Overdue alert banner
    @FXML private javafx.scene.layout.VBox overdueAlert;
    @FXML private Label lblOverdueCount;

    // Notifications panel
    @FXML private javafx.scene.layout.VBox  notifPanel;
    @FXML private Label                     lblNotifBadge;
    @FXML private ListView<String>          notifList;

    private final IssueService        issueService  = new IssueService();
    private final UserService         userService   = new UserService();
    private final NotificationService notifService  = new NotificationService();

    private ObservableList<Issue> allIssues;
    private Issue selectedIssue;
    private List<Staff> schedulers;
    private String myManagerId;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        myManagerId = SessionManager.getInstance().getCurrentStaff().getStaffId();

        statusFilter.setItems(FXCollections.observableArrayList(
                "All", "OPEN", "IN_PROGRESS", "DONE", "CLOSED", "CANCELLED"));
        statusFilter.getSelectionModel().selectFirst();

        comboStatus.setItems(FXCollections.observableArrayList(
                "OPEN", "IN_PROGRESS", "DONE", "CLOSED", "CANCELLED"));
        comboStatus.getSelectionModel().selectFirst();

        if (comboPriority != null)
            comboPriority.setItems(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH"));

        loadSchedulers();
        setupTable();
        loadIssues();
        checkOverdueAlert();
        loadNotifications();
    }

    // ── Notifications ────────────────────────────────────────────────

    private void loadNotifications() {
        if (notifList == null) return;
        List<Notification> unread = notifService.getUnreadForRecipient(myManagerId);
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
        notifService.markAllRead(myManagerId);
        loadNotifications();
    }

    // ── Schedulers / performance ─────────────────────────────────────

    private void loadSchedulers() {
        schedulers = userService.getSchedulers();
        List<String> options = schedulers.stream()
                .map(s -> s.getStaffId() + " — " + s.getFullName())
                .collect(Collectors.toList());
        comboStaff.setItems(FXCollections.observableArrayList(options));
        if (!options.isEmpty()) comboStaff.getSelectionModel().selectFirst();
        updateStaffPerformance();
    }

    private void setupTable() {
        colId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIssueId()));
        colCust  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerId()));
        colHall  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        colTitle .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReportedAt().substring(0, 10)));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colAssign.setCellValueFactory(c -> {
            IssueAssignment a = issueService.getAssignmentByIssue(c.getValue().getIssueId());
            return new SimpleStringProperty(a != null ? a.getAssignedToStaffId() : "Unassigned");
        });
        if (colPriority != null)
            colPriority.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority()));
    }

    private void loadIssues() {
        allIssues = FXCollections.observableArrayList(issueService.getAllIssues());
        issueTable.setItems(allIssues);
        issueTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Issue item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty)          setStyle("");
                else if (item.isOverdue())          setStyle("-fx-background-color:#fff3cd;");
                else if ("HIGH".equals(item.getPriority()) && !item.isResolved())
                                                    setStyle("-fx-background-color:#fce8e8;");
                else                                setStyle("");
            }
        });
    }

    private void checkOverdueAlert() {
        if (overdueAlert == null) return;
        List<Issue> overdue = issueService.getOverdueIssues();
        if (!overdue.isEmpty()) {
            overdueAlert.setVisible(true);
            overdueAlert.setManaged(true);
            if (lblOverdueCount != null)
                lblOverdueCount.setText(overdue.size() + " issue(s) have been IN_PROGRESS for over 3 days!");
        } else {
            overdueAlert.setVisible(false);
            overdueAlert.setManaged(false);
        }
    }

    private void updateStaffPerformance() {
        if (lblPerfAssigned == null) return;
        String sel = comboStaff.getValue();
        if (sel == null) return;
        String staffId = sel.split(" — ")[0];
        IssueService.StaffPerformance perf = issueService.getPerformanceByStaff(staffId);
        lblPerfAssigned .setText(String.valueOf(perf.assigned));
        lblPerfCompleted.setText(String.valueOf(perf.completed));
        lblPerfAvgHours .setText(String.format("%.1f hrs", perf.avgResolutionHours));
    }

    @FXML public void onStaffSelected() { updateStaffPerformance(); }

    @FXML
    public void onRowSelected() {
        selectedIssue = issueTable.getSelectionModel().getSelectedItem();
        if (selectedIssue == null) return;
        lblIssueId   .setText(selectedIssue.getIssueId());
        lblTitle     .setText(selectedIssue.getTitle());
        lblHall      .setText(selectedIssue.getHallId());
        lblCustomer  .setText(selectedIssue.getCustomerId());
        lblDescription.setText(selectedIssue.getDescription());
        comboStatus  .setValue(selectedIssue.getStatus());
        if (comboPriority != null) comboPriority.setValue(selectedIssue.getPriority());

        fieldCustomerNote.clear();
        fieldSchedulerNote.clear();
        clearError();

        IssueAssignment existing = issueService.getAssignmentByIssue(selectedIssue.getIssueId());
        if (existing != null) {
            fieldCustomerNote .setText(existing.getCustomerNote());
            fieldSchedulerNote.setText(existing.getSchedulerNote());
            comboStaff.getItems().stream()
                    .filter(s -> s.startsWith(existing.getAssignedToStaffId()))
                    .findFirst().ifPresent(comboStaff::setValue);
        }
        updateStaffPerformance();
    }

    @FXML
    public void handleAssign() {
        clearError();
        if (selectedIssue == null)              { showError("Please select an issue."); return; }
        String staffSel      = comboStaff.getValue();
        String customerNote  = fieldCustomerNote.getText().trim();
        String schedulerNote = fieldSchedulerNote.getText().trim();
        if (staffSel == null)            { showError("Please select a scheduler to assign."); return; }
        if (customerNote.isEmpty())      { showError("A response to the customer is required."); return; }
        if (schedulerNote.isEmpty())     { showError("Instructions for the scheduler are required."); return; }

        if (comboPriority != null && comboPriority.getValue() != null)
            issueService.updateIssuePriority(selectedIssue.getIssueId(), comboPriority.getValue());

        String staffId = staffSel.split(" — ")[0];
        String err = issueService.assignIssue(selectedIssue.getIssueId(), staffId,
                customerNote, schedulerNote);
        if (err != null) { showError(err); return; }

        MainApp.showInfo("Assigned", "Issue " + selectedIssue.getIssueId()
                + " assigned to " + staffSel + ". Scheduler has been notified.");
        handleClear();
        loadIssues();
        checkOverdueAlert();
        loadNotifications();
    }

    @FXML
    public void handleUpdateStatus() {
        clearError();
        if (selectedIssue == null) { showError("Please select an issue."); return; }
        if (comboPriority != null && comboPriority.getValue() != null)
            issueService.updateIssuePriority(selectedIssue.getIssueId(), comboPriority.getValue());
        issueService.updateIssueStatus(selectedIssue.getIssueId(), comboStatus.getValue());
        MainApp.showInfo("Updated", "Issue updated successfully.");
        loadIssues();
        checkOverdueAlert();
    }

    @FXML
    public void applyFilter() {
        String f = statusFilter.getValue();
        if (f == null || f.equals("All")) { issueTable.setItems(allIssues); return; }
        issueTable.setItems(allIssues.filtered(i -> i.getStatus().equals(f)));
    }

    @FXML
    public void handleClear() {
        lblIssueId.setText("—"); lblTitle.setText("—");
        lblHall.setText("—"); lblCustomer.setText("—");
        lblDescription.clear();
        fieldCustomerNote.clear();
        fieldSchedulerNote.clear();
        comboStatus.getSelectionModel().selectFirst();
        if (comboPriority != null) comboPriority.getSelectionModel().select("MEDIUM");
        issueTable.getSelectionModel().clearSelection();
        selectedIssue = null;
        clearError();
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/manager/dashboard.fxml", 980, 640); }
    @FXML public void goToSales()     { MainApp.switchScene("/fxml/manager/sales.fxml",    980, 640); }
    @FXML public void goToIssues()    { /* already here */ }
}
