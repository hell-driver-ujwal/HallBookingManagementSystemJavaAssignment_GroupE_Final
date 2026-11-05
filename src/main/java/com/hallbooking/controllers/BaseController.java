package com.hallbooking.controllers;

import com.hallbooking.MainApp;
import com.hallbooking.services.AuthService;
import com.hallbooking.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * BaseController — abstract superclass for all dashboard/screen controllers.
 *
 * Contains shared behaviour that every screen controller needs:
 *   - Username / avatar initial setup in the sidebar
 *   - Error label helpers (showError / clearError)
 *   - Logout handling (each subclass declares its own target screen)
 *
 * OOP Concepts demonstrated:
 *   - Abstraction  : BaseController cannot be instantiated directly
 *   - Inheritance  : All 17 screen controllers extend BaseController
 *   - Polymorphism : getLogoutTarget() returns a different FXML path per role
 *   - Encapsulation: shared fields are protected, not public
 */
public abstract class BaseController {

    // These @FXML fields are injected by JavaFX if the FXML declares them.
    // They are protected so subclasses can access them directly if needed.
    // If the FXML for a particular screen does not include one of these
    // fx:id values, the field simply stays null — all helper methods check for null.
    @FXML protected Label lblUsername;
    @FXML protected Label lblAvatarInitial;
    @FXML protected Label errorLabel;

    // ── Shared initialisation helpers ──────────────────────────────

    /**
     * Populates the sidebar username label and avatar initial.
     * Call this from every subclass initialize() instead of repeating
     * the 3-line block in every controller.
     */
    protected void setupUsername() {
        String name = SessionManager.getInstance().getDisplayName();
        if (lblUsername != null) {
            lblUsername.setText(name);
        }
        if (lblAvatarInitial != null) {
            lblAvatarInitial.setText(
                name == null || name.isEmpty()
                    ? "?"
                    : String.valueOf(name.charAt(0)).toUpperCase()
            );
        }
    }

    // ── Error label helpers ─────────────────────────────────────────

    /** Display an error message in the screen's error label. */
    protected void showError(String message) {
        if (errorLabel != null) errorLabel.setText(message);
    }

    /** Clear any existing error message from the error label. */
    protected void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }

    // ── Logout ─────────────────────────────────────────────────────

    /**
     * Each subclass must declare which login screen to return to on logout.
     * Customer controllers return "/fxml/login_customer.fxml";
     * Staff controllers return "/fxml/login_staff.fxml".
     */
    protected abstract String getLogoutTarget();

    /**
     * Shared logout handler wired in FXML via onAction="#handleLogout".
     * Clears the session and navigates to the login screen.
     */
    @FXML
    public void handleLogout() {
        new AuthService().logout();
        MainApp.switchScene(getLogoutTarget());
    }
}
