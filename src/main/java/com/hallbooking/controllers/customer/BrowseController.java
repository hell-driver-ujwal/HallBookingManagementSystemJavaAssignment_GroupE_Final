package com.hallbooking.controllers.customer;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Hall;
import com.hallbooking.models.HallAvailability;
import com.hallbooking.models.Maintenance;
import com.hallbooking.services.*;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

public class BrowseController extends BaseController implements Initializable {

    @FXML private ComboBox<String> typeFilter;
    @FXML private TextField        searchField;

    @FXML private TableView<Hall>           hallTable;
    @FXML private TableColumn<Hall, String> colId, colName, colType, colCap, colRate, colDesc;

    @FXML private javafx.scene.layout.VBox bookingPanel;
    @FXML private Label    lblSelectedHall;
    @FXML private TextArea unavailInfo;

    @FXML private DatePicker       dateStart;
    @FXML private DatePicker       dateEnd;
    @FXML private Spinner<Integer> startHour, startMin, endHour, endMin;

    @FXML private ComboBox<String> comboPayment;
    @FXML private Label            lblEstimate;
    @FXML private Label            conflictLabel;

    private final HallService         hallService         = new HallService();
    private final BookingService      bookingService      = new BookingService();
    private final MaintenanceService  maintenanceService  = new MaintenanceService();
    private final AvailabilityService availabilityService = new AvailabilityService();
    private final PaymentService      paymentService      = new PaymentService();

    private ObservableList<Hall> allHalls;
    private Hall selectedHall;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_customer.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        typeFilter.setItems(FXCollections.observableArrayList(
                "All Types", "AUDITORIUM", "BANQUET_HALL", "MEETING_ROOM"));
        typeFilter.getSelectionModel().selectFirst();
        comboPayment.setItems(FXCollections.observableArrayList(
                "Cash", "Credit Card", "Online Transfer"));
        comboPayment.getSelectionModel().selectFirst();
        setupTable();
        loadHalls();
        setupDateTimePickers();

