module com.hallbooking {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.hallbooking to javafx.fxml;
    opens com.hallbooking.controllers to javafx.fxml;
    opens com.hallbooking.controllers.customer  to javafx.fxml;
    opens com.hallbooking.controllers.scheduler to javafx.fxml;
    opens com.hallbooking.controllers.admin     to javafx.fxml;
    opens com.hallbooking.controllers.manager   to javafx.fxml;

    exports com.hallbooking;
    exports com.hallbooking.models;
    exports com.hallbooking.services;
    exports com.hallbooking.utils;
}
