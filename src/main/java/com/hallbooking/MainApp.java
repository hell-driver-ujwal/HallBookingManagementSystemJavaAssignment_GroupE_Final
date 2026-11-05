package com.hallbooking;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * MainApp — JavaFX entry point.
 *
 * One static method switchScene(fxmlPath) is all any controller needs
 * to navigate between screens. Every controller calls:
 *
 *   MainApp.switchScene("/fxml/scheduler/hall_management.fxml");
 *
 * FXML paths are relative to src/main/resources/
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Hall Symphony — Booking Management System");
        stage.setResizable(true);
        stage.setMinWidth(800);
        stage.setMinHeight(550);

        // Always start at the customer login screen
        switchScene("/fxml/login_customer.fxml");
        stage.show();
    }

    // ── Switch to any FXML screen ──────────────────────────────────
    public static void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation Error",
                    "Could not load screen:\n" + fxmlPath + "\n\n" + e.getMessage());
        }
    }

    // ── Switch with a specific size ────────────────────────────────
    public static void switchScene(String fxmlPath, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), width, height);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation Error",
                    "Could not load screen:\n" + fxmlPath + "\n\n" + e.getMessage());
        }
    }

    // ── Role-based dashboard routing ─ call this after any login ──
    public static void goToDashboard(String role) {
        switch (role) {
            case "CUSTOMER"  -> switchScene("/fxml/customer/dashboard.fxml",  980, 640);
            case "SCHEDULER" -> switchScene("/fxml/scheduler/dashboard.fxml", 980, 640);
            case "ADMIN"     -> switchScene("/fxml/admin/dashboard.fxml",     980, 640);
            case "MANAGER"   -> switchScene("/fxml/manager/dashboard.fxml",   980, 640);
            default          -> showError("Login Error", "Unknown role: " + role);
        }
    }

    // ── Shared alert helper ────────────────────────────────────────
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait()
                .filter(r -> r == javafx.scene.control.ButtonType.OK)
                .isPresent();
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