        // ── Auto-fill from Re-book if pending ─────────────────────
        if (SessionManager.getInstance().hasPendingRebook()) {
            applyRebookContext();
        }
    }

    private void applyRebookContext() {
        SessionManager session = SessionManager.getInstance();
        String hallId = session.getRebookHallId();

        // Find and select the hall in the table
        for (Hall h : allHalls) {
            if (h.getHallId().equals(hallId)) {
                hallTable.getSelectionModel().select(h);
                hallTable.scrollTo(h);
                selectedHall = h;

                // Show booking panel
                bookingPanel.setVisible(true);
                bookingPanel.setManaged(true);
                lblSelectedHall.setText("Re-booking: " + h.getHallName());
                loadUnavailableInfo(hallId);

                // Pre-fill time spinners with original times
                // Set tomorrow as default date (can't rebook past)
                LocalDate tomorrow = LocalDate.now().plusDays(1);
                dateStart.setValue(tomorrow);
                dateEnd  .setValue(tomorrow);
                startHour.getValueFactory().setValue(session.getRebookStartHour());
                startMin .getValueFactory().setValue(session.getRebookStartMin());
                endHour  .getValueFactory().setValue(session.getRebookEndHour());
                endMin   .getValueFactory().setValue(session.getRebookEndMin());

                hideConflict();
                clearError();
                updateEstimate();
                break;
            }
        }
        // Clear pending rebook so it doesn't persist
        session.clearPendingRebook();
    }

    private void setupDateTimePickers() {
        startHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9,  1));
        endHour  .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 17, 1));
        startMin .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0,  15));
        endMin   .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 45, 0,  15));
        padMinutes(startMin); padMinutes(endMin);

        dateStart.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now())
                        || d.isAfter(LocalDate.now().plusDays(30)));
            }
        });
        dateEnd.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now())
                        || d.isAfter(LocalDate.now().plusDays(30)));
            }
        });
        dateStart.setValue(LocalDate.now());
        dateEnd  .setValue(LocalDate.now());

        Runnable refresh = this::updateEstimate;
        startHour.valueProperty().addListener((o, ov, nv) -> refresh.run());
        startMin .valueProperty().addListener((o, ov, nv) -> refresh.run());
        endHour  .valueProperty().addListener((o, ov, nv) -> refresh.run());
        endMin   .valueProperty().addListener((o, ov, nv) -> refresh.run());
        dateStart.valueProperty().addListener((o, ov, nv) -> refresh.run());
        dateEnd  .valueProperty().addListener((o, ov, nv) -> refresh.run());
    }

    private void padMinutes(Spinner<Integer> spinner) {
        spinner.getEditor().textProperty().addListener((obs, old, val) -> {
            try {
                int v = Integer.parseInt(val.trim());
                if (v < 10 && !val.startsWith("0"))
                    spinner.getEditor().setText(String.format("%02d", v));
            } catch (NumberFormatException ignored) {}
        });
    }

    private LocalDateTime getStartDateTime() {
        if (dateStart.getValue() == null) return null;
        return LocalDateTime.of(dateStart.getValue(),
                LocalTime.of(startHour.getValue(), startMin.getValue()));
    }

    private LocalDateTime getEndDateTime() {
        if (dateEnd.getValue() == null) return null;
        return LocalDateTime.of(dateEnd.getValue(),
                LocalTime.of(endHour.getValue(), endMin.getValue()));
    }

    private void showConflict(String msg) {
        conflictLabel.setText(msg);
        conflictLabel.setVisible(true);
        conflictLabel.setManaged(true);
        lblEstimate.setText("");
    }

    private void hideConflict() {
        conflictLabel.setVisible(false);
        conflictLabel.setManaged(false);
    }

    private void setupTable() {
        colId  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallName()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallType()));
        colCap .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCapacity() + " pax"));
        colRate.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f / hr", c.getValue().getRatePerHour())));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
    }

    private void loadHalls() {
        allHalls = FXCollections.observableArrayList(hallService.getActiveHalls());
        hallTable.setItems(allHalls);
    }

    @FXML
    public void onHallSelected() {
        selectedHall = hallTable.getSelectionModel().getSelectedItem();
        if (selectedHall == null) return;
        bookingPanel.setVisible(true);
        bookingPanel.setManaged(true);
        lblSelectedHall.setText("Book: " + selectedHall.getHallName());
        loadUnavailableInfo(selectedHall.getHallId());
        hideConflict();
        clearError();
        lblEstimate.setText("");
        updateEstimate();
    }

    private void loadUnavailableInfo(String hallId) {
        StringBuilder sb = new StringBuilder();
        HallAvailability rule = availabilityService.getRuleByHall(hallId);
        if (rule != null) {
            sb.append("OPEN: ").append(rule.getOpenDays().replace(",", ", "))
              .append("   ").append(rule.getOpenTime())
              .append(" – ").append(rule.getCloseTime()).append("\n\n");
        } else {
            sb.append("No schedule set — bookings not currently accepted.\n\n");
        }
        List<Booking> bookings = bookingService.getConfirmedByHall(hallId);
        for (Booking b : bookings) {
            sb.append("BOOKED: ")
              .append(DateTimeUtils.toDisplayFormat(b.getEventStart()))
              .append(" – ").append(DateTimeUtils.toDisplayFormat(b.getEventEnd()))
              .append("  (").append(b.getBookingId()).append(")\n");
        }
        List<Maintenance> maints = maintenanceService.getMaintenanceByHall(hallId);
        for (Maintenance m : maints) {
            if (!m.isResolved()) {
                sb.append("MAINTENANCE [").append(m.getStatus()).append("]: ")
                  .append(DateTimeUtils.toDisplayFormat(m.getStartDatetime()))
                  .append(" – ").append(DateTimeUtils.toDisplayFormat(m.getEndDatetime()))
                  .append("  — ").append(m.getRemarks()).append("\n");
            }
        }
        boolean noConflicts = bookings.isEmpty() && maints.stream().allMatch(m -> m.isResolved());
        if (noConflicts) sb.append("No existing bookings or maintenance conflicts.");
        unavailInfo.setText(sb.toString().trim());
    }

    @FXML
    public void updateEstimate() {
        if (selectedHall == null) return;
        hideConflict(); lblEstimate.setText("");
        LocalDateTime start = getStartDateTime();
        LocalDateTime end   = getEndDateTime();
        if (start == null || end == null) return;
        if (start.isBefore(LocalDateTime.now())) {
            showConflict("Start date/time is in the past."); return;
        }
        if (!end.isAfter(start)) { showConflict("End time must be after start time."); return; }
        String startStr = DateTimeUtils.format(start);
        String endStr   = DateTimeUtils.format(end);
        for (Booking b : bookingService.getConfirmedByHall(selectedHall.getHallId())) {
            if (DateTimeUtils.overlaps(startStr, endStr, b.getEventStart(), b.getEventEnd())) {
                showConflict("Overlaps with booking " + b.getBookingId()
                        + " (" + DateTimeUtils.toDisplayFormat(b.getEventStart())
                        + " – " + DateTimeUtils.toDisplayFormat(b.getEventEnd()) + ").");
                return;
            }
        }
        double hours  = DateTimeUtils.hoursBetween(startStr, endStr);
        double amount = hours * selectedHall.getRatePerHour();
        if (hours > 0)
            lblEstimate.setText(String.format("Estimated total: RM %.2f  (%.1f hrs × RM %.2f)",
                    amount, hours, selectedHall.getRatePerHour()));
    }

    @FXML
    public void handleBook() {
        clearError();
        if (selectedHall == null) { showError("Please select a hall."); return; }
        LocalDateTime start = getStartDateTime();
        LocalDateTime end   = getEndDateTime();
        if (start == null || end == null) { showError("Please pick start and end date/time."); return; }
        if (start.isBefore(LocalDateTime.now())) {
            showError("Cannot book in the past."); return;
        }
        if (!end.isAfter(start)) { showError("End time must be after start time."); return; }
        String startStr   = DateTimeUtils.format(start);
        String endStr     = DateTimeUtils.format(end);
        String customerId = SessionManager.getInstance().getCurrentCustomer().getCustomerId();
        String method     = comboPayment.getValue();
        String result = bookingService.createBooking(customerId, selectedHall.getHallId(),
                "NULL", startStr, endStr);
        if (result.startsWith("ERR:")) { showError(result.substring(4)); return; }
        String newBookingId = result;
        Booking latest = bookingService.getBookingById(newBookingId);
        if (latest == null) { showError("Booking created but could not retrieve record. Please check your bookings."); return; }

        if ("Cash".equals(method)) {
            // Cash: record payment immediately and show confirmation
            paymentService.recordPayment(latest.getBookingId(), latest.getTotalAmount(), method);
            MainApp.showInfo("Booking Confirmed!",
                    "Booking " + newBookingId + " confirmed.\n"
                    + "Amount: RM " + String.format("%.2f", latest.getTotalAmount()) + "\n"
                    + "Payment: Cash (pay at counter)");
            cancelBooking();
            loadHalls();
        } else {
            // Credit Card or Online Transfer: record base payment then go to payment screen
            paymentService.recordPayment(latest.getBookingId(), latest.getTotalAmount(), method);
            SessionManager.getInstance().setPendingPayment(
                    newBookingId, latest.getTotalAmount(), method);
            MainApp.switchScene("/fxml/customer/payment.fxml", 980, 640);
        }
    }

    @FXML
    public void cancelBooking() {
        bookingPanel.setVisible(false);
        bookingPanel.setManaged(false);
        dateStart.setValue(LocalDate.now());
        dateEnd  .setValue(LocalDate.now());
        startHour.getValueFactory().setValue(9);  startMin.getValueFactory().setValue(0);
        endHour  .getValueFactory().setValue(17); endMin  .getValueFactory().setValue(0);
        lblEstimate.setText(""); clearError(); hideConflict();
        hallTable.getSelectionModel().clearSelection();
        selectedHall = null;
    }

    @FXML public void applyFilter() {
        String type = typeFilter.getValue();
        if (type == null || type.equals("All Types")) { hallTable.setItems(allHalls); return; }
        hallTable.setItems(allHalls.filtered(h -> h.getHallType().equals(type)));
    }

    @FXML public void handleSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) { hallTable.setItems(allHalls); return; }
        hallTable.setItems(allHalls.filtered(h ->
                h.getHallName().toLowerCase().contains(kw) ||
                h.getHallType().toLowerCase().contains(kw)));
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/customer/dashboard.fxml",      980, 640); }
    @FXML public void goToBrowse()    { /* already here */ }
    @FXML public void goToBookings()  { MainApp.switchScene("/fxml/customer/booking_history.fxml",980, 640); }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/customer/issues.fxml",         980, 640); }
    @FXML public void goToProfile()   { MainApp.switchScene("/fxml/customer/profile.fxml",        980, 640); }
}
