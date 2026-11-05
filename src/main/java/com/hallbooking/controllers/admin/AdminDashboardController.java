package com.hallbooking.controllers.admin;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.ActivityLog;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Staff;
import com.hallbooking.models.User;
import com.hallbooking.services.ActivityLogService;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.UserService;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController extends BaseController implements Initializable {

    @FXML private Label statUsers, statCustomers, statStaff, statBlocked;

    @FXML private TableView<Staff>             staffTable;
    @FXML private TableColumn<Staff, String>   colStaffId, colName, colDept,
                                               colEmail, colHireDate, colActive;

    @FXML private TableView<Booking>           bookingsTable;
    @FXML private TableColumn<Booking, String> bColId, bColCust, bColHall,
                                               bColDate, bColAmount, bColStatus;

    @FXML private TableView<ActivityLog>           activityTable;
    @FXML private TableColumn<ActivityLog, String> aColTime, aColUser,
                                                    aColRole, aColAction, aColDetails;

    // Password reset fields
    @FXML private TextField     fieldResetUserId;
    @FXML private PasswordField fieldResetPassword;
    @FXML private Label         lblResetResult;

    private final UserService        userService        = new UserService();
    private final BookingService     bookingService     = new BookingService();
    private final ActivityLogService activityLogService = new ActivityLogService();

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        setupStaffTable();
        setupBookingsTable();
        setupActivityTable();
        loadData();
    }

    private void setupStaffTable() {
        colStaffId .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStaffId()));
        colName    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colDept    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartment()));
        colHireDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHireDate()));
        colActive  .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null && u.isActive() ? "Active" : "Blocked");
        });
        colEmail   .setCellValueFactory(c -> {
            User u = userService.getUserById(c.getValue().getUserId());
            return new SimpleStringProperty(u != null ? u.getEmail() : "—");
        });
    }

    private void setupBookingsTable() {
        bColId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookingId()));
        bColCust  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerId()));
        bColHall  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        bColDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEventStart().substring(0, 10)));
        bColAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getTotalAmount())));
        bColStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void setupActivityTable() {
        if (activityTable == null) return;
        aColTime   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTimestamp()));
        aColUser   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserId()));
        aColRole   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        aColAction .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        aColDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));
    }

    private void loadData() {
        var allUsers  = userService.getAllUsers();
        var customers = userService.getAllCustomerUsers();
        var staffList = userService.getAllStaff();
        long blocked  = allUsers.stream().filter(u -> !u.isActive()).count();
        List<Booking> bookings = bookingService.getAllBookings();

        statUsers    .setText(String.valueOf(allUsers.size()));
        statCustomers.setText(String.valueOf(customers.size()));
        statStaff    .setText(String.valueOf(staffList.size()));
        statBlocked  .setText(String.valueOf(blocked));

        staffTable   .setItems(FXCollections.observableArrayList(staffList));
        bookingsTable.setItems(FXCollections.observableArrayList(
                bookings.subList(0, Math.min(bookings.size(), 10))));

        if (activityTable != null) {
            List<ActivityLog> recent = activityLogService.getRecentLogs(20);
            activityTable.setItems(FXCollections.observableArrayList(recent));
        }
    }

    // ── Password Reset ─────────────────────────────────────────────
    @FXML
    public void handlePasswordReset() {
        if (lblResetResult != null) lblResetResult.setText("");
        if (fieldResetUserId == null || fieldResetPassword == null) return;

        String userId   = fieldResetUserId.getText().trim();
        String newPass  = fieldResetPassword.getText();

        if (userId.isEmpty()) {
            if (lblResetResult != null) lblResetResult.setText("User ID is required.");
            return;
        }
        String err = userService.resetPassword(userId, newPass);
        if (err != null) {
            if (lblResetResult != null) {
                lblResetResult.setText(err);
                lblResetResult.setStyle("-fx-text-fill:#c0392b;-fx-font-size:11px;");
            }
        } else {
            if (lblResetResult != null) {
                lblResetResult.setText("✓ Password reset successfully for " + userId);
                lblResetResult.setStyle("-fx-text-fill:#1e5c3a;-fx-font-size:11px;");
            }
            fieldResetUserId.clear();
            fieldResetPassword.clear();
            // Refresh activity log
            if (activityTable != null)
                activityTable.setItems(FXCollections.observableArrayList(
                        activityLogService.getRecentLogs(20)));
        }
    }

    @FXML public void goToDashboard()       { MainApp.switchScene("/fxml/admin/dashboard.fxml",       980, 640); }
    @FXML public void goToStaffManagement() { MainApp.switchScene("/fxml/admin/staff_management.fxml",980, 640); }
    @FXML public void goToUserManagement()  { MainApp.switchScene("/fxml/admin/user_management.fxml", 980, 640); }
    @FXML public void goToSales()           { MainApp.switchScene("/fxml/admin/sales.fxml",           980, 640); }
    @FXML public void goToBookings()        { MainApp.switchScene("/fxml/admin/bookings_overview.fxml",980, 640); }
}
