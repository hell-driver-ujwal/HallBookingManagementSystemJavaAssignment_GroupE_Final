package com.hallbooking.controllers.admin;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Customer;
import com.hallbooking.models.User;
import com.hallbooking.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CustomerManagementController — Admin views, blocks and deletes customer accounts only.
 * Staff accounts are managed separately in StaffManagementController.
 * The FXML file is still named user_management.fxml for backward compatibility.
 */
public class UserManagementController extends BaseController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label     lblSelected;

    @FXML private TableView<Customer>           userTable;
    @FXML private TableColumn<Customer, String> colUserId, colEmail, colName, colPhone,
                                                colAddress, colCreated, colLastLogin, colStatus;

    private final UserService userService = new UserService();
    private ObservableList<Customer> allCustomers;
    private Customer selectedCustomer;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        setupTable();
        loadCustomers();
    }

    private void setupTable() {
        colUserId   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerId()));
        colName     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colEmail    .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null ? u.getEmail() : "—");
        });
        colPhone    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        colAddress  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        colCreated  .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null ? u.getCreatedAt().substring(0, 10) : "—");
        });
        colLastLogin.setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null ? u.getLastLoginAt().substring(0, 10) : "—");
        });
        colStatus   .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null && u.isActive() ? "Active" : "Blocked");
        });
    }

    private void loadCustomers() {
        List<Customer> customers = userService.getAllCustomers();
        allCustomers = FXCollections.observableArrayList(customers);
        userTable.setItems(allCustomers);
        lblSelected.setText("None");
        selectedCustomer = null;
    }

    @FXML
    public void onRowSelected() {
        selectedCustomer = userTable.getSelectionModel().getSelectedItem();
        if (selectedCustomer == null) return;
        User u = userService.getUserById(selectedCustomer.getUserId());
        String email = u != null ? u.getEmail() : selectedCustomer.getUserId();
        lblSelected.setText(selectedCustomer.getFullName() + "  [" + email + "]");
        clearError();
    }

    @FXML
    public void handleToggleBlock() {
        if (selectedCustomer == null) { showError("Please select a customer."); return; }
        User u = userService.getUserById(selectedCustomer.getUserId());
        if (u == null) return;
        if (u.isActive()) {
            userService.blockUser(u.getUserId());
            MainApp.showInfo("Blocked", selectedCustomer.getFullName() + " has been blocked.");
        } else {
            userService.unblockUser(u.getUserId());
            MainApp.showInfo("Unblocked", selectedCustomer.getFullName() + " has been unblocked.");
        }
        loadCustomers();
    }

    @FXML
    public void handleDelete() {
        if (selectedCustomer == null) { showError("Please select a customer."); return; }
        if (!MainApp.showConfirm("Delete Customer",
                "Permanently delete account for " + selectedCustomer.getFullName()
                + "?\nThis cannot be undone."))
            return;
        userService.deleteUser(selectedCustomer.getUserId());
        MainApp.showInfo("Deleted", "Customer account deleted.");
        loadCustomers();
    }

    @FXML
    public void handleSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) { userTable.setItems(allCustomers); return; }
        userTable.setItems(allCustomers.filtered(c ->
                c.getFullName().toLowerCase().contains(kw) ||
                c.getPhone().toLowerCase().contains(kw)));
    }

    @FXML public void goToDashboard()       { MainApp.switchScene("/fxml/admin/dashboard.fxml",       980, 640); }
    @FXML public void goToStaffManagement() { MainApp.switchScene("/fxml/admin/staff_management.fxml",980, 640); }
    @FXML public void goToUserManagement()  { /* already here */ }
    @FXML public void goToSales()           { MainApp.switchScene("/fxml/admin/sales.fxml",           980, 640); }
    @FXML public void goToBookings()        { MainApp.switchScene("/fxml/admin/bookings_overview.fxml",980, 640); }
}
