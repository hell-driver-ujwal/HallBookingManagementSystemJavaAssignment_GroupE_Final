package com.hallbooking.controllers.customer;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Hall;
import com.hallbooking.models.PaymentDetail;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.HallService;
import com.hallbooking.services.PaymentDetailService;
import com.hallbooking.services.PaymentService;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class PaymentController extends BaseController implements Initializable {

    // ── Booking summary labels ──────────────────────────────────────
    @FXML private Label lblBookingId, lblHallName, lblEventDate,
                        lblEventTime, lblDuration, lblAmount, lblMethod;

    // ── Section containers ──────────────────────────────────────────
    @FXML private VBox sectionCreditCard;
    @FXML private VBox sectionOnlineTransfer;

    // ── Credit Card fields ──────────────────────────────────────────
    @FXML private TextField     fieldCardHolder, fieldCardNumber, fieldExpiry;
    @FXML private PasswordField fieldCVV;
    @FXML private Label         ccErrorLabel;

    // ── Online Transfer fields ──────────────────────────────────────
    @FXML private Label     lblServiceBank, lblServiceAccount, lblServiceAccountNo, lblTransferAmount;
    @FXML private TextField fieldAccountName, fieldAccountNumber, fieldBankName,
                            fieldReference, fieldAmountConfirm;

    private final PaymentDetailService paymentDetailService = new PaymentDetailService();
    private final PaymentService       paymentService       = new PaymentService();
    private final BookingService       bookingService       = new BookingService();
    private final HallService          hallService          = new HallService();

    private String bookingId;
    private double amount;
    private String method;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_customer.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();

        SessionManager session = SessionManager.getInstance();
        bookingId = session.getPendingBookingId();
        amount    = session.getPendingAmount();
        method    = session.getPendingPaymentMethod();

        // Safety check — if someone navigates here directly with no pending payment
        if (bookingId == null) {
            MainApp.switchScene("/fxml/customer/browse_halls.fxml", 980, 640);
            return;
        }

        loadBookingSummary();
        showMethodSection();
    }

    private void loadBookingSummary() {
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) return;

        Hall hall = hallService.getHallById(booking.getHallId());

        lblBookingId .setText(bookingId);
        lblHallName  .setText(hall != null ? hall.getHallName() : booking.getHallId());
        lblEventDate .setText(booking.getEventStart().substring(0, 10));
        lblEventTime .setText(booking.getEventStart().substring(11)
                + " – " + booking.getEventEnd().substring(11));
        lblDuration  .setText((int) booking.getTotalHours() + " hour(s)");
        lblAmount    .setText(String.format("RM %.2f", amount));
        lblMethod    .setText(method);
    }

    private void showMethodSection() {
        if (PaymentDetail.CREDIT_CARD.equals(method)) {
            sectionCreditCard.setVisible(true);
            sectionCreditCard.setManaged(true);
            sectionOnlineTransfer.setVisible(false);
            sectionOnlineTransfer.setManaged(false);
        } else {
            sectionCreditCard.setVisible(false);
            sectionCreditCard.setManaged(false);
            sectionOnlineTransfer.setVisible(true);
            sectionOnlineTransfer.setManaged(true);
            // Pre-fill our service bank details (read-only)
            lblServiceBank.setText(PaymentDetailService.SERVICE_BANK_NAME);
            lblServiceAccount.setText(PaymentDetailService.SERVICE_ACCOUNT_NAME);
            lblServiceAccountNo.setText(PaymentDetailService.SERVICE_ACCOUNT_NO);
            lblTransferAmount.setText(String.format("RM %.2f", amount));
            // Auto-generate reference number
            fieldReference.setText(PaymentDetailService.generateReferenceNumber());
        }
    }

    // ── Auto-detect bank when customer types their account number ───
    @FXML
    public void onAccountNumberChanged() {
        String acc = fieldAccountNumber.getText().replaceAll("\\s+", "");
        if (acc.length() >= 2) {
            fieldBankName.setText(PaymentDetailService.detectBank(acc));
        } else {
            fieldBankName.setText("");
        }
    }

    // ── Submit Credit Card ──────────────────────────────────────────
    @FXML
    public void handlePayCreditCard() {
        if (ccErrorLabel != null) ccErrorLabel.setText("");
        String cvv = fieldCVV.getText().trim();
        if (!cvv.matches("\\d{3}")) { if (ccErrorLabel != null) ccErrorLabel.setText("CVV must be exactly 3 digits."); return; }

        com.hallbooking.models.Payment payment = paymentService.getPaymentByBooking(bookingId);
        if (payment == null) { if (ccErrorLabel != null) ccErrorLabel.setText("Payment record not found."); return; }

        String err = paymentDetailService.saveCreditCardDetail(
                payment.getPaymentId(),
                fieldCardHolder.getText().trim(),
                fieldCardNumber.getText().trim(),
                fieldExpiry.getText().trim());

        if (err != null) { if (ccErrorLabel != null) ccErrorLabel.setText(err); return; }
        finishPayment();
    }

    // ── Submit Online Transfer ──────────────────────────────────────
    @FXML
    public void handlePayTransfer() {
        clearError();
        double confirmedAmt;
        try {
            confirmedAmt = Double.parseDouble(
                    fieldAmountConfirm.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            showError("Please enter a valid amount (e.g. 300.00)");
            return;
        }

        com.hallbooking.models.Payment payment = paymentService.getPaymentByBooking(bookingId);
        if (payment == null) { showError("Payment record not found."); return; }

        String err = paymentDetailService.saveOnlineTransferDetail(
                payment.getPaymentId(),
                fieldAccountName.getText().trim(),
                fieldBankName.getText().trim(),
                fieldReference.getText().trim(),
                amount,
                confirmedAmt);

        if (err != null) { showError(err); return; }
        finishPayment();
    }

    // ── Shared finish — store confirmed booking and show receipt screen ──
    private void finishPayment() {
        SessionManager session = SessionManager.getInstance();
        session.setConfirmedBookingId(bookingId);
        session.clearPendingPayment();
        MainApp.switchScene("/fxml/customer/booking_confirmation.fxml", 980, 640);
    }

    // ── Cancel — go back to browse, booking stays confirmed ─────────
    @FXML
    public void handleCancel() {
        if (!MainApp.showConfirm("Cancel Payment",
                "Your booking is already confirmed.\n"
                + "Payment details won't be saved if you leave now.\n\n"
                + "Go back to Browse Halls?")) return;
        SessionManager.getInstance().clearPendingPayment();
        MainApp.switchScene("/fxml/customer/browse_halls.fxml", 980, 640);
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/customer/dashboard.fxml",      980, 640); }
    @FXML public void goToBrowse()    { MainApp.switchScene("/fxml/customer/browse_halls.fxml",   980, 640); }
    @FXML public void goToBookings()  { MainApp.switchScene("/fxml/customer/booking_history.fxml",980, 640); }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/customer/issues.fxml",         980, 640); }
    @FXML public void goToProfile()   { MainApp.switchScene("/fxml/customer/profile.fxml",        980, 640); }
}
