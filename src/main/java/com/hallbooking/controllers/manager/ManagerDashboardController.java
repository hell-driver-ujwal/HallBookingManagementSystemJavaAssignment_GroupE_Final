package com.hallbooking.controllers.manager;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Issue;
import com.hallbooking.models.Payment;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.IssueService;
import com.hallbooking.services.PaymentService;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class ManagerDashboardController extends BaseController implements Initializable {

    @FXML private Label statMonthly, statYearly, statOpen, statBookings;

    @FXML private TableView<Issue>             issuesTable;
    @FXML private TableColumn<Issue, String>   iColId, iColTitle, iColHall, iColCust, iColDate, iColStatus;

    @FXML private TableView<Booking>           revenueTable;
    @FXML private TableColumn<Booking, String> rColId, rColHall, rColDate, rColHours, rColAmt, rColMethod;

    private final BookingService bookingService = new BookingService();
    private final IssueService   issueService   = new IssueService();
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        setupIssuesTable();
        setupRevenueTable();
        loadData();
    }

    private void setupIssuesTable() {
        iColId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIssueId()));
        iColTitle .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        iColHall  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        iColCust  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerId()));
        iColDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReportedAt().substring(0, 10)));
        iColStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void setupRevenueTable() {
        rColId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookingId()));
        rColHall  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        rColDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEventStart().substring(0, 10)));
        rColHours .setCellValueFactory(c -> new SimpleStringProperty(
                (int) c.getValue().getTotalHours() + "h"));
        rColAmt   .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getTotalAmount())));
        rColMethod.setCellValueFactory(c -> {
            Payment p = paymentService.getPaymentByBooking(c.getValue().getBookingId());
            return new SimpleStringProperty(p != null ? p.getPaymentMethod() : "—");
        });
    }

    private void loadData() {
        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        List<Issue>   open      = issueService.getOpenIssues();
        List<Booking> bookings  = bookingService.getAllBookings();
        List<Booking> confirmed = bookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus())).toList();

        statMonthly .setText(String.format("RM %.0f", bookingService.getRevenueByMonth(year, month)));
        statYearly  .setText(String.format("RM %.0f", bookingService.getRevenueByYear(year)));
        statOpen    .setText(String.valueOf(open.size()));
        statBookings.setText(String.valueOf(confirmed.size()));

        issuesTable .setItems(FXCollections.observableArrayList(open));
        revenueTable.setItems(FXCollections.observableArrayList(
                confirmed.subList(0, Math.min(confirmed.size(), 8))));
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/manager/dashboard.fxml",980, 640); }
    @FXML public void goToSales()     { MainApp.switchScene("/fxml/manager/sales.fxml",   980, 640); }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/manager/issues.fxml",  980, 640); }
}
