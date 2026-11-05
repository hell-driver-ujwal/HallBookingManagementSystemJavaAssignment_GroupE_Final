package com.hallbooking.controllers.customer;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.HallService;
import com.hallbooking.services.IssueService;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class CustomerDashboardController extends BaseController implements Initializable {

    // Stat cards
    @FXML private Label statUpcoming, statTotal, statIssues, statSpent;

    // Reminder banner (visible when booking is within 48 hours)
    @FXML private VBox reminderBanner;
    @FXML private Label reminderText;

    @FXML private TableView<Booking>           upcomingTable;
    @FXML private TableColumn<Booking, String> colBookId, colHall, colDate, colTime,
                                               colAmount, colStatus;

    private final BookingService bookingService = new BookingService();
    private final HallService    hallService    = new HallService();
    private final IssueService   issueService   = new IssueService();

    @Override
    protected String getLogoutTarget() { return "/fxml/login_customer.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        setupTable();
        loadData();
        checkReminder();
    }

    private void setupTable() {
        colBookId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookingId()));
        colHall  .setCellValueFactory(c -> {
            var h = hallService.getHallById(c.getValue().getHallId());
            return new SimpleStringProperty(h != null ? h.getHallName() : c.getValue().getHallId());
        });
        colDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEventStart().substring(0, 10)));
        colTime  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEventStart().substring(11)
                + " – " + c.getValue().getEventEnd().substring(11)));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getTotalAmount())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void loadData() {
        String customerId = SessionManager.getInstance().getCurrentCustomer().getCustomerId();
        List<Booking> upcoming = bookingService.getUpcomingByCustomer(customerId);
        List<Booking> all      = bookingService.getBookingsByCustomer(customerId);
        long openIssues = issueService.getIssuesByCustomer(customerId).stream()
                .filter(i -> !i.isResolved()).count();
        double spent = all.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .mapToDouble(Booking::getTotalAmount).sum();

        statUpcoming.setText(String.valueOf(upcoming.size()));
        statTotal   .setText(String.valueOf(all.size()));
        statIssues  .setText(String.valueOf(openIssues));
        statSpent   .setText(String.format("RM %.0f", spent));
        upcomingTable.setItems(FXCollections.observableArrayList(upcoming));
    }

    // ── 48-hour reminder banner ────────────────────────────────────
    private void checkReminder() {
        if (reminderBanner == null) return;
        String customerId = SessionManager.getInstance().getCurrentCustomer().getCustomerId();
        List<Booking> upcoming = bookingService.getUpcomingByCustomer(customerId);

        // Find the soonest booking within next 48 hours
        Booking soonest = upcoming.stream()
                .filter(b -> {
                    try {
                        long hours = ChronoUnit.HOURS.between(
                                java.time.LocalDateTime.now(),
                                DateTimeUtils.parse(b.getEventStart()));
                        return hours >= 0 && hours <= 48;
                    } catch (Exception e) { return false; }
                })
                .min((a, b2) -> a.getEventStart().compareTo(b2.getEventStart()))
                .orElse(null);

        if (soonest != null) {
            long hoursLeft = ChronoUnit.HOURS.between(
                    java.time.LocalDateTime.now(),
                    DateTimeUtils.parse(soonest.getEventStart()));
            var hall = hallService.getHallById(soonest.getHallId());
            String hallName = hall != null ? hall.getHallName() : soonest.getHallId();
            String timeLabel = hoursLeft <= 1 ? "less than 1 hour"
                    : hoursLeft + " hours";
            reminderText.setText("⏰  Reminder: Your booking at " + hallName
                    + " (" + soonest.getBookingId() + ") starts in " + timeLabel
                    + " — " + DateTimeUtils.toDisplayFormat(soonest.getEventStart()));
            reminderBanner.setVisible(true);
            reminderBanner.setManaged(true);
        } else {
            reminderBanner.setVisible(false);
            reminderBanner.setManaged(false);
        }
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/customer/dashboard.fxml",      980, 640); }
    @FXML public void goToBrowse()    { MainApp.switchScene("/fxml/customer/browse_halls.fxml",   980, 640); }
    @FXML public void goToBookings()  { MainApp.switchScene("/fxml/customer/booking_history.fxml",980, 640); }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/customer/issues.fxml",         980, 640); }
    @FXML public void goToProfile()   { MainApp.switchScene("/fxml/customer/profile.fxml",        980, 640); }
}
