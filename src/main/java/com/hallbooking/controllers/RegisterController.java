package com.hallbooking.controllers;

import com.hallbooking.MainApp;
import com.hallbooking.services.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     phoneField;
    @FXML private TextField     addressField;
    @FXML private Label         errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleRegister() {
        errorLabel.setText("");

        String error = authService.registerCustomer(
                fullNameField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText(),
                confirmPasswordField.getText(),
                phoneField.getText().trim(),
                addressField.getText().trim()
        );

        if (error != null) {
            errorLabel.setText(error);
        } else {
            MainApp.showInfo("Account Created",
                    "Your account has been created successfully!\nYou can now log in.");
            goToLogin();
        }
    }

    @FXML public void goToLogin() { MainApp.switchScene("/fxml/login_customer.fxml"); }
}
