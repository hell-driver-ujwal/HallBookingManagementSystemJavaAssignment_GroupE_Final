package com.hallbooking.controllers;

import com.hallbooking.MainApp;
import com.hallbooking.services.AuthService;
import com.hallbooking.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginStaffController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setText("");

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter your email and password.");
            return;
        }

        String error = authService.loginStaff(email, password);
        if (error != null) {
            errorLabel.setText(error);
        } else {
            MainApp.goToDashboard(SessionManager.getInstance().getRole());
        }
    }

    @FXML public void switchToCustomer() { MainApp.switchScene("/fxml/login_customer.fxml"); }
    @FXML public void switchToStaff()    { /* already here */ }
}
