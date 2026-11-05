package com.hallbooking.controllers.customer;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Issue;
import com.hallbooking.models.IssueAssignment;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.IssueService;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class IssueReportController extends BaseController implements Initializable {

    @FXML private TableView<Issue>           issueTable;
    @FXML private TableColumn<Issue, String> colId, colBooking, colHall, colTitle,
                                             colDate, colStatus, colResponse;

    @FXML private ComboBox<String> comboBooking;
    @FXML private TextField        fieldTitle;
    @FXML private TextArea         fieldDesc;
    @FXML private ComboBox<String> comboPriority;

    private final IssueService   issueService   = new IssueService();
    private final BookingService bookingService = new BookingService();
    private List<Booking> myConfirmedBookings;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_customer.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        if (comboPriority != null)
            comboPriority.setItems(javafx.collections.FXCollections.observableArrayList("LOW","MEDIUM","HIGH"));
        if (comboPriority != null) comboPriority.getSelectionModel().select("MEDIUM");
        setupTable();
        loadBookingsIntoCombo();
        loadIssues();
    }

    private void setupTable() {
        colId      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIssueId()));
        colBooking .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookingId()));
        colHall    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHallId()));
        colTitle   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colDate    .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReportedAt().substring(0, 10)));
        colStatus  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colResponse.setCellValueFactory(c -> {
            IssueAssignment a = issueService.getAssignmentByIssue(c.getValue().getIssueId());
            return new SimpleStringProperty(a != null ? a.getManagerResponse() : "Awaiting response");
        });
    }

    private void loadBookingsIntoCombo() {
        String customerId = SessionManager.getInstance().getCurrentCustomer().getCustomerId();
        myConfirmedBookings = bookingService.getBookingsByCustomer(customerId).stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .collect(Collectors.toList());
        List<String> options = myConfirmedBookings.stream()
                .map(b -> b.getBookingId() + " — " + b.getHallId()
                        + " (" + b.getEventStart().substring(0, 10) + ")")
                .collect(Collectors.toList());
        comboBooking.setItems(FXCollections.observableArrayList(options));
        if (!options.isEmpty()) comboBooking.getSelectionModel().selectFirst();
    }

    private void loadIssues() {
        String customerId = SessionManager.getInstance().getCurrentCustomer().getCustomerId();
        issueTable.setItems(FXCollections.observableArrayList(
                issueService.getIssuesByCustomer(customerId)));
    }

    @FXML public void onRowSelected() {
        Issue selected = issueTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fieldTitle.setText(selected.getTitle());
            fieldDesc .setText(selected.getDescription());
        }
    }

    @FXML
    public void handleSubmit() {
        clearError();
        String bookingSel = comboBooking.getValue();
        if (bookingSel == null) { showError("Please select a related booking."); return; }
        String bookingId = bookingSel.split(" — ")[0];
        String hallId    = myConfirmedBookings.stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .map(Booking::getHallId).findFirst().orElse("");
        String title = fieldTitle.getText().trim();
        String desc  = fieldDesc.getText().trim();

        String customerId = SessionManager.getInstance().getCurrentCustomer().getCustomerId();
        String priority = (comboPriority != null && comboPriority.getValue() != null)
                ? comboPriority.getValue() : "MEDIUM";
        String err = issueService.reportIssue(customerId, bookingId, hallId, title, desc, priority);
        if (err != null) { showError(err); return; }
        MainApp.showInfo("Issue Submitted",
                "Your issue has been reported. A manager will respond shortly.");
        handleClear();
        loadIssues();
    }

    @FXML
    public void handleClear() {
        fieldTitle.clear(); fieldDesc.clear();
        if (comboPriority != null) comboPriority.getSelectionModel().select("MEDIUM");
        issueTable.getSelectionModel().clearSelection();
        clearError();
    }

    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/customer/dashboard.fxml",      980, 640); }
    @FXML public void goToBrowse()    { MainApp.switchScene("/fxml/customer/browse_halls.fxml",   980, 640); }
    @FXML public void goToBookings()  { MainApp.switchScene("/fxml/customer/booking_history.fxml",980, 640); }
    @FXML public void goToIssues()    { /* already here */ }
    @FXML public void goToProfile()   { MainApp.switchScene("/fxml/customer/profile.fxml",        980, 640); }
}
