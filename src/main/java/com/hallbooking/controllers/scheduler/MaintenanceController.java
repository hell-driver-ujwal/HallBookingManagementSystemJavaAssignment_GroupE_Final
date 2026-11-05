package com.hallbooking.controllers.scheduler;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Hall;
import com.hallbooking.models.Maintenance;
import com.hallbooking.services.HallService;
import com.hallbooking.services.MaintenanceService;
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

public class MaintenanceController extends BaseController implements Initializable {

    @FXML private TableView<Maintenance>           maintTable;
    @FXML private TableColumn<Maintenance, String> colId, colHall, colStart, colEnd, colRemark, colStatus;

    @FXML private ComboBox<String> comboHall, comboStatus;
    @FXML private TextField        fieldStart, fieldEnd, fieldRemarks;

    private final MaintenanceService maintenanceService = new MaintenanceService();
    private final HallService        hallService        = new HallService();
    private ObservableList<Maintenance> allRecords;
    private Maintenance selectedRecord;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        comboStatus.setItems(FXCollections.observableArrayList("SCHEDULED", "IN_PROGRESS", "DONE"));
        comboStatus.getSelectionModel().selectFirst();
        setupTable(); loadHalls(); loadRecords();
    }

    private void setupTable() {
        colId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMaintId()));
        colHall  .setCellValueFactory(c -> {
            Hall h = hallService.getHallById(c.getValue().getHallId());
            return new SimpleStringProperty(h != null ? h.getHallName() : c.getValue().getHallId());
        });
        colStart .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartDatetime()));
        colEnd   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEndDatetime()));
        colRemark.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRemarks()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void loadHalls() {
        List<String> options = hallService.getActiveHalls().stream()
                .map(h -> h.getHallId() + " — " + h.getHallName())
                .collect(Collectors.toList());
        comboHall.setItems(FXCollections.observableArrayList(options));
        if (!options.isEmpty()) comboHall.getSelectionModel().selectFirst();
    }

    private void loadRecords() {
        allRecords = FXCollections.observableArrayList(maintenanceService.getAllMaintenance());
        maintTable.setItems(allRecords);
    }

    @FXML
    public void onRowSelected() {
        selectedRecord = maintTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) return;
        fieldStart  .setText(selectedRecord.getStartDatetime());
        fieldEnd    .setText(selectedRecord.getEndDatetime());
        fieldRemarks.setText(selectedRecord.getRemarks());
        comboStatus .setValue(selectedRecord.getStatus());
        comboHall.getItems().stream()
                .filter(s -> s.startsWith(selectedRecord.getHallId()))
                .findFirst().ifPresent(comboHall::setValue);
        clearError();
    }

    @FXML
    public void handleAdd() {
        clearError();
        String hallSel = comboHall.getValue();
        if (hallSel == null) { showError("Please select a hall."); return; }
        String start   = fieldStart.getText().trim();
        String end     = fieldEnd.getText().trim();
        String remarks = fieldRemarks.getText().trim();
        if (start.isEmpty() || end.isEmpty() || remarks.isEmpty()) {
            showError("Start, end and remarks are all required."); return;
        }
        try {
            com.hallbooking.utils.DateTimeUtils.parse(start);
            com.hallbooking.utils.DateTimeUtils.parse(end);
        } catch (Exception e) {
            showError("Invalid date format. Use: yyyy-MM-dd HH:mm"); return;
        }
        // ── Booking conflict check ─────────────────────────────────────
        String hallId = hallSel.split(" — ")[0];
        String conflict = maintenanceService.checkBookingConflict(hallId, start, end);
        if (conflict != null) {
            if (!MainApp.showConfirm("Booking Conflict Detected",
                    conflict + "\n\nSchedule maintenance anyway?")) return;
        }
        String err = maintenanceService.addMaintenance(hallId, start, end, remarks);
        if (err != null) { showError(err); return; }
        MainApp.showInfo("Scheduled", "Maintenance scheduled successfully.");
        handleClear(); loadRecords();
    }

    @FXML
    public void handleUpdateStatus() {
        if (selectedRecord == null) { showError("Please select a record."); return; }
        maintenanceService.updateStatus(selectedRecord.getMaintId(), comboStatus.getValue());
        MainApp.showInfo("Updated", "Status updated to " + comboStatus.getValue());
        loadRecords();
    }

    @FXML
    public void handleDelete() {
        if (selectedRecord == null) { showError("Please select a record."); return; }
        String err = maintenanceService.deleteMaintenance(selectedRecord.getMaintId());
        if (err != null) { showError(err); return; }
        MainApp.showInfo("Deleted", "Maintenance record deleted.");
        handleClear(); loadRecords();
    }

    @FXML
    public void handleClear() {
        fieldStart.clear(); fieldEnd.clear(); fieldRemarks.clear();
        comboStatus.getSelectionModel().selectFirst();
        maintTable.getSelectionModel().clearSelection();
        selectedRecord = null; clearError();
    }

    @FXML public void goToDashboard()      { MainApp.switchScene("/fxml/scheduler/dashboard.fxml",     980, 640); }
    @FXML public void goToHallManagement() { MainApp.switchScene("/fxml/scheduler/hall_management.fxml",980, 640); }
    @FXML public void goToAvailability()   { MainApp.switchScene("/fxml/scheduler/availability.fxml",  980, 640); }
    @FXML public void goToMaintenance()    { /* already here */ }
    @FXML public void goToIssues()         { MainApp.switchScene("/fxml/scheduler/issues.fxml",        980, 640); }
}
