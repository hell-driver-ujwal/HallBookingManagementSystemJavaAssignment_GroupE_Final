package com.hallbooking.controllers.scheduler;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Hall;
import com.hallbooking.models.Maintenance;
import com.hallbooking.services.*;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class SchedulerDashboardController extends BaseController implements Initializable {

    @FXML private Label statHalls, statSlots, statMaint, statToday, statIssues;

    @FXML private TableView<Hall>              hallsTable;
    @FXML private TableColumn<Hall, String>    colId, colName, colType, colCapacity, colRate, colStatus;

    @FXML private TableView<Maintenance>           maintTable;
    @FXML private TableColumn<Maintenance, String> mColId, mColHall, mColStart, mColEnd, mColRemark, mColStatus;

    private final HallService         hallService         = new HallService();
    private final MaintenanceService  maintenanceService  = new MaintenanceService();
    private final AvailabilityService availabilityService = new AvailabilityService();
    private final BookingService      bookingService      = new BookingService();
    private final IssueService        issueService        = new IssueService();

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        setupHallsTable();
        setupMaintTable();
        loadData();
    }

    private void setupHallsTable() {
        colId      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        colName    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallName()));
        colType    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallType()));
        colCapacity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCapacity() + " pax"));
        colRate    .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getRatePerHour())));
        colStatus  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isActive() ? "Active" : "Inactive"));
    }

    private void setupMaintTable() {
        mColId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMaintId()));
        mColHall  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        mColStart .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartDatetime()));
        mColEnd   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEndDatetime()));
        mColRemark.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRemarks()));
        mColStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void loadData() {
        var halls  = hallService.getActiveHalls();
        var active = maintenanceService.getActiveMaintenance();
        var slots  = availabilityService.getAllSlots().size();
        long today = bookingService.getAllBookings().stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus())
                        && b.getEventStart().startsWith(LocalDate.now().toString()))
                .count();
        String myStaffId = SessionManager.getInstance().getCurrentStaff().getStaffId();
        long pendingIssues = issueService.getAssignmentsByStaff(myStaffId).stream()
                .filter(a -> "NULL".equals(a.getCompletedAt())).count();

        statHalls .setText(String.valueOf(halls.size()));
        statSlots .setText(String.valueOf(slots));
        statMaint .setText(String.valueOf(active.size()));
        statToday .setText(String.valueOf(today));
        if (statIssues != null) statIssues.setText(String.valueOf(pendingIssues));

        hallsTable.setItems(FXCollections.observableArrayList(halls));
        maintTable.setItems(FXCollections.observableArrayList(active));
    }

    @FXML public void goToDashboard()      { MainApp.switchScene("/fxml/scheduler/dashboard.fxml",     980, 640); }
    @FXML public void goToHallManagement() { MainApp.switchScene("/fxml/scheduler/hall_management.fxml",980, 640); }
    @FXML public void goToAvailability()   { MainApp.switchScene("/fxml/scheduler/availability.fxml",  980, 640); }
    @FXML public void goToMaintenance()    { MainApp.switchScene("/fxml/scheduler/maintenance.fxml",   980, 640); }
    @FXML public void goToIssues()         { MainApp.switchScene("/fxml/scheduler/issues.fxml",        980, 640); }
}
