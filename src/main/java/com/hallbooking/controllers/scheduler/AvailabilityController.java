package com.hallbooking.controllers.scheduler;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Hall;
import com.hallbooking.models.HallAvailability;
import com.hallbooking.services.AvailabilityService;
import com.hallbooking.services.HallService;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AvailabilityController extends BaseController implements Initializable {

    @FXML private ComboBox<String> comboHall;
    @FXML private CheckBox chkMon, chkTue, chkWed, chkThu, chkFri, chkSat, chkSun;
    @FXML private Spinner<Integer> openHour, openMin, closeHour, closeMin;
    @FXML private Label lblCurrentRule;

    @FXML private TableView<HallAvailability>           ruleTable;
    @FXML private TableColumn<HallAvailability, String> colHallId, colHallName,
                                                         colOpenDays, colOpenTime, colCloseTime;

    private final AvailabilityService availabilityService = new AvailabilityService();
    private final HallService         hallService         = new HallService();
    private ObservableList<HallAvailability> allRules;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        setupTable();
        setupSpinners();
        loadHalls();
        loadRules();
    }

    private void setupSpinners() {
        openHour .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8,  1));
        openMin  .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0,  15));
        closeHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18, 1));
        closeMin .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0,  15));
        padMinutes(openMin); padMinutes(closeMin);
    }

    private void padMinutes(Spinner<Integer> s) {
        s.getEditor().textProperty().addListener((obs, old, val) -> {
            try {
                int v = Integer.parseInt(val.trim());
                if (v < 10 && !val.startsWith("0"))
                    s.getEditor().setText(String.format("%02d", v));
            } catch (NumberFormatException ignored) {}
        });
    }

    private void setupTable() {
        colHallId  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        colHallName.setCellValueFactory(c -> {
            Hall h = hallService.getHallById(c.getValue().getHallId());
            return new SimpleStringProperty(h != null ? h.getHallName() : c.getValue().getHallId());
        });
        colOpenDays.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOpenDays().replace(",", ", ")));
        colOpenTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOpenTime()));
        colCloseTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCloseTime()));
    }

    private void loadHalls() {
        List<String> options = hallService.getAllHalls().stream()
                .map(h -> h.getHallId() + " — " + h.getHallName())
                .collect(Collectors.toList());
        comboHall.setItems(FXCollections.observableArrayList(options));
        if (!options.isEmpty()) comboHall.getSelectionModel().selectFirst();
        onHallSelected();
    }

    private void loadRules() {
        allRules = FXCollections.observableArrayList(availabilityService.getAllSlots());
        ruleTable.setItems(allRules);
    }

    @FXML
    public void onHallSelected() {
        String sel = comboHall.getValue();
        if (sel == null) return;
        String hallId = sel.split(" — ")[0];
        HallAvailability rule = availabilityService.getRuleByHall(hallId);
        if (rule != null) {
            List<String> days = List.of(rule.getOpenDays().split(","));
            chkMon.setSelected(days.contains("MON")); chkTue.setSelected(days.contains("TUE"));
            chkWed.setSelected(days.contains("WED")); chkThu.setSelected(days.contains("THU"));
            chkFri.setSelected(days.contains("FRI")); chkSat.setSelected(days.contains("SAT"));
            chkSun.setSelected(days.contains("SUN"));
            LocalTime open  = LocalTime.parse(rule.getOpenTime());
            LocalTime close = LocalTime.parse(rule.getCloseTime());
            openHour .getValueFactory().setValue(open.getHour());
            openMin  .getValueFactory().setValue(open.getMinute());
            closeHour.getValueFactory().setValue(close.getHour());
            closeMin .getValueFactory().setValue(close.getMinute());
            lblCurrentRule.setText("Current: " + rule.toDisplayString());
        } else {
            chkMon.setSelected(true); chkTue.setSelected(true); chkWed.setSelected(true);
            chkThu.setSelected(true); chkFri.setSelected(true);
            chkSat.setSelected(false); chkSun.setSelected(false);
            openHour.getValueFactory().setValue(8);   openMin.getValueFactory().setValue(0);
            closeHour.getValueFactory().setValue(18); closeMin.getValueFactory().setValue(0);
            lblCurrentRule.setText("No schedule set for this hall yet.");
        }
        clearError();
    }

    @FXML
    public void handleSave() {
        clearError();
        String sel = comboHall.getValue();
        if (sel == null) { showError("Please select a hall."); return; }
        List<String> days = new ArrayList<>();
        if (chkMon.isSelected()) days.add("MON"); if (chkTue.isSelected()) days.add("TUE");
        if (chkWed.isSelected()) days.add("WED"); if (chkThu.isSelected()) days.add("THU");
        if (chkFri.isSelected()) days.add("FRI"); if (chkSat.isSelected()) days.add("SAT");
        if (chkSun.isSelected()) days.add("SUN");
        if (days.isEmpty()) { showError("Please select at least one open day."); return; }
        String openTime  = String.format("%02d:%02d", openHour.getValue(),  openMin.getValue());
        String closeTime = String.format("%02d:%02d", closeHour.getValue(), closeMin.getValue());
        if (!LocalTime.parse(closeTime).isAfter(LocalTime.parse(openTime))) {
            showError("Close time must be after open time."); return;
        }
        String hallId   = sel.split(" — ")[0];
        String openDays = String.join(",", days);
        availabilityService.saveRule(hallId, openDays, openTime, closeTime);
        MainApp.showInfo("Saved", "Schedule saved for " + sel.split(" — ")[1] + ".\n"
                + openDays.replace(",", ", ") + "   " + openTime + " – " + closeTime);
        loadRules(); onHallSelected();
    }

    @FXML
    public void handleDelete() {
        HallAvailability selected = ruleTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Please select a rule in the table to delete."); return; }
        Hall h = hallService.getHallById(selected.getHallId());
        String name = h != null ? h.getHallName() : selected.getHallId();
        if (!MainApp.showConfirm("Delete Schedule", "Remove schedule for " + name + "?\n"
                + "Customers will no longer be able to book this hall.")) return;
        availabilityService.deleteRule(selected.getRuleId());
        loadRules(); onHallSelected();
    }

    @FXML public void goToDashboard()      { MainApp.switchScene("/fxml/scheduler/dashboard.fxml",     980, 640); }
    @FXML public void goToHallManagement() { MainApp.switchScene("/fxml/scheduler/hall_management.fxml",980, 640); }
    @FXML public void goToAvailability()   { /* already here */ }
    @FXML public void goToMaintenance()    { MainApp.switchScene("/fxml/scheduler/maintenance.fxml",   980, 640); }
    @FXML public void goToIssues()         { MainApp.switchScene("/fxml/scheduler/issues.fxml",        980, 640); }
}
