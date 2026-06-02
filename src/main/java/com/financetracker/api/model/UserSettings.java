package com.financetracker.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    private String mainCurrency = "UAH";
    private String secondaryCurrency = "USD";
    private String language = "UA";

    private String theme = "LIGHT";

    private String roundingFormat = "TWO_DECIMALS";
    private String firstDayOfWeek = "MONDAY";

    private boolean remindDataEntry = true;
    private boolean warnLimitExceeded = true;
    private boolean remindUpdates = true;
}
