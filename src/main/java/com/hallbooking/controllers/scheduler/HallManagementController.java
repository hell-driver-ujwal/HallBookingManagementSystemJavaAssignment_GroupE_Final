package com.hallbooking.controllers.scheduler;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Hall;
import com.hallbooking.services.HallService;
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

public class HallManagementController extends BaseController implements Initializable {

    @FXML private TextField searchField;

    @FXML private TableView<Hall>           hallTable;
    @FXML private TableColumn<Hall, String> colHallId, colName, colType, colCapacity,
                                            colRate, colDesc, colStatus;

    @FXML private TextField      fieldHallId, fieldName, fieldCapacity, fieldRate, fieldDescription;
    @FXML private ComboBox<String> comboType;

    private final HallService hallService = new HallService();
    private ObservableList<Hall> allHalls;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        comboType.setItems(FXCollections.observableArrayList(
                "AUDITORIUM", "BANQUET_HALL", "MEETING_ROOM"));
        comboType.getSelectionModel().selectFirst();
        setupTable();
        loadHalls();
    }

    private void setupTable() {
        colHallId  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        colName    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallName()));
        colType    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallType()));
        colCapacity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCapacity() + " pax"));
        colRate    .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getRatePerHour())));
        colDesc    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colStatus  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isActive() ? "Active" : "Inactive"));
    }

    private void loadHalls() {
        allHalls = FXCollections.observableArrayList(hallService.getAllHalls());
        hallTable.setItems(allHalls);
    }

    @FXML
    public void onRowSelected() {
        Hall selected = hallTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        fieldHallId    .setText(selected.getHallId());
        fieldName      .setText(selected.getHallName());
        comboType      .setValue(selected.getHallType());
        fieldCapacity  .setText(String.valueOf(selected.getCapacity()));
        fieldRate      .setText(String.format("%.2f", selected.getRatePerHour()));
        fieldDescription.setText(selected.getDescription());
        clearError();
    }

    @FXML
    public void handleAdd() {
        clearError();
        String name = fieldName.getText().trim();
        String cap  = fieldCapacity.getText().trim();
        String rate = fieldRate.getText().trim();
        if (name.isEmpty() || cap.isEmpty() || rate.isEmpty()) {
            showError("Hall name, capacity and rate are required."); return;
        }
        try {
            String err = hallService.addHall(name, comboType.getValue(),
                    Integer.parseInt(cap), Double.parseDouble(rate),
                    fieldDescription.getText().trim());
            if (err != null) { showError(err); return; }
            MainApp.showInfo("Success", "Hall added successfully.");
            handleClear(); loadHalls();
        } catch (NumberFormatException e) {
            showError("Capacity must be a whole number. Rate must be a decimal (e.g. 300.00).");
        }
    }

    @FXML
    public void handleUpdate() {
        clearError();
        Hall selected = hallTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Please select a hall to update."); return; }
        try {
            selected.setHallName(fieldName.getText().trim());
            selected.setHallType(comboType.getValue());
            selected.setCapacity(Integer.parseInt(fieldCapacity.getText().trim()));
            selected.setRatePerHour(Double.parseDouble(fieldRate.getText().trim()));
            selected.setDescription(fieldDescription.getText().trim());
            hallService.updateHall(selected);
            MainApp.showInfo("Updated", "Hall updated successfully.");
            loadHalls();
        } catch (NumberFormatException e) {
            showError("Invalid capacity or rate value.");
        }
    }

    @FXML
    public void handleDelete() {
        Hall selected = hallTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Please select a hall to delete."); return; }
        if (!MainApp.showConfirm("Delete Hall",
                "Deactivate hall \"" + selected.getHallName()
                + "\"?\nThis will hide it from customers.")) return;
        hallService.deleteHall(selected.getHallId());
        MainApp.showInfo("Deleted", "Hall deactivated.");
        handleClear(); loadHalls();
    }

    @FXML
    public void handleClear() {
        fieldHallId.clear(); fieldName.clear(); fieldCapacity.clear();
        fieldRate.clear(); fieldDescription.clear();
        comboType.getSelectionModel().selectFirst();
        hallTable.getSelectionModel().clearSelection();
        clearError();
    }

    @FXML
    public void handleSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) { hallTable.setItems(allHalls); return; }
        List<Hall> filtered = allHalls.filtered(h ->
                h.getHallName().toLowerCase().contains(kw) ||
                h.getHallType().toLowerCase().contains(kw));
        hallTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML public void goToDashboard()      { MainApp.switchScene("/fxml/scheduler/dashboard.fxml",     980, 640); }
    @FXML public void goToHallManagement() { /* already here */ }
    @FXML public void goToAvailability()   { MainApp.switchScene("/fxml/scheduler/availability.fxml",  980, 640); }
    @FXML public void goToMaintenance()    { MainApp.switchScene("/fxml/scheduler/maintenance.fxml",   980, 640); }
    @FXML public void goToIssues()         { MainApp.switchScene("/fxml/scheduler/issues.fxml",        980, 640); }
}
