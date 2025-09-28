package com.datakomerz.pymes.sales.reports;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesTimeseriesPoint(LocalDate date, BigDecimal total) {
}
