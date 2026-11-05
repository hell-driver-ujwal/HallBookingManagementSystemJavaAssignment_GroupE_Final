package com.hallbooking.controllers.admin;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Staff;
import com.hallbooking.models.User;
import com.hallbooking.services.UserService;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class StaffManagementController extends BaseController implements Initializable {

    @FXML private TextField searchField;

    @FXML private TableView<Staff>           staffTable;
    @FXML private TableColumn<Staff, String> colStaffId, colName, colRole, colDept,
                                             colEmail, colHire, colActive;

    @FXML private TextField     fieldName, fieldEmail, fieldPhone, fieldDept;
    @FXML private PasswordField fieldPassword;
    @FXML private ComboBox<String> comboRole;

    private final UserService userService = new UserService();
    private ObservableList<Staff> allStaff;
    private Staff selectedStaff;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        comboRole.setItems(FXCollections.observableArrayList("SCHEDULER", "MANAGER"));
        comboRole.getSelectionModel().selectFirst();
        setupTable();
        loadStaff();
    }

    private void setupTable() {
        colStaffId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStaffId()));
        colName   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colDept   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartment()));
        colHire   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHireDate()));
        colActive .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null && u.isActive() ? "Active" : "Blocked");
        });
        colRole   .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null ? u.getRole() : "—");
        });
        colEmail  .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null ? u.getEmail() : "—");
        });
    }

    private void loadStaff() {
        allStaff = FXCollections.observableArrayList(userService.getAllStaff());
        staffTable.setItems(allStaff);
    }

    @FXML
    public void onRowSelected() {
        selectedStaff = staffTable.getSelectionModel().getSelectedItem();
        if (selectedStaff == null) return;
        User u = userService.getUserById(selectedStaff.getUserId());
        fieldName .setText(selectedStaff.getFullName());
        fieldEmail.setText(u != null ? u.getEmail() : "");
        fieldPhone.setText(u != null ? u.getPhone()  : "");
        fieldDept .setText(selectedStaff.getDepartment());
        if (u != null) comboRole.setValue(u.getRole());
        clearError();
    }

    @FXML
    public void handleCreate() {
        clearError();
        String err = userService.createStaffAccount(
                fieldName.getText().trim(),
                fieldEmail.getText().trim(),
                fieldPassword.getText(),
                fieldPhone.getText().trim(),
                comboRole.getValue(),
                fieldDept.getText().trim()
        );
        if (err != null) { showError(err); return; }
        MainApp.showInfo("Created", "Staff account created successfully.");
        handleClear();
        loadStaff();
    }

    /** Update the selected staff member's name, department, and phone. */
    @FXML
    public void handleUpdate() {
        clearError();
        if (selectedStaff == null) {
            showError("Please select a staff member to update."); return;
        }
        String name  = fieldName.getText().trim();
        String dept  = fieldDept.getText().trim();
        String phone = fieldPhone.getText().trim();

        if (name.isEmpty()) { showError("Full name cannot be empty."); return; }

        selectedStaff.setFullName(name);
        selectedStaff.setDepartment(dept);
        userService.updateStaff(selectedStaff);

        // Also update phone in the linked User record
        User u = userService.getUserById(selectedStaff.getUserId());
        if (u != null && !phone.isEmpty()) {
            u.setPhone(phone);
            userService.updateUser(u);
        }

        MainApp.showInfo("Updated", "Staff details updated successfully.");
        loadStaff();
    }

    @FXML
    public void handleToggleBlock() {
        if (selectedStaff == null) { showError("Please select a staff member."); return; }
        User u = userService.getUserById(selectedStaff.getUserId());
        if (u == null) return;
        if (u.isActive()) {
            userService.blockUser(u.getUserId());
            MainApp.showInfo("Blocked", selectedStaff.getFullName() + " has been blocked.");
        } else {
            userService.unblockUser(u.getUserId());
            MainApp.showInfo("Unblocked", selectedStaff.getFullName() + " has been unblocked.");
        }
        loadStaff();
    }

    @FXML
    public void handleDelete() {
        if (selectedStaff == null) { showError("Please select a staff member."); return; }
        if (!MainApp.showConfirm("Delete Account",
                "Permanently delete account for " + selectedStaff.getFullName()
                + "?\nThis cannot be undone.")) return;
        userService.deleteUser(selectedStaff.getUserId());
        MainApp.showInfo("Deleted", "Staff account deleted.");
        handleClear();
        loadStaff();
    }

    @FXML
    public void handleClear() {
        fieldName.clear(); fieldEmail.clear(); fieldPhone.clear();
        fieldDept.clear(); fieldPassword.clear();
        comboRole.getSelectionModel().selectFirst();
        staffTable.getSelectionModel().clearSelection();
        selectedStaff = null;
        clearError();
    }

    @FXML
    public void handleSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) { staffTable.setItems(allStaff); return; }
        staffTable.setItems(allStaff.filtered(s -> s.getFullName().toLowerCase().contains(kw)));
    }

    @FXML public void goToDashboard()       { MainApp.switchScene("/fxml/admin/dashboard.fxml",       980, 640); }
    @FXML public void goToStaffManagement() { /* already here */ }
    @FXML public void goToUserManagement()  { MainApp.switchScene("/fxml/admin/user_management.fxml", 980, 640); }
    @FXML public void goToSales()           { MainApp.switchScene("/fxml/admin/sales.fxml",           980, 640); }
    @FXML public void goToBookings()        { MainApp.switchScene("/fxml/admin/bookings_overview.fxml",980, 640); }
}
