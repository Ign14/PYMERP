package com.datakomerz.pymes.sales;

import com.datakomerz.pymes.sales.dto.SaleDetailLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ThermalTicketFormatter {

  private static final int WIDTH = 32;
  private static final DateTimeFormatter DATE_FORMAT =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CL"));

  private ThermalTicketFormatter() {
  }

  static String build(String companyName, Sale sale, String customerName, List<SaleDetailLine> lines) {
    StringBuilder sb = new StringBuilder();
    appendCentered(sb, safe(companyName, "PyMEs Suite"));
    appendCentered(sb, safe(sale.getDocType(), SaleDocumentType.FACTURA.label()));
    sb.append('\n');
    appendKeyValue(sb, "Fecha", formatDate(sale.getIssuedAt()));
    if (customerName != null && !customerName.isBlank()) {
      appendKeyValue(sb, "Cliente", customerName);
    }
    sb.append(repeat('-', WIDTH)).append('\n');

    if (lines != null && !lines.isEmpty()) {
      for (SaleDetailLine line : lines) {
        appendItem(sb, line);
      }
    } else {
      appendCentered(sb, "(Sin items)");
    }

    sb.append(repeat('-', WIDTH)).append('\n');
    appendAmount(sb, "Neto", sale.getNet());
    appendAmount(sb, "IVA", sale.getVat());
    appendAmount(sb, "Total", sale.getTotal());
    sb.append(repeat('-', WIDTH)).append('\n');
    appendKeyValue(sb, "Pago", safe(sale.getPaymentMethod(), SalePaymentMethod.TRANSFERENCIA.label()));
    sb.append('\n');
    appendCentered(sb, "Gracias por su compra");
    return sb.toString().strip();
  }

  private static void appendItem(StringBuilder sb, SaleDetailLine line) {
    List<String> nameLines = wrapText(line.productName(), WIDTH);
    if (nameLines.isEmpty()) {
      nameLines.add("Producto");
    }
    for (String nameLine : nameLines) {
      sb.append(nameLine);
      sb.append('\n');
    }

    String qtyPart = formatQuantity(line.qty()) + " x " + formatAmount(line.unitPrice());
    String totalPart = formatAmount(line.lineTotal());
    appendAligned(sb, qtyPart, totalPart);

    if (line.discount() != null && line.discount().compareTo(BigDecimal.ZERO) > 0) {
      String discountValue = "-" + formatAmount(line.discount());
      appendAligned(sb, "  Descuento", discountValue);
    }
  }

  private static void appendCentered(StringBuilder sb, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    String text = value.trim();
    if (text.length() > WIDTH) {
      List<String> lines = wrapText(text, WIDTH);
      for (String line : lines) {
        appendCentered(sb, line);
      }
      return;
    }
    int padding = Math.max(0, (WIDTH - text.length()) / 2);
    sb.append(repeat(' ', padding)).append(text).append('\n');
  }

  private static void appendKeyValue(StringBuilder sb, String key, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    String prefix = key + ": ";
    List<String> lines = wrapText(value.trim(), WIDTH - prefix.length());
    if (lines.isEmpty()) {
      sb.append(prefix).append('\n');
      return;
    }
    sb.append(prefix).append(lines.get(0)).append('\n');
    for (int i = 1; i < lines.size(); i++) {
      sb.append(repeat(' ', prefix.length())).append(lines.get(i)).append('\n');
    }
  }

  private static void appendAmount(StringBuilder sb, String label, BigDecimal amount) {
    appendAligned(sb, label + ":", formatAmount(amount));
  }

  private static void appendAligned(StringBuilder sb, String left, String right) {
    String leftClean = left == null ? "" : left;
    String rightClean = right == null ? "" : right;
    int free = WIDTH - leftClean.length() - rightClean.length();
    if (free < 1) {
      free = 1;
    }
    sb.append(leftClean).append(repeat(' ', free)).append(rightClean).append('\n');
  }

  private static String formatQuantity(BigDecimal qty) {
    if (qty == null) {
      return "0";
    }
    BigDecimal normalized = qty.stripTrailingZeros();
    return normalized.toPlainString();
  }

  private static String formatAmount(BigDecimal value) {
    BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
    return safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static String formatDate(OffsetDateTime dateTime) {
    OffsetDateTime effective = dateTime == null ? OffsetDateTime.now() : dateTime;
    return DATE_FORMAT.format(effective);
  }

  private static String safe(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

  private static String repeat(char value, int times) {
    if (times <= 0) {
      return "";
    }
    return String.valueOf(value).repeat(times);
  }

  private static List<String> wrapText(String text, int width) {
    List<String> lines = new ArrayList<>();
    if (text == null) {
      return lines;
    }
    String remaining = text.trim();
    while (!remaining.isEmpty()) {
      if (remaining.length() <= width) {
        lines.add(remaining);
        break;
      }
      int breakIdx = Math.min(width, remaining.length());
      while (breakIdx > 0 && !Character.isWhitespace(remaining.charAt(breakIdx - 1))) {
        breakIdx--;
      }
      if (breakIdx == 0) {
        breakIdx = Math.min(width, remaining.length());
      }
      String line = remaining.substring(0, breakIdx).trim();
      if (!line.isEmpty()) {
        lines.add(line);
      }
      remaining = remaining.substring(Math.min(breakIdx, remaining.length())).trim();
    }
    return lines;
  }
}
