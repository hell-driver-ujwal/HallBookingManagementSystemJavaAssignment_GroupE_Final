package com.hallbooking.controllers.customer;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Customer;
import com.hallbooking.models.Hall;
import com.hallbooking.models.Payment;
import com.hallbooking.models.PaymentDetail;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.HallService;
import com.hallbooking.services.PaymentDetailService;
import com.hallbooking.services.PaymentService;
import com.hallbooking.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BookingConfirmationController extends BaseController implements Initializable {

    // ── Header ──────────────────────────────────────────────────────
    @FXML private Label lblReceiptNumber;
    @FXML private Label lblPaymentDate;

    // ── Booking details ─────────────────────────────────────────────
    @FXML private Label lblBookingId;
    @FXML private Label lblCustomerName;
    @FXML private Label lblCustomerId;
    @FXML private Label lblHallName;
    @FXML private Label lblHallType;
    @FXML private Label lblEventDate;
    @FXML private Label lblEventTime;
    @FXML private Label lblDuration;
    @FXML private Label lblCapacity;

    // ── Payment details ─────────────────────────────────────────────
    @FXML private Label lblPaymentId;
    @FXML private Label lblPaymentMethod;
    @FXML private Label lblAmountPaid;

    // ── Payment method-specific detail panel ───────────────────────
    @FXML private VBox  sectionCreditCard;
    @FXML private Label lblCardHolder;
    @FXML private Label lblMaskedCard;
    @FXML private Label lblExpiry;

    @FXML private VBox  sectionOnlineTransfer;
    @FXML private Label lblAccountName;
    @FXML private Label lblBankName;
    @FXML private Label lblReference;

    // ── Services ────────────────────────────────────────────────────
    private final BookingService       bookingService       = new BookingService();
    private final HallService          hallService          = new HallService();
    private final PaymentService       paymentService       = new PaymentService();
    private final PaymentDetailService paymentDetailService = new PaymentDetailService();

    // ── Data loaded at init ──────────────────────────────────────────
    private Booking       booking;
    private Hall          hall;
    private Payment       payment;
    private PaymentDetail detail;
    private Customer      customer;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_customer.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();

        String bookingId = SessionManager.getInstance().getConfirmedBookingId();
        if (bookingId == null) {
            MainApp.switchScene("/fxml/customer/browse_halls.fxml", 980, 640);
            return;
        }

        customer = SessionManager.getInstance().getCurrentCustomer();
        booking  = bookingService.getBookingById(bookingId);
        if (booking == null) {
            MainApp.switchScene("/fxml/customer/booking_history.fxml", 980, 640);
            return;
        }

        hall    = hallService.getHallById(booking.getHallId());
        payment = paymentService.getPaymentByBooking(bookingId);
        if (payment != null)
            detail = paymentDetailService.getDetailByPayment(payment.getPaymentId());

        populateFields();
    }

    private void populateFields() {
        // ── Receipt header ──────────────────────────────────────────
        lblReceiptNumber.setText(payment != null ? payment.getReceiptNumber() : "—");
        lblPaymentDate  .setText(payment != null ? formatDate(payment.getPaymentDate()) : "—");

        // ── Booking ─────────────────────────────────────────────────
        lblBookingId   .setText(booking.getBookingId());
        lblCustomerName.setText(customer != null ? customer.getFullName() : "—");
        lblCustomerId  .setText(customer != null ? customer.getCustomerId() : "—");
        lblHallName    .setText(hall != null ? hall.getHallName() : booking.getHallId());
        lblHallType    .setText(hall != null ? hall.getHallType().replace("_", " ") : "—");
        lblEventDate   .setText(booking.getEventStart().substring(0, 10));
        lblEventTime   .setText(booking.getEventStart().substring(11, 16)
                + " – " + booking.getEventEnd().substring(11, 16));
        lblDuration    .setText((int) booking.getTotalHours() + " hour(s)");
        lblCapacity    .setText(hall != null ? hall.getCapacity() + " pax" : "—");

        // ── Payment ─────────────────────────────────────────────────
        lblPaymentId    .setText(payment != null ? payment.getPaymentId() : "—");
        lblPaymentMethod.setText(payment != null ? payment.getPaymentMethod() : "—");
        lblAmountPaid   .setText(payment != null
                ? String.format("RM %.2f", payment.getAmount()) : "—");

        // ── Method-specific details ──────────────────────────────────
        if (detail != null && PaymentDetail.CREDIT_CARD.equals(detail.getMethod())) {
            sectionCreditCard.setVisible(true);
            sectionCreditCard.setManaged(true);
            sectionOnlineTransfer.setVisible(false);
            sectionOnlineTransfer.setManaged(false);
            lblCardHolder.setText(detail.getCardholderName());
            lblMaskedCard.setText(detail.getMaskedCard());
            lblExpiry    .setText(detail.getExpiry());
        } else if (detail != null && PaymentDetail.ONLINE_TRANSFER.equals(detail.getMethod())) {
            sectionCreditCard.setVisible(false);
            sectionCreditCard.setManaged(false);
            sectionOnlineTransfer.setVisible(true);
            sectionOnlineTransfer.setManaged(true);
            lblAccountName.setText(detail.getAccountName());
            lblBankName   .setText(detail.getBankName());
            lblReference  .setText(detail.getReferenceNumber());
        } else {
            sectionCreditCard.setVisible(false);
            sectionCreditCard.setManaged(false);
            sectionOnlineTransfer.setVisible(false);
            sectionOnlineTransfer.setManaged(false);
        }
    }

    /** Safely formats a stored date string regardless of separator (space or T) or length. */
    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        return raw.replace("T", " ").length() > 16
                ? raw.replace("T", " ").substring(0, 16)
                : raw.replace("T", " ");
    }

    // ── Export receipt as .txt ───────────────────────────────────────
    @FXML
    public void handleExportReceipt() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Receipt");
        chooser.setInitialFileName("Receipt_" + booking.getBookingId() + ".txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));

        File file = chooser.showSaveDialog(MainApp.getPrimaryStage());
        if (file == null) return; // user cancelled

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(buildReceiptText());
            MainApp.showInfo("Receipt Saved",
                    "Receipt saved successfully to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            MainApp.showError("Export Failed",
                    "Could not save receipt:\n" + e.getMessage());
        }
    }

    private String buildReceiptText() {
        String line  = "=".repeat(52);
        String dline = "-".repeat(52);
        StringBuilder sb = new StringBuilder();

        sb.append(line).append("\n");
        sb.append("         HALL SYMPHONY SDN BHD\n");
        sb.append("       Official Booking Receipt\n");
        sb.append(line).append("\n\n");

        sb.append(String.format("  Receipt No  : %s\n",
                payment != null ? payment.getReceiptNumber() : "—"));
        sb.append(String.format("  Date        : %s\n",
                payment != null ? formatDate(payment.getPaymentDate()) : "—"));
        sb.append("\n").append(dline).append("\n");
        sb.append("  BOOKING DETAILS\n").append(dline).append("\n");
        sb.append(String.format("  Booking ID  : %s\n", booking.getBookingId()));
        sb.append(String.format("  Customer    : %s  (%s)\n",
                customer != null ? customer.getFullName() : "—",
                customer != null ? customer.getCustomerId() : "—"));
        sb.append(String.format("  Hall        : %s\n",
                hall != null ? hall.getHallName() : booking.getHallId()));
        sb.append(String.format("  Hall Type   : %s\n",
                hall != null ? hall.getHallType().replace("_", " ") : "—"));
        sb.append(String.format("  Capacity    : %s\n",
                hall != null ? hall.getCapacity() + " pax" : "—"));
        sb.append(String.format("  Date        : %s\n",
                booking.getEventStart().substring(0, 10)));
        sb.append(String.format("  Time        : %s – %s\n",
                booking.getEventStart().substring(11, 16),
                booking.getEventEnd().substring(11, 16)));
        sb.append(String.format("  Duration    : %d hour(s)\n",
                (int) booking.getTotalHours()));
        sb.append("\n").append(dline).append("\n");
        sb.append("  PAYMENT DETAILS\n").append(dline).append("\n");
        sb.append(String.format("  Payment ID  : %s\n",
                payment != null ? payment.getPaymentId() : "—"));
        sb.append(String.format("  Method      : %s\n",
                payment != null ? payment.getPaymentMethod() : "—"));

        if (detail != null && PaymentDetail.CREDIT_CARD.equals(detail.getMethod())) {
            sb.append(String.format("  Cardholder  : %s\n", detail.getCardholderName()));
            sb.append(String.format("  Card No     : %s\n", detail.getMaskedCard()));
            sb.append(String.format("  Expiry      : %s\n", detail.getExpiry()));
        } else if (detail != null && PaymentDetail.ONLINE_TRANSFER.equals(detail.getMethod())) {
            sb.append(String.format("  Account     : %s\n", detail.getAccountName()));
            sb.append(String.format("  Bank        : %s\n", detail.getBankName()));
            sb.append(String.format("  Reference   : %s\n", detail.getReferenceNumber()));
        }

        sb.append("\n").append(dline).append("\n");
        sb.append(String.format("  TOTAL PAID  : RM %.2f\n",
                payment != null ? payment.getAmount() : 0.0));
        sb.append(dline).append("\n\n");
        sb.append("  Status      : CONFIRMED\n\n");
        sb.append(line).append("\n");
        sb.append("  Thank you for choosing Hall Symphony!\n");
        sb.append("  For enquiries: support@hallsymphony.com\n");
        sb.append(line).append("\n");
        sb.append(String.format("\n  Printed: %s\n",
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        return sb.toString();
    }

    // ── Navigation ───────────────────────────────────────────────────
    @FXML
    public void goToBookings() {
        SessionManager.getInstance().clearConfirmedBooking();
        MainApp.switchScene("/fxml/customer/booking_history.fxml", 980, 640);
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/customer/dashboard.fxml",    980, 640); }
    @FXML public void goToBrowse()    { MainApp.switchScene("/fxml/customer/browse_halls.fxml", 980, 640); }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/customer/issues.fxml",       980, 640); }
    @FXML public void goToProfile()   { MainApp.switchScene("/fxml/customer/profile.fxml",      980, 640); }
}
