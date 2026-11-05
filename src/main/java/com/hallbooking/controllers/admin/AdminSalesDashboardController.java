package com.hallbooking.controllers.admin;

import com.hallbooking.MainApp;
import com.hallbooking.controllers.BaseController;
import com.hallbooking.models.Booking;
import com.hallbooking.models.Hall;
import com.hallbooking.models.Payment;
import com.hallbooking.models.User;
import com.hallbooking.services.BookingService;
import com.hallbooking.services.HallService;
import com.hallbooking.services.PaymentService;
import com.hallbooking.services.UserService;
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
 * AdminSalesDashboardController — Admin's full financial & strategic view.
 *
 * Additional features vs Manager:
 *   ✅ SST/tax breakdown (8% Malaysia SST)
 *   ✅ All-time totals (gross / net)
 *   ✅ Top halls ranking bar chart
 *   ✅ User activity stats (new registrations, active customers)
 *   ✅ Richer export report (includes tax breakdown)
 *   ✅ Tabbed table panel same as manager
 *
 * Theme: Navy blue (#1a2850 / #2d4080)
 */
public class AdminSalesDashboardController extends BaseController implements Initializable {

    private static final double SST_RATE = 0.08;

    // ── Filters ──────────────────────────────────────────────────────
    @FXML private ComboBox<String> yearFilter, monthFilter;

    // ── Stat cards — shared with manager ─────────────────────────────
    @FXML private Label statPeriod, statWeek, statMonth, statYear,
                        statCancelRate, statTotalBookings;

    // ── Admin-exclusive stat cards ────────────────────────────────────
    @FXML private Label statGrossTotal, statSstAmount, statNetTotal;
    @FXML private Label statTotalUsers, statActiveCustomers, statNewThisMonth;

    // ── Charts — shared with manager ──────────────────────────────────
    @FXML private LineChart<String, Number> revenueLineChart;
    @FXML private BarChart<String, Number>  monthlyBarChart;
    @FXML private PieChart                  hallTypePieChart;
    @FXML private VBox                      pieLegend;
    @FXML private BarChart<String, Number>  peakHoursChart;

    // ── Admin-exclusive charts ────────────────────────────────────────
    @FXML private BarChart<Number, String>  hallRankingChart;
    @FXML private BarChart<String, Number>  userActivityChart;

    // ── Tab buttons ──────────────────────────────────────────────────
    @FXML private Button tabWeekly, tabMonthly, tabDetails, tabHalls;

    // ── Table panels ─────────────────────────────────────────────────
    @FXML private VBox panelWeekly, panelMonthly, panelDetails, panelHalls;

    // ── Weekly table ─────────────────────────────────────────────────
    @FXML private TableView<WeeklySummary>           weeklyTable;
    @FXML private TableColumn<WeeklySummary, String> wColWeek, wColRange, wColCount, wColRevenue;

    // ── Monthly table ─────────────────────────────────────────────────
    @FXML private TableView<MonthlySummary>              monthlyTable;
    @FXML private TableColumn<MonthlySummary, String>    mColMonth, mColCount, mColRevenue,
                                                          mColGross, mColSst, mColNet;

    // ── Details table ─────────────────────────────────────────────────
    @FXML private TableView<Booking>           detailTable;
    @FXML private TableColumn<Booking, String> dColId, dColHall, dColType,
                                               dColDate, dColHours, dColAmount, dColMethod;

    // ── Top halls table ───────────────────────────────────────────────
    @FXML private TableView<HallRevenueSummary>           hallsTable;
    @FXML private TableColumn<HallRevenueSummary, String> hColRank, hColHall, hColType,
                                                           hColBookings, hColRevenue, hColShare;

    private final BookingService bookingService = new BookingService();
    private final HallService    hallService    = new HallService();
    private final PaymentService paymentService = new PaymentService();
    private final UserService    userService    = new UserService();

    private int selectedYear, selectedMonth;

    // ── Inner summary models (defined locally — no cross-package dependency) ──
    public static class WeeklySummary {
        public final String weekLabel, dateRange;
        public final long   count;
        public final double revenue;
        public WeeklySummary(String w, String r, long c, double rev) {
            weekLabel = w; dateRange = r; count = c; revenue = rev;
        }
    }

    public static class MonthlySummary {
        public final String monthName;
        public final long   count;
        public final double revenue, auditorium, banquet, meeting;
        public MonthlySummary(String m, long c, double rev, double a, double b, double mt) {
            monthName = m; count = c; revenue = rev;
            auditorium = a; banquet = b; meeting = mt;
        }
    }

    // ── Inner model for hall revenue ranking ──────────────────────────
    public static class HallRevenueSummary {
        final int    rank;
        final String hallName, hallType;
        final long   bookings;
        final double revenue, sharePercent;
        HallRevenueSummary(int r, String n, String t, long b, double rev, double share) {
            rank = r; hallName = n; hallType = t; bookings = b; revenue = rev; sharePercent = share;
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
    @FXML public void showHallsTab()   { showTab("halls"); }

    private void showTab(String tab) {
        setPanel(panelWeekly,  false); setPanel(panelMonthly, false);
        setPanel(panelDetails, false); setPanel(panelHalls,   false);
        resetTabStyle(tabWeekly); resetTabStyle(tabMonthly);
        resetTabStyle(tabDetails); resetTabStyle(tabHalls);
        switch (tab) {
            case "weekly"  -> { setPanel(panelWeekly,  true); activateTab(tabWeekly); }
            case "monthly" -> { setPanel(panelMonthly, true); activateTab(tabMonthly); }
            case "details" -> { setPanel(panelDetails, true); activateTab(tabDetails); }
            case "halls"   -> { setPanel(panelHalls,   true); activateTab(tabHalls); }
        }
    }

    private void setPanel(VBox p, boolean show) {
        if (p == null) return; p.setVisible(show); p.setManaged(show);
    }

    private void resetTabStyle(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#8a8880;"
                + "-fx-font-weight:normal;-fx-border-color:transparent transparent #e8e4df transparent;"
                + "-fx-border-width:0 0 2 0;-fx-padding:10 20 10 20;-fx-cursor:hand;");
    }

    private void activateTab(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#1a2850;"
                + "-fx-font-weight:bold;-fx-border-color:transparent transparent #1a2850 transparent;"
                + "-fx-border-width:0 0 2 0;-fx-padding:10 20 10 20;-fx-cursor:hand;");
    }

    // ── Load all ──────────────────────────────────────────────────────
    private void loadAll() {
        loadStatCards();
        buildRevenueLineChart();
        buildMonthlyBarChart();
        buildHallTypePie();
        buildPeakHoursChart();
        buildHallRankingChart();
        buildUserActivityChart();
        buildWeeklyTable();
        buildMonthlyTable();
        buildDetailTable();
        buildHallsTable();
    }

    // ── Stat cards ────────────────────────────────────────────────────
    private void loadStatCards() {
        int nowMonth = LocalDate.now().getMonthValue();
        int nowYear  = LocalDate.now().getYear();
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<Booking> all = bookingService.getAllBookings();
        long cancelled = all.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();
        long confirmed = all.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        double cancelRate = all.isEmpty() ? 0 : (cancelled * 100.0 / all.size());
        double periodRev = selectedMonth > 0
                ? bookingService.getRevenueByMonth(selectedYear, selectedMonth)
                : bookingService.getRevenueByYear(selectedYear);

        statPeriod       .setText(String.format("RM %.2f", periodRev));
        statWeek         .setText(String.format("RM %.2f", bookingService.getRevenueByWeek(weekStart)));
        statMonth        .setText(String.format("RM %.2f", bookingService.getRevenueByMonth(nowYear, nowMonth)));
        statYear         .setText(String.format("RM %.2f", bookingService.getRevenueByYear(nowYear)));
        statCancelRate   .setText(String.format("%.1f%%", cancelRate));
        statTotalBookings.setText(String.valueOf(confirmed));

        // ── Admin-exclusive: SST breakdown ────────────────────────────
        double gross = bookingService.getTotalRevenue();
        double sst   = gross * SST_RATE;
        double net   = gross - sst;
        if (statGrossTotal != null) statGrossTotal.setText(String.format("RM %.2f", gross));
        if (statSstAmount  != null) statSstAmount .setText(String.format("RM %.2f", sst));
        if (statNetTotal   != null) statNetTotal  .setText(String.format("RM %.2f", net));

        // ── Admin-exclusive: User activity ────────────────────────────
        List<User> allUsers = userService.getAllUsers();
        long totalUsers     = allUsers.size();
        long activeCustomers = allUsers.stream()
                .filter(u -> "CUSTOMER".equals(u.getRole()) && u.isActive()).count();
        String thisMonthPrefix = String.format("%04d-%02d", nowYear, nowMonth);
        long newThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt().startsWith(thisMonthPrefix)).count();

        if (statTotalUsers      != null) statTotalUsers     .setText(String.valueOf(totalUsers));
        if (statActiveCustomers != null) statActiveCustomers.setText(String.valueOf(activeCustomers));
        if (statNewThisMonth    != null) statNewThisMonth   .setText(String.valueOf(newThisMonth));
    }

    // ── Shared charts ─────────────────────────────────────────────────
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
            revSeries.getData().add(new XYChart.Data<>(label,
                    yearBookings.stream().filter(b -> b.getEventStart().startsWith(prefix))
                            .mapToDouble(Booking::getTotalAmount).sum()));
            cntSeries.getData().add(new XYChart.Data<>(label,
                    yearBookings.stream().filter(b -> b.getEventStart().startsWith(prefix)).count()));
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

        String[] colors = {"#2d7a50", "#4ab978", "#1e5c3a"};
        String[] names  = {"Auditorium", "Banquet Hall", "Meeting Room"};
        double[] values = {audRev, banRev, metRev};
        double   total  = audRev + banRev + metRev;
        boolean  anyData = false;

        for (int i = 0; i < 3; i++) {
            if (values[i] <= 0) continue;
            anyData = true;
            PieChart.Data slice = new PieChart.Data(names[i], values[i]);
            hallTypePieChart.getData().add(slice);

            final String tip = String.format("%s%nRM %.2f  (%.1f%%)",
                    names[i], values[i], total > 0 ? values[i] * 100.0 / total : 0);
            Tooltip tt = new Tooltip(tip);
            tt.setStyle("-fx-font-size:11px;");
            final int idx = i;
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip.install(newNode, tt);
                    newNode.setStyle("-fx-pie-color:" + colors[idx] + ";");
                }
            });

            if (pieLegend != null) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                Rectangle swatch = new Rectangle(12, 12);
                swatch.setFill(Color.web(colors[i]));
                swatch.setArcWidth(3); swatch.setArcHeight(3);
                Label lbl = new Label(
                        names[i] + " — RM " + String.format("%,.0f", values[i])
                        + " (" + String.format("%.1f", total > 0 ? values[i] * 100.0 / total : 0) + "%)");
                lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#1a2850;");
                row.getChildren().addAll(swatch, lbl);
                pieLegend.getChildren().add(row);
            }
        }

        if (!anyData) {
            hallTypePieChart.getData().add(new PieChart.Data("No Data", 1));
            if (pieLegend != null) {
                Label empty = new Label("No confirmed bookings");
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

    // ── Admin-exclusive charts ────────────────────────────────────────
    private void buildHallRankingChart() {
        if (hallRankingChart == null) return;
        hallRankingChart.getData().clear();
        hallRankingChart.setAnimated(false);
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Revenue (RM)");
        Map<String, Double> revenueByHall = new LinkedHashMap<>();
        for (Booking b : bookingService.getAllBookings()) {
            if (!"CONFIRMED".equals(b.getStatus())) continue;
            revenueByHall.merge(b.getHallId(), b.getTotalAmount(), Double::sum);
        }
        revenueByHall.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> {
                    Hall h = hallService.getHallById(e.getKey());
                    series.getData().add(new XYChart.Data<>(
                            e.getValue(), h != null ? h.getHallName() : e.getKey()));
                });
        hallRankingChart.getData().add(series);
    }

    private void buildUserActivityChart() {
        if (userActivityChart == null) return;
        userActivityChart.getData().clear();
        userActivityChart.setAnimated(false);
        XYChart.Series<String, Number> regSeries = new XYChart.Series<>();
        regSeries.setName("New Registrations");
        XYChart.Series<String, Number> bookSeries = new XYChart.Series<>();
        bookSeries.setName("New Bookings");

        List<User>    allUsers    = userService.getAllUsers();
        List<Booking> allBookings = bookingService.getAllBookings();

        for (int m = 1; m <= 12; m++) {
            String prefix = String.format("%04d-%02d", selectedYear, m);
            String label  = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            long regs  = allUsers.stream()
                    .filter(u -> "CUSTOMER".equals(u.getRole())
                            && u.getCreatedAt().startsWith(prefix)).count();
            long books = allBookings.stream()
                    .filter(b -> b.getBookedAt().startsWith(prefix)).count();
            regSeries .getData().add(new XYChart.Data<>(label, regs));
            bookSeries.getData().add(new XYChart.Data<>(label, books));
        }
        userActivityChart.getData().addAll(regSeries, bookSeries);
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

    private void buildHallsTable() {
        if (hallsTable == null) return;
        Map<String, Double> revenueByHall = new LinkedHashMap<>();
        Map<String, Long>   bookingsByHall = new LinkedHashMap<>();
        double totalRev = 0;
        for (Booking b : bookingService.getAllBookings()) {
            if (!"CONFIRMED".equals(b.getStatus())) continue;
            revenueByHall.merge(b.getHallId(), b.getTotalAmount(), Double::sum);
            bookingsByHall.merge(b.getHallId(), 1L, Long::sum);
            totalRev += b.getTotalAmount();
        }
        final double finalTotal = totalRev > 0 ? totalRev : 1;
        List<HallRevenueSummary> list = new ArrayList<>();
        int[] rank = {1};
        revenueByHall.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> {
                    Hall h = hallService.getHallById(e.getKey());
                    list.add(new HallRevenueSummary(
                            rank[0]++,
                            h != null ? h.getHallName() : e.getKey(),
                            h != null ? h.getHallType() : "—",
                            bookingsByHall.getOrDefault(e.getKey(), 0L),
                            e.getValue(),
                            e.getValue() * 100.0 / finalTotal));
                });
        hallsTable.setItems(FXCollections.observableArrayList(list));
    }

    private void setupTables() {
        // Weekly
        wColWeek   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().weekLabel));
        wColRange  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().dateRange));
        wColCount  .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().count)));
        wColRevenue.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().revenue)));
        // Monthly — with SST columns
        mColMonth.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().monthName));
        mColCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().count)));
        mColRevenue.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().revenue)));
        mColGross.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().revenue)));
        mColSst.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().revenue * SST_RATE)));
        mColNet.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("RM %.2f", c.getValue().revenue * (1 - SST_RATE))));
        // Details
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
        // Hall ranking
        if (hallsTable != null) {
            hColRank    .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().rank)));
            hColHall    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().hallName));
            hColType    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().hallType));
            hColBookings.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().bookings)));
            hColRevenue .setCellValueFactory(c -> new SimpleStringProperty(
                    String.format("RM %.2f", c.getValue().revenue)));
            hColShare   .setCellValueFactory(c -> new SimpleStringProperty(
                    String.format("%.1f%%", c.getValue().sharePercent)));
        }
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

    // ── Export — Admin version with SST ──────────────────────────────
    @FXML
    public void handleExport() {
        String periodLabel = selectedMonth > 0
                ? Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        + " " + selectedYear
                : "Full Year " + selectedYear;

        double gross = bookingService.getTotalRevenue();
        double sst   = gross * SST_RATE;
        double net   = gross - sst;

        StringBuilder sb = new StringBuilder();
        sb.append("=======================================================\n");
        sb.append("     HALL SYMPHONY — ADMIN FINANCIAL REPORT\n");
        sb.append("         (CONFIDENTIAL — ADMINISTRATOR USE)\n");
        sb.append("=======================================================\n");
        sb.append("Period      : ").append(periodLabel).append("\n");
        sb.append("Generated   : ").append(DateTimeUtils.now()).append("\n");
        sb.append("Generated by: ").append(SessionManager.getInstance().getDisplayName())
          .append(" (Administrator)\n");
        sb.append("-------------------------------------------------------\n\n");

        sb.append("REVENUE SUMMARY\n");
        sb.append(String.format("  This Week      : %s%n", statWeek.getText()));
        sb.append(String.format("  This Month     : %s%n", statMonth.getText()));
        sb.append(String.format("  This Year      : %s%n", statYear.getText()));
        sb.append(String.format("  Selected Period: %s%n", statPeriod.getText()));
        sb.append(String.format("  Cancel Rate    : %s%n", statCancelRate.getText()));
        sb.append(String.format("  Total Confirmed: %s bookings%n%n", statTotalBookings.getText()));

        sb.append("SST TAX BREAKDOWN (8% Service Tax — Malaysia 2024)\n");
        sb.append("-".repeat(50)).append("\n");
        sb.append(String.format("  Gross Revenue (All Time) : RM %.2f%n", gross));
        sb.append(String.format("  SST @ 8%%                 : RM %.2f%n", sst));
        sb.append(String.format("  Net Revenue (After SST)  : RM %.2f%n%n", net));

        sb.append("USER ACTIVITY\n");
        sb.append(String.format("  Total Users        : %s%n", statTotalUsers != null ? statTotalUsers.getText() : "—"));
        sb.append(String.format("  Active Customers   : %s%n", statActiveCustomers != null ? statActiveCustomers.getText() : "—"));
        sb.append(String.format("  New This Month     : %s%n%n", statNewThisMonth != null ? statNewThisMonth.getText() : "—"));

        sb.append("TOP PERFORMING HALLS\n");
        sb.append(String.format("  %-4s %-26s %-16s %8s %14s %8s%n",
                "Rank","Hall","Type","Bookings","Revenue","Share"));
        sb.append("  ").append("-".repeat(78)).append("\n");
        List<HallRevenueSummary> hallItems = hallsTable != null
                ? new ArrayList<>(hallsTable.getItems())
                : new ArrayList<>();
        for (HallRevenueSummary h : hallItems)
            sb.append(String.format("  %-4d %-26s %-16s %8d %14s %7.1f%%%n",
                    h.rank, h.hallName, h.hallType, h.bookings,
                    String.format("RM %.2f", h.revenue), h.sharePercent));
        sb.append("\n");

        sb.append("MONTHLY BREAKDOWN WITH SST\n");
        sb.append(String.format("  %-24s %8s %14s %12s %12s%n",
                "Month","Bookings","Gross","SST (8%)","Net"));
        sb.append("  ").append("-".repeat(72)).append("\n");
        for (MonthlySummary ms : monthlyTable.getItems())
            sb.append(String.format("  %-24s %8d %14s %12s %12s%n",
                    ms.monthName, ms.count,
                    String.format("RM %.2f", ms.revenue),
                    String.format("RM %.2f", ms.revenue * SST_RATE),
                    String.format("RM %.2f", ms.revenue * (1 - SST_RATE))));
        sb.append("\n");

        sb.append("=======================================================\n");
        sb.append("              END OF CONFIDENTIAL REPORT\n");
        sb.append("=======================================================\n");

        String fileName = "AdminFinancialReport_" + periodLabel.replace(" ", "_")
                + "_" + LocalDate.now() + ".txt";
        try {
            java.nio.file.Path path = Paths.get(System.getProperty("user.dir"), fileName);
            Files.writeString(path, sb.toString());
            MainApp.showInfo("Report Exported", "Saved to:\n" + path.toAbsolutePath());
        } catch (Exception e) {
            MainApp.showError("Export Failed", e.getMessage());
        }
    }

    private boolean isType(Booking b, String type) {
        Hall h = hallService.getHallById(b.getHallId());
        return h != null && h.getHallType().equals(type);
    }

    // ── Navigation ────────────────────────────────────────────────────
    @FXML public void goToDashboard()       { MainApp.switchScene("/fxml/admin/dashboard.fxml",       980, 640); }
    @FXML public void goToSales()           { /* already here */ }
    @FXML public void goToStaffManagement() { MainApp.switchScene("/fxml/admin/staff_management.fxml",980, 640); }
    @FXML public void goToUserManagement()  { MainApp.switchScene("/fxml/admin/user_management.fxml", 980, 640); }
    @FXML public void goToBookings()        { MainApp.switchScene("/fxml/admin/bookings_overview.fxml",980, 640); }
}
