package com.hallbooking.controllers;

import com.hallbooking.MainApp;
import com.hallbooking.services.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

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

        String error = authService.loginCustomer(email, password);
        if (error != null) {
            errorLabel.setText(error);
        } else {
            MainApp.goToDashboard("CUSTOMER");
        }
    }

    @FXML public void goToRegister()  { MainApp.switchScene("/fxml/register.fxml"); }
    @FXML public void switchToStaff() { MainApp.switchScene("/fxml/login_staff.fxml"); }
    @FXML public void switchToCustomer() { /* already here */ }
}
