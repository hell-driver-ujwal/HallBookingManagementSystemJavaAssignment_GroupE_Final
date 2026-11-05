package com.hallbooking.controllers.admin;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Hall;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.HallService;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class BookingOverviewController extends BaseController implements Initializable {

    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField        searchField;

    @FXML private TableView<Booking>           bookingTable;
    @FXML private TableColumn<Booking, String> colId, colCust, colHall, colStart,
                                               colEnd, colHours, colAmount, colStatus;

    private final BookingService bookingService = new BookingService();
    private final HallService    hallService    = new HallService();
    private ObservableList<Booking> allBookings;

    @Override
    protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        statusFilter.setItems(FXCollections.observableArrayList("All", "CONFIRMED", "CANCELLED"));
        statusFilter.getSelectionModel().selectFirst();
        setupTable();
        loadBookings();
    }

    private void setupTable() {
        colId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookingId()));
        colCust  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerId()));
        colHall  .setCellValueFactory(c -> {
            Hall h = hallService.getHallById(c.getValue().getHallId());
            return new SimpleStringProperty(h != null ? h.getHallName() : c.getValue().getHallId());
        });
        colStart .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEventStart()));
        colEnd   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEventEnd()));
        colHours .setCellValueFactory(c -> new SimpleStringProperty(
                (int) c.getValue().getTotalHours() + "h"));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getTotalAmount())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    private void loadBookings() {
        allBookings = FXCollections.observableArrayList(bookingService.getAllBookings());
        bookingTable.setItems(allBookings);
    }

    @FXML
    public void applyFilter() {
        String f = statusFilter.getValue();
        if (f == null || f.equals("All")) { bookingTable.setItems(allBookings); return; }
        bookingTable.setItems(allBookings.filtered(b -> b.getStatus().equals(f)));
    }

    @FXML
    public void handleSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) { bookingTable.setItems(allBookings); return; }
        bookingTable.setItems(allBookings.filtered(b ->
                b.getBookingId().toLowerCase().contains(kw) ||
                b.getCustomerId().toLowerCase().contains(kw) ||
                b.getHallId().toLowerCase().contains(kw)));
    }

    @FXML public void goToDashboard()       { MainApp.switchScene("/fxml/admin/dashboard.fxml",       980, 640); }
    @FXML public void goToStaffManagement() { MainApp.switchScene("/fxml/admin/staff_management.fxml",980, 640); }
    @FXML public void goToUserManagement()  { MainApp.switchScene("/fxml/admin/user_management.fxml", 980, 640); }
    @FXML public void goToSales()           { MainApp.switchScene("/fxml/admin/sales.fxml",           980, 640); }
    @FXML public void goToBookings()        { /* already here */ }
}
