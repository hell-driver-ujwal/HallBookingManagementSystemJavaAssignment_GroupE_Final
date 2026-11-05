package com.hallbooking.controllers.manager;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Hall;
import com.hallbooking.models.Payment;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.HallService;
import com.hallbooking.services.PaymentService;
import com.hallbooking.utils.DateTimeUtils;
import com.hallbooking.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SalesDashboardController — Manager's operational sales view.
 *
 * Features (per assignment + additions):
 *   ✅ Weekly / Monthly / Yearly revenue stats
 *   ✅ Filter by year and month
 *   ✅ Revenue trend line chart
 *   ✅ Monthly revenue + bookings bar chart
 *   ✅ Hall type pie chart
 *   ✅ Peak booking hours bar chart
 *   ✅ Tabbed table panel (Weekly / Monthly / Booking Details)
 *   ✅ Export plain-text report
 *   ❌ No SST/tax (accounting level — Admin only)
 *   ❌ No all-time totals (Admin only)
 *   ❌ No top halls ranking (Admin only)
 *   ❌ No user activity stats (Admin only)
 *
 * Theme: Green (#1e5c3a / #2d7a50)
 */
public class SalesDashboardController extends BaseController implements Initializable {

    // ── Filters ──────────────────────────────────────────────────────
    @FXML private ComboBox<String> yearFilter, monthFilter;

    // ── Stat cards ───────────────────────────────────────────────────
    @FXML private Label statPeriod, statWeek, statMonth, statYear,
                        statCancelRate, statTotalBookings;

    // ── Charts ───────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> revenueLineChart;
    @FXML private BarChart<String, Number>  monthlyBarChart;
    @FXML private PieChart                  hallTypePieChart;
    @FXML private BarChart<String, Number>  peakHoursChart;
    @FXML private javafx.scene.layout.VBox pieLegend;

    // ── Tab buttons ──────────────────────────────────────────────────
    @FXML private Button tabWeekly, tabMonthly, tabDetails;

    // ── Table panels ─────────────────────────────────────────────────
    @FXML private VBox panelWeekly, panelMonthly, panelDetails;

    // ── Weekly table ─────────────────────────────────────────────────
    @FXML private TableView<WeeklySummary>           weeklyTable;
    @FXML private TableColumn<WeeklySummary, String> wColWeek, wColRange, wColCount, wColRevenue;

    // ── Monthly table ─────────────────────────────────────────────────
    @FXML private TableView<MonthlySummary>              monthlyTable;
    @FXML private TableColumn<MonthlySummary, String>    mColMonth, mColCount, mColRevenue,
                                                          mColAuditorium, mColBanquet, mColMeeting;

    // ── Details table ─────────────────────────────────────────────────
    @FXML private TableView<Booking>           detailTable;
    @FXML private TableColumn<Booking, String> dColId, dColHall, dColType,
                                               dColDate, dColHours, dColAmount, dColMethod;

    private final BookingService bookingService = new BookingService();
    private final HallService    hallService    = new HallService();
    private final PaymentService paymentService = new PaymentService();

    private int selectedYear, selectedMonth;

    // ── Inner summary models ──────────────────────────────────────────
    public static class WeeklySummary {
        public final String weekLabel, dateRange;
        public final long   count;
        public final double revenue;
        WeeklySummary(String w, String r, long c, double rev) {
            weekLabel = w; dateRange = r; count = c; revenue = rev;
        }
    }

    public static class MonthlySummary {
        public final String monthName;
        public final long   count;
        public final double revenue, auditorium, banquet, meeting;
        MonthlySummary(String m, long c, double rev, double a, double b, double mt) {
            monthName = m; count = c; revenue = rev;
            auditorium = a; banquet = b; meeting = mt;
        }
    }

    @Override protected String getLogoutTarget() { return "/fxml/login_staff.fxml"; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUsername();
        selectedMonth = 0;

        List<String> years = bookingService.getAllBookings().stream()
                .map(b -> b.getEventStart().substring(0, 4))
                .distinct().sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        if (years.isEmpty()) years.add(String.valueOf(LocalDate.now().getYear()));
        yearFilter.setItems(FXCollections.observableArrayList(years));
        selectedYear = Integer.parseInt(years.get(0));
        yearFilter.setValue(years.get(0));

        List<String> months = new ArrayList<>();
        months.add("All Months");
        for (int m = 1; m <= 12; m++)
            months.add(m + " — " + Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        monthFilter.setItems(FXCollections.observableArrayList(months));
        monthFilter.getSelectionModel().selectFirst();

        setupTables();
        loadAll();
        showTab("weekly");
    }

    // ── Tab switching ─────────────────────────────────────────────────
    @FXML public void showWeeklyTab()  { showTab("weekly"); }
    @FXML public void showMonthlyTab() { showTab("monthly"); }
    @FXML public void showDetailsTab() { showTab("details"); }

    private void showTab(String tab) {
        // Hide all panels
        setPanel(panelWeekly,  false);
        setPanel(panelMonthly, false);
        setPanel(panelDetails, false);
        // Reset tab button styles
        resetTabStyle(tabWeekly);
        resetTabStyle(tabMonthly);
        resetTabStyle(tabDetails);
        // Show selected
        switch (tab) {
            case "weekly"  -> { setPanel(panelWeekly,  true); activateTab(tabWeekly); }
            case "monthly" -> { setPanel(panelMonthly, true); activateTab(tabMonthly); }
            case "details" -> { setPanel(panelDetails, true); activateTab(tabDetails); }
        }
    }

    private void setPanel(VBox panel, boolean show) {
        if (panel == null) return;
        panel.setVisible(show); panel.setManaged(show);
    }

    private void resetTabStyle(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#8a8880;"
                + "-fx-font-weight:normal;-fx-border-color:transparent transparent #e8e4df transparent;"
                + "-fx-border-width:0 0 2 0;-fx-padding:10 20 10 20;-fx-cursor:hand;");
    }

    private void activateTab(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#1e5c3a;"
                + "-fx-font-weight:bold;-fx-border-color:transparent transparent #1e5c3a transparent;"
                + "-fx-border-width:0 0 2 0;-fx-padding:10 20 10 20;-fx-cursor:hand;");
    }

    // ── Load all ──────────────────────────────────────────────────────
    private void loadAll() {
        loadStatCards();
        buildRevenueLineChart();
        buildMonthlyBarChart();
        buildHallTypePie();
        buildPeakHoursChart();
        buildWeeklyTable();
        buildMonthlyTable();
        buildDetailTable();
    }

    // ── Stat cards ────────────────────────────────────────────────────
    private void loadStatCards() {
        int nowMonth = LocalDate.now().getMonthValue();
        int nowYear  = LocalDate.now().getYear();
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Booking> all = bookingService.getAllBookings();
        long cancelled    = all.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        double cancelRate = all.isEmpty() ? 0 : (cancelled * 100.0 / all.size());
        long confirmed    = all.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        double periodRev  = selectedMonth > 0
                ? bookingService.getRevenueByMonth(selectedYear, selectedMonth)
                : bookingService.getRevenueByYear(selectedYear);

        statPeriod       .setText(String.format("RM %.2f", periodRev));
        statWeek         .setText(String.format("RM %.2f", bookingService.getRevenueByWeek(weekStart)));
        statMonth        .setText(String.format("RM %.2f", bookingService.getRevenueByMonth(nowYear, nowMonth)));
        statYear         .setText(String.format("RM %.2f", bookingService.getRevenueByYear(nowYear)));
        statCancelRate   .setText(String.format("%.1f%%", cancelRate));
        statTotalBookings.setText(String.valueOf(confirmed));
    }

    // ── Charts ────────────────────────────────────────────────────────
    private void buildRevenueLineChart() {
        revenueLineChart.getData().clear();
        revenueLineChart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(selectedYear + " Revenue");
        for (int m = 1; m <= 12; m++)
            series.getData().add(new XYChart.Data<>(
                    Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    bookingService.getRevenueByMonth(selectedYear, m)));
        revenueLineChart.getData().add(series);
    }

    private void buildMonthlyBarChart() {
        monthlyBarChart.getData().clear();
        monthlyBarChart.setAnimated(false);
        XYChart.Series<String, Number> revSeries = new XYChart.Series<>();
        revSeries.setName("Revenue (RM)");
        XYChart.Series<String, Number> cntSeries = new XYChart.Series<>();
        cntSeries.setName("Bookings");

        List<Booking> yearBookings = bookingService.getAllBookings().stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus())
                        && b.getEventStart().startsWith(String.valueOf(selectedYear)))
                .collect(Collectors.toList());

        for (int m = 1; m <= 12; m++) {
            String prefix = String.format("%04d-%02d", selectedYear, m);
            String label  = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            long   count  = yearBookings.stream().filter(b -> b.getEventStart().startsWith(prefix)).count();
            double rev    = yearBookings.stream().filter(b -> b.getEventStart().startsWith(prefix))
                    .mapToDouble(Booking::getTotalAmount).sum();
            revSeries.getData().add(new XYChart.Data<>(label, rev));
            cntSeries.getData().add(new XYChart.Data<>(label, count));
        }
        monthlyBarChart.getData().addAll(revSeries, cntSeries);
    }

    private void buildHallTypePie() {
        hallTypePieChart.getData().clear();
        if (pieLegend != null) pieLegend.getChildren().clear();

        double audRev = 0, banRev = 0, metRev = 0;
        for (Booking b : bookingService.getAllBookings()) {
            if (!"CONFIRMED".equals(b.getStatus())) continue;
            Hall h = hallService.getHallById(b.getHallId());
            if (h == null) continue;
            switch (h.getHallType()) {
                case "AUDITORIUM"   -> audRev += b.getTotalAmount();
                case "BANQUET_HALL" -> banRev += b.getTotalAmount();
                case "MEETING_ROOM" -> metRev += b.getTotalAmount();
            }
        }

        // Slice colors matching the CSS defaults
        String[] colors = {"#2d7a50", "#4ab978", "#1e5c3a"};
        String[] names  = {"Auditorium", "Banquet Hall", "Meeting Room"};
        double[] values = {audRev, banRev, metRev};

        double total = audRev + banRev + metRev;
        boolean anyData = false;

        for (int i = 0; i < 3; i++) {
            if (values[i] <= 0) continue;
            anyData = true;
            PieChart.Data slice = new PieChart.Data(names[i], values[i]);
            hallTypePieChart.getData().add(slice);

            // Tooltip on hover
            final String tip = String.format("%s%nRM %.2f  (%.1f%%)",
                    names[i], values[i], total > 0 ? values[i] * 100.0 / total : 0);
            javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(tip);
            tt.setStyle("-fx-font-size:11px;");
            // attach tooltip after scene is ready
            final int idx = i;
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    javafx.scene.control.Tooltip.install(newNode, tt);
                    // Force correct color — JavaFX sometimes ignores CSS order
                    newNode.setStyle("-fx-pie-color:" + colors[idx] + ";");
                }
            });

            // Manual legend row
            if (pieLegend != null) {
                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                javafx.scene.shape.Rectangle swatch = new javafx.scene.shape.Rectangle(12, 12);
                swatch.setFill(javafx.scene.paint.Color.web(colors[i]));
                swatch.setArcWidth(3); swatch.setArcHeight(3);

                javafx.scene.control.Label lbl = new javafx.scene.control.Label(
                        names[i] + " — RM " + String.format("%,.0f", values[i])
                        + " (" + String.format("%.1f", total > 0 ? values[i] * 100.0 / total : 0) + "%)");
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#1a2820;");

                row.getChildren().addAll(swatch, lbl);
                pieLegend.getChildren().add(row);
            }
        }

        if (!anyData) {
            hallTypePieChart.getData().add(new PieChart.Data("No Data", 1));
            if (pieLegend != null) {
                javafx.scene.control.Label empty = new javafx.scene.control.Label("No confirmed bookings");
                empty.setStyle("-fx-font-size:11px;-fx-text-fill:#b0ada8;");
                pieLegend.getChildren().add(empty);
            }
        }

        hallTypePieChart.setAnimated(false);
    }

    private void buildPeakHoursChart() {
        peakHoursChart.getData().clear();
        peakHoursChart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bookings");
        Map<Integer, Long> countByHour = new TreeMap<>();
        for (Booking b : bookingService.getAllBookings()) {
            if (!"CONFIRMED".equals(b.getStatus())) continue;
            try {
                int hour = Integer.parseInt(b.getEventStart().substring(11, 13));
                countByHour.merge(hour, 1L, Long::sum);
            } catch (Exception ignored) {}
        }
        for (int h = 6; h <= 22; h++)
            series.getData().add(new XYChart.Data<>(
                    String.format("%02d:00", h), countByHour.getOrDefault(h, 0L)));
        peakHoursChart.getData().add(series);
    }

    // ── Tables ────────────────────────────────────────────────────────
    private void buildWeeklyTable() {
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("d MMM");
        List<WeeklySummary> summaries = new ArrayList<>();
        LocalDate rangeStart = selectedMonth > 0
                ? LocalDate.of(selectedYear, selectedMonth, 1)
                : LocalDate.of(selectedYear, 1, 1);
        LocalDate rangeEnd = selectedMonth > 0
                ? rangeStart.with(TemporalAdjusters.lastDayOfMonth())
                : LocalDate.of(selectedYear, 12, 31);
        LocalDate wStart = rangeStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int weekNum = 1;
        while (!wStart.isAfter(rangeEnd)) {
            LocalDate wEnd = wStart.plusDays(6);
            List<Booking> inWeek = bookingService.getBookingsForWeek(wStart);
            if (!inWeek.isEmpty()) {
                double revenue = inWeek.stream().mapToDouble(Booking::getTotalAmount).sum();
                summaries.add(new WeeklySummary("Week " + weekNum,
                        wStart.format(fmt) + " – " + wEnd.format(fmt), inWeek.size(), revenue));
            }
            wStart = wStart.plusWeeks(1); weekNum++;
        }
        weeklyTable.setItems(FXCollections.observableArrayList(summaries));
    }

    private void buildMonthlyTable() {
        List<MonthlySummary> summaries = new ArrayList<>();
        List<Booking> all = bookingService.getAllBookings().stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus())
                        && b.getEventStart().startsWith(String.valueOf(selectedYear)))
                .collect(Collectors.toList());
        for (int m = 1; m <= 12; m++) {
            String prefix = String.format("%04d-%02d", selectedYear, m);
            List<Booking> inMonth = all.stream()
                    .filter(b -> b.getEventStart().startsWith(prefix)).collect(Collectors.toList());
            if (inMonth.isEmpty()) continue;
            double rev = inMonth.stream().mapToDouble(Booking::getTotalAmount).sum();
            double aud = inMonth.stream().filter(b -> isType(b, "AUDITORIUM"))
                    .mapToDouble(Booking::getTotalAmount).sum();
            double ban = inMonth.stream().filter(b -> isType(b, "BANQUET_HALL"))
                    .mapToDouble(Booking::getTotalAmount).sum();
            double met = inMonth.stream().filter(b -> isType(b, "MEETING_ROOM"))
                    .mapToDouble(Booking::getTotalAmount).sum();
            summaries.add(new MonthlySummary(
                    Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedYear,
                    inMonth.size(), rev, aud, ban, met));
        }
        monthlyTable.setItems(FXCollections.observableArrayList(summaries));
    }

    private void buildDetailTable() {
        List<Booking> filtered = bookingService.getAllBookings().stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus())
                        && b.getEventStart().startsWith(String.valueOf(selectedYear)))
                .collect(Collectors.toList());
        if (selectedMonth > 0) {
            String prefix = String.format("%04d-%02d", selectedYear, selectedMonth);
            filtered = filtered.stream()
                    .filter(b -> b.getEventStart().startsWith(prefix)).collect(Collectors.toList());
        }
        detailTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void setupTables() {
        wColWeek   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().weekLabel));
        wColRange  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().dateRange));
        wColCount  .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().count)));
        wColRevenue.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().revenue)));

        mColMonth     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().monthName));
        mColCount     .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().count)));
        mColRevenue   .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().revenue)));
        mColAuditorium.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().auditorium)));
        mColBanquet   .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().banquet)));
        mColMeeting   .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().meeting)));

        dColId    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookingId()));
        dColHall  .setCellValueFactory(c -> {
            Hall h = hallService.getHallById(c.getValue().getHallId());
            return new SimpleStringProperty(h != null ? h.getHallName() : c.getValue().getHallId());
        });
        dColType  .setCellValueFactory(c -> {
            Hall h = hallService.getHallById(c.getValue().getHallId());
            return new SimpleStringProperty(h != null ? h.getHallType() : "—");
        });
        dColDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEventStart().substring(0, 10)));
        dColHours .setCellValueFactory(c -> new SimpleStringProperty(
                (int) c.getValue().getTotalHours() + "h"));
        dColAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().getTotalAmount())));
        dColMethod.setCellValueFactory(c -> {
            Payment p = paymentService.getPaymentByBooking(c.getValue().getBookingId());
            return new SimpleStringProperty(p != null ? p.getPaymentMethod() : "—");
        });
    }

    // ── Filter ────────────────────────────────────────────────────────
    @FXML
    public void applyFilter() {
        String yr = yearFilter.getValue();
        String mo = monthFilter.getValue();
        if (yr != null) selectedYear = Integer.parseInt(yr);
        selectedMonth = (mo != null && !mo.startsWith("All"))
                ? Integer.parseInt(mo.split(" — ")[0]) : 0;
        loadAll();
    }

    // ── Export ────────────────────────────────────────────────────────
    @FXML
    public void handleExport() {
        String periodLabel = selectedMonth > 0
                ? Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedYear
                : "Full Year " + selectedYear;

        StringBuilder sb = new StringBuilder();
        sb.append("=======================================================\n");
        sb.append("      HALL SYMPHONY — MANAGER SALES REPORT\n");
        sb.append("=======================================================\n");
        sb.append("Period      : ").append(periodLabel).append("\n");
        sb.append("Generated   : ").append(DateTimeUtils.now()).append("\n");
        sb.append("Generated by: ").append(SessionManager.getInstance().getDisplayName())
          .append(" (Manager)\n");
        sb.append("-------------------------------------------------------\n\n");

        sb.append("REVENUE SUMMARY\n");
        sb.append(String.format("  This Week      : %s%n", statWeek.getText()));
        sb.append(String.format("  This Month     : %s%n", statMonth.getText()));
        sb.append(String.format("  This Year      : %s%n", statYear.getText()));
        sb.append(String.format("  Selected Period: %s%n", statPeriod.getText()));
        sb.append(String.format("  Cancel Rate    : %s%n", statCancelRate.getText()));
        sb.append(String.format("  Total Confirmed: %s bookings%n%n", statTotalBookings.getText()));

        sb.append("MONTHLY BREAKDOWN\n");
        sb.append(String.format("  %-28s %8s %12s %12s %12s %12s%n",
                "Month","Bookings","Revenue","Auditorium","Banquet","Meeting"));
        sb.append("  ").append("-".repeat(86)).append("\n");
        for (MonthlySummary ms : monthlyTable.getItems())
            sb.append(String.format("  %-28s %8d %12s %12s %12s %12s%n",
                    ms.monthName, ms.count,
                    String.format("RM %.2f", ms.revenue),
                    String.format("RM %.2f", ms.auditorium),
                    String.format("RM %.2f", ms.banquet),
                    String.format("RM %.2f", ms.meeting)));
        sb.append("\n");

        sb.append("CANCELLATION SUMMARY\n");
        List<Booking> all = bookingService.getAllBookings();
        long cancelled = all.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        long confirmed = all.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        sb.append(String.format("  Confirmed   : %d%n", confirmed));
        sb.append(String.format("  Cancelled   : %d%n", cancelled));
        sb.append(String.format("  Cancel Rate : %.1f%%%n%n",
                all.isEmpty() ? 0 : cancelled * 100.0 / all.size()));

        sb.append("=======================================================\n");
        sb.append("                 END OF REPORT\n");
        sb.append("=======================================================\n");

        String fileName = "ManagerSalesReport_" + periodLabel.replace(" ", "_")
                + "_" + LocalDate.now() + ".txt";
        try {
            java.nio.file.Path path = Paths.get(System.getProperty("user.dir"), fileName);
            Files.writeString(path, sb.toString());
            MainApp.showInfo("Report Exported", "Saved to:\n" + path.toAbsolutePath());
        } catch (Exception e) {
            MainApp.showError("Export Failed", e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────
    private boolean isType(Booking b, String type) {
        Hall h = hallService.getHallById(b.getHallId());
        return h != null && h.getHallType().equals(type);
    }

    // ── Navigation ────────────────────────────────────────────────────
    @FXML public void goToDashboard() { MainApp.switchScene("/fxml/manager/dashboard.fxml", 980, 640); }
    @FXML public void goToSales()     { /* already here */ }
    @FXML public void goToIssues()    { MainApp.switchScene("/fxml/manager/issues.fxml",    980, 640); }
}
