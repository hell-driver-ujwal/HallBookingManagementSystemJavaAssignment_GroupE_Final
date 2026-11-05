package com.hallbooking.controllers.customer;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Customer;
import com.hallbooking.models.User;
import com.hallbooking.utils.FileManager;
import com.hallbooking.utils.PasswordUtils;
import com.hallbooking.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController extends BaseController implements Initializable {

    @FXML private TextField     fieldFullName, fieldEmail, fieldPhone, fieldAddress;
    @FXML private PasswordField fieldNewPassword, fieldConfirmPassword;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_customer.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        loadProfile();
    }

    private void loadProfile() {
        Customer c = SessionManager.getInstance().getCurrentCustomer();
        User     u = SessionManager.getInstance().getCurrentUser();
        fieldFullName.setText(c.getFullName());
        fieldEmail   .setText(u.getEmail());
        fieldPhone   .setText(c.getPhone());
        fieldAddress .setText(c.getAddress());
    }

    @FXML
    public void handleSave() {
        clearError();
        String fullName = fieldFullName.getText().trim();
        String phone    = fieldPhone.getText().trim();
        String address  = fieldAddress.getText().trim();
        String newPass  = fieldNewPassword.getText();
        String confPass = fieldConfirmPassword.getText();

        if (fullName.isEmpty()) { showError("Full name cannot be empty."); return; }
        if (!phone.matches("^0[0-9]{8,10}$")) {
            showError("Phone must be 9–11 digits starting with 0."); return;
        }

        Customer c = SessionManager.getInstance().getCurrentCustomer();
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setAddress(address);
        FileManager.updateLine(FileManager.CUSTOMERS, c.getCustomerId(), c.toFileString());

        User u = SessionManager.getInstance().getCurrentUser();
        u.setPhone(phone);

        if (!newPass.isEmpty()) {
            if (newPass.length() < 6) { showError("Password must be at least 6 characters."); return; }
            if (!newPass.equals(confPass)) { showError("Passwords do not match."); return; }
            u.setPassword(PasswordUtils.hash(newPass));
        }
        FileManager.updateLine(FileManager.USERS, u.getUserId(), u.toFileString());

        SessionManager.getInstance().setCurrentCustomer(c);
        SessionManager.getInstance().setCurrentUser(u);
        setupUsername();

        MainApp.showInfo("Saved", "Your profile has been updated successfully.");
        fieldNewPassword.clear();
        fieldConfirmPassword.clear();
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/customer/dashboard.fxml",      980, 640); }
    @FXML public void goToBrowse()    { MainApp.switchScene("/fxml/customer/browse_halls.fxml",   980, 640); }
    @FXML public void goToBookings()  { MainApp.switchScene("/fxml/customer/booking_history.fxml",980, 640); }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/customer/issues.fxml",         980, 640); }
    @FXML public void goToProfile()   { /* already here */ }
}
