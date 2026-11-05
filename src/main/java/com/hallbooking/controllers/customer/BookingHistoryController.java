package com.hallbooking.controllers.customer;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Hall;
import com.hallbooking.models.Payment;
import com.hallbooking.models.PaymentDetail;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.HallService;
import com.hallbooking.services.PaymentDetailService;
import com.hallbooking.services.PaymentService;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BookingHistoryController extends BaseController implements Initializable {

    @FXML private ComboBox<String> filterStatus;

    @FXML private TableView<Booking>           bookingTable;
    @FXML private TableColumn<Booking, String> colId, colHall, colDate, colTime,
                                               colHours, colAmount, colPaid, colStatus;

    @FXML private Label detBookingId, detHall, detStart, detEnd,
                        detAmount, detReceipt, detMethod, detDuration;
    @FXML private Label cancelError;

    private final BookingService       bookingService       = new BookingService();
    private final HallService          hallService          = new HallService();
    private final PaymentService       paymentService       = new PaymentService();
    private final PaymentDetailService paymentDetailService = new PaymentDetailService();

    private ObservableList<Booking> allBookings;
    private Booking selectedBooking;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_customer.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        filterStatus.setItems(FXCollections.observableArrayList("All", "CONFIRMED", "CANCELLED"));
        filterStatus.getSelectionModel().selectFirst();
        setupTable();
        loadBookings();
    }

    private void setupTable() {
        colId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookingId()));
        colHall  .setCellValueFactory(c -> {
            Hall h = hallService.getHallById(c.getValue().getHallId());
            return new SimpleStringProperty(h != null ? h.getHallName() : c.getValue().getHallId());
        });
        colDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEventStart().substring(0, 10)));
        colTime  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEventStart().substring(11, 16)
                + " – " + c.getValue().getEventEnd().substring(11, 16)));
        colHours .setCellValueFactory(c -> new SimpleStringProperty(
                (int) c.getValue().getTotalHours() + " hr"));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getTotalAmount())));
        colPaid  .setCellValueFactory(c -> {
            Payment p = paymentService.getPaymentByBooking(c.getValue().getBookingId());
            return new SimpleStringProperty(p != null ? p.getPaymentMethod() : "—");
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void loadBookings() {
        String customerId = SessionManager.getInstance().getCurrentCustomer().getCustomerId();
        allBookings = FXCollections.observableArrayList(
                bookingService.getBookingsByCustomer(customerId));
        bookingTable.setItems(allBookings);
    }

    @FXML
    public void onRowSelected() {
        selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) return;
        cancelError.setText("");

        Hall    h = hallService.getHallById(selectedBooking.getHallId());
        Payment p = paymentService.getPaymentByBooking(selectedBooking.getBookingId());

        detBookingId.setText(selectedBooking.getBookingId());
        detHall     .setText(h != null ? h.getHallName() : selectedBooking.getHallId());
        detStart    .setText(selectedBooking.getEventStart().substring(0, 16));
        detEnd      .setText(selectedBooking.getEventEnd().substring(0, 16));
        detDuration .setText((int) selectedBooking.getTotalHours() + " hour(s)");
        detAmount   .setText(String.format("RM %.2f", selectedBooking.getTotalAmount()));
        detMethod   .setText(p != null ? p.getPaymentMethod() : "—");
        detReceipt  .setText(p != null ? p.getReceiptNumber() : "—");
    }

    // ── Export receipt from booking history ──────────────────────────
    @FXML
    public void handleExportReceipt() {
        cancelError.setText("");
        if (selectedBooking == null) {
            cancelError.setText("Please select a booking to export its receipt.");
            return;
        }
        Payment p = paymentService.getPaymentByBooking(selectedBooking.getBookingId());
        if (p == null) {
            cancelError.setText("No payment record found for this booking.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Receipt");
        chooser.setInitialFileName("Receipt_" + selectedBooking.getBookingId() + ".txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));

        File file = chooser.showSaveDialog(MainApp.getPrimaryStage());
        if (file == null) return;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(buildReceiptText(selectedBooking, p));
            MainApp.showInfo("Receipt Saved",
                    "Receipt saved successfully to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            MainApp.showError("Export Failed", "Could not save receipt:\n" + e.getMessage());
        }
    }

    /** Safely formats a stored date string regardless of separator (space or T) or length. */
    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        return raw.replace("T", " ").length() > 16
                ? raw.replace("T", " ").substring(0, 16)
                : raw.replace("T", " ");
    }

    private String buildReceiptText(Booking booking, Payment payment) {
        Hall          hall     = hallService.getHallById(booking.getHallId());
        PaymentDetail detail   = paymentDetailService.getDetailByPayment(payment.getPaymentId());
        var           customer = SessionManager.getInstance().getCurrentCustomer();

        String line  = "=".repeat(52);
        String dline = "-".repeat(52);
        StringBuilder sb = new StringBuilder();

        sb.append(line).append("\n");
        sb.append("         HALL SYMPHONY SDN BHD\n");
        sb.append("       Official Booking Receipt\n");
        sb.append(line).append("\n\n");
        sb.append(String.format("  Receipt No  : %s\n", payment.getReceiptNumber()));
        sb.append(String.format("  Date        : %s\n", formatDate(payment.getPaymentDate())));
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
        sb.append(String.format("  Date        : %s\n", booking.getEventStart().substring(0, 10)));
        sb.append(String.format("  Time        : %s – %s\n",
                booking.getEventStart().substring(11, 16),
                booking.getEventEnd().substring(11, 16)));
        sb.append(String.format("  Duration    : %d hour(s)\n", (int) booking.getTotalHours()));
        sb.append("\n").append(dline).append("\n");
        sb.append("  PAYMENT DETAILS\n").append(dline).append("\n");
        sb.append(String.format("  Payment ID  : %s\n", payment.getPaymentId()));
        sb.append(String.format("  Method      : %s\n", payment.getPaymentMethod()));

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
        sb.append(String.format("  TOTAL PAID  : RM %.2f\n", payment.getAmount()));
        sb.append(dline).append("\n\n");
        sb.append(String.format("  Status      : %s\n\n", booking.getStatus()));
        sb.append(line).append("\n");
        sb.append("  Thank you for choosing Hall Symphony!\n");
        sb.append("  For enquiries: support@hallsymphony.com\n");
        sb.append(line).append("\n");
        sb.append(String.format("\n  Printed: %s\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        return sb.toString();
    }

    @FXML
    public void handleRebook() {
        if (selectedBooking == null) {
            cancelError.setText("Please select a booking to re-book.");
            return;
        }
        try {
            String startStr = selectedBooking.getEventStart();
            String endStr   = selectedBooking.getEventEnd();
            int startHour = Integer.parseInt(startStr.substring(11, 13));
            int startMin  = Integer.parseInt(startStr.substring(14, 16));
            int endHour   = Integer.parseInt(endStr.substring(11, 13));
            int endMin    = Integer.parseInt(endStr.substring(14, 16));
            SessionManager.getInstance().setPendingRebook(
                    selectedBooking.getHallId(), startHour, startMin, endHour, endMin);
            MainApp.switchScene("/fxml/customer/browse_halls.fxml", 980, 640);
        } catch (Exception e) {
            cancelError.setText("Could not parse booking times. Please book manually.");
        }
    }

    @FXML
    public void handleCancel() {
        cancelError.setText("");
        if (selectedBooking == null) { cancelError.setText("Please select a booking."); return; }
        if ("CANCELLED".equals(selectedBooking.getStatus())) {
            cancelError.setText("This booking is already cancelled."); return;
        }
        if (!MainApp.showConfirm("Cancel Booking",
                "Cancel booking " + selectedBooking.getBookingId() + "?\nThis cannot be undone."))
            return;
        String err = bookingService.cancelBooking(selectedBooking.getBookingId());
        if (err != null) { cancelError.setText(err); return; }
        selectedBooking = null;
        MainApp.showInfo("Cancelled", "Booking cancelled successfully.");
        loadBookings();
    }

    @FXML
    public void applyFilter() {
        String filter = filterStatus.getValue();
        if (filter == null || filter.equals("All")) { bookingTable.setItems(allBookings); return; }
        bookingTable.setItems(allBookings.filtered(b -> b.getStatus().equals(filter)));
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/customer/dashboard.fxml",      980, 640); }
    @FXML public void goToBrowse()    { MainApp.switchScene("/fxml/customer/browse_halls.fxml",   980, 640); }
    @FXML public void goToBookings()  { /* already here */ }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/customer/issues.fxml",         980, 640); }
    @FXML public void goToProfile()   { MainApp.switchScene("/fxml/customer/profile.fxml",        980, 640); }
}
