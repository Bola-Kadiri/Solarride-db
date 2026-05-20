package com.solarride.solarride.domain.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SolarSize {
    STARTER("1–2 kW", "Basic home"),
    STANDARD("3–5 kW", "Family home"),
    PREMIUM("6–10 kW", "Large home"),
    COMMERCIAL("11 kW+", "Business / estate");

    private final String powerRange;
    private final String description;
}