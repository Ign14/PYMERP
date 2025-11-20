package com.datakomerz.pymes.billing.render;

import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.NonFiscalDocument;
import com.datakomerz.pymes.billing.dto.PurchaseOrderPayload;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleItem;
import com.datakomerz.pymes.sales.SaleItemRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StreamUtils;

@Component
@Transactional
public class PdfLocalInvoiceRenderer implements LocalInvoiceRenderer {

  private static final Locale LOCALE = Locale.forLanguageTag("es-CL");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withLocale(LOCALE);
  private static final DateTimeFormatter PDF_DOC_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withLocale(LOCALE);
  private static final DateTimeFormatter DATE_ONLY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(LOCALE);

  private final SaleItemRepository saleItemRepository;
  private final ProductRepository productRepository;
  private final CustomerRepository customerRepository;
  private final CompanyRepository companyRepository;
  private final BillingPdfProperties pdfProperties;
  private final ResourceLoader resourceLoader;
  private final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}");
  private final String offlineLegend;
  private final String contingencyTemplate;
  private final String nonFiscalTemplate;
  private final String quotationTemplate;
  private final String deliveryTemplate;
  private final String creditNoteTemplate;
  private final String purchaseOrderTemplate;
  private final String receptionTemplate;
  private final Clock clock;
  private final AtomicBoolean logoLoaded = new AtomicBoolean(false);
  private volatile String cachedLogoDataUrl;

  public PdfLocalInvoiceRenderer(SaleItemRepository saleItemRepository,
                                 ProductRepository productRepository,
                                 CustomerRepository customerRepository,
                                 CompanyRepository companyRepository,
                                 BillingPdfProperties pdfProperties,
                                 ResourceLoader resourceLoader,
                                 ObjectProvider<Clock> clockProvider,
                                 @Value("${billing.offline.legend:Documento emitido en contingencia. Sera validado al sincronizarse con el SII.}") String offlineLegend) {
    this.saleItemRepository = saleItemRepository;
    this.productRepository = productRepository;
    this.customerRepository = customerRepository;
    this.companyRepository = companyRepository;
    this.pdfProperties = pdfProperties;
    this.resourceLoader = resourceLoader;
    this.offlineLegend = offlineLegend;
    Clock providedClock = clockProvider.getIfAvailable();
    this.clock = providedClock != null ? providedClock : Clock.systemDefaultZone();
    this.contingencyTemplate = loadTemplate("classpath:templates/billing/fiscal-contingency.html");
    this.nonFiscalTemplate = loadTemplate("classpath:templates/billing/non-fiscal.html");
    this.quotationTemplate = loadTemplate("classpath:templates/billing/quotation.html");
    this.deliveryTemplate = loadTemplate("classpath:templates/billing/delivery-note.html");
    this.creditNoteTemplate = loadTemplate("classpath:templates/billing/credit-note.html");
    this.purchaseOrderTemplate = loadTemplate("classpath:templates/billing/purchase-order.html");
    this.receptionTemplate = loadTemplate("classpath:templates/billing/reception-guide.html");
  }

  @Override
  public RenderedInvoice renderContingencyFiscalPdf(FiscalDocument document, Sale sale) {
    Objects.requireNonNull(document, "document is required");
    Objects.requireNonNull(sale, "sale is required");
    SaleContext context = loadSaleContext(sale);
    String qrUrl = buildDocumentUrl(document.getId());
    Map<String, String> variables = buildCommonContext(
        sale,
        document.getCreatedAt(),
        context.company(),
        context.customer(),
        context.items(),
        qrUrl);
    variables.put("documentTitle", "Contingencia " + document.getDocumentType().name());
    variables.put("documentNumberLabel", "Numero provisional");
    variables.put("documentNumber", safeText(document.getProvisionalNumber()));
    variables.put("secondaryNumberLabel", "Folio definitivo");
    variables.put("secondaryNumber", safeText(Optional.ofNullable(document.getFinalFolio()).orElse("PENDIENTE")));
    variables.put("offlineLegend", htmlEscape(offlineLegend));
    variables.put("taxMode", document.getTaxMode() != null ? htmlEscape(document.getTaxMode().name()) : "");
    variables.put("siiCode", document.getSiiDocumentType() != null ? Integer.toString(document.getSiiDocumentType().code()) : "");
    variables.put("documentFolio", safeText(Optional.ofNullable(document.getFinalFolio()).orElse("-")));
    variables.put("documentProvisional", safeText(document.getProvisionalNumber()));
    variables.put("resolutionNumber", safeText(document.getResolutionNumber()));
    variables.put("resolutionDate", formatDate(document.getResolutionDate()));
    variables.put("totalsExempt", formatMoney(calculateExemptTotal(sale)));
    variables.put("taxModeLabel", document.getTaxMode() != null ? htmlEscape(document.getTaxMode().name()) : "");

    byte[] pdfBytes = renderPdf(placeholderHelper.replacePlaceholders(contingencyTemplate, variables::get));
    String filename = "contingencia-" + sanitizeFilename(document.getProvisionalNumber(), document.getId()) + ".pdf";
    return new RenderedInvoice(pdfBytes, filename, "application/pdf");
  }

  @Override
  public RenderedInvoice renderNonFiscalPdf(NonFiscalDocument document, Sale sale) {
    Objects.requireNonNull(document, "document is required");
    Objects.requireNonNull(sale, "sale is required");
    return switch (document.getDocumentType()) {
      case COTIZACION -> renderQuotationPdf(document, sale);
      case COMPROBANTE_ENTREGA -> renderDeliveryNotePdf(document, sale);
    };
  }

  @Override
  public RenderedInvoice renderQuotationPdf(NonFiscalDocument document, Sale sale) {
    Objects.requireNonNull(document, "document is required");
    Objects.requireNonNull(sale, "sale is required");
    SaleContext context = loadSaleContext(sale);
    String qrUrl = buildDocumentUrl(document.getId());
    Map<String, String> variables = buildCommonContext(
        sale,
        document.getCreatedAt(),
        context.company(),
        context.customer(),
        context.items(),
        qrUrl);
    OffsetDateTime issuedAt = sale.getIssuedAt() != null ? sale.getIssuedAt() : document.getCreatedAt();
    LocalDate issuedDate = issuedAt != null ? issuedAt.toLocalDate() : LocalDate.now(clock);
    LocalDate validUntil = issuedDate.plusDays(30);
    variables.put("documentTitle", "COTIZACIÓN");
    variables.put("documentNumberLabel", "Cotización N°");
    variables.put("documentNumber", safeText(document.getNumber()));
    variables.put("secondaryNumberLabel", "");
    variables.put("secondaryNumber", "");
    variables.put("offlineLegend", htmlEscape("SIN VALOR TRIBUTARIO"));
    variables.put("taxMode", "");
    variables.put("quotationDate", formatDate(issuedDate));
    variables.put("quotationValidUntil", formatDate(validUntil));
    variables.put("watermarkText", htmlEscape("SIN VALOR TRIBUTARIO"));
    variables.put("notes", htmlEscape("Cotización sin valor tributario"));
    variables.put("footerText", htmlEscape("Válida por 30 días desde la fecha de emisión"));
    byte[] pdfBytes = renderPdf(placeholderHelper.replacePlaceholders(quotationTemplate, variables::get));
    String filename = "cotizacion-" + sanitizeFilename(document.getNumber(), document.getId()) + ".pdf";
    return new RenderedInvoice(pdfBytes, filename, "application/pdf");
  }

  @Override
  public RenderedInvoice renderDeliveryNotePdf(NonFiscalDocument document, Sale sale) {
    Objects.requireNonNull(document, "document is required");
    Objects.requireNonNull(sale, "sale is required");
    SaleContext context = loadSaleContext(sale);
    String qrUrl = buildDocumentUrl(document.getId());
    Map<String, String> variables = buildCommonContext(
        sale,
        document.getCreatedAt(),
        context.company(),
        context.customer(),
        context.items(),
        qrUrl);
    variables.put("documentTitle", "NOTA DE ENTREGA");
    variables.put("documentNumberLabel", "Nota N°");
    variables.put("documentNumber", safeText(document.getNumber()));
    variables.put("secondaryNumberLabel", "");
    variables.put("secondaryNumber", "");
    variables.put("offlineLegend", "");
    variables.put("taxMode", "");
    variables.put("deliveryItemsRows", buildDeliveryItemsRows(context.items()));
    variables.put("deliveryAddress", variables.get("customerAddress"));
    variables.put("driverName", "");
    variables.put("vehiclePlate", "");
    variables.put("deliveryNotes", htmlEscape("Documento para control de despacho"));
    byte[] pdfBytes = renderPdf(placeholderHelper.replacePlaceholders(deliveryTemplate, variables::get));
    String filename = "nota-entrega-" + sanitizeFilename(document.getNumber(), document.getId()) + ".pdf";
    return new RenderedInvoice(pdfBytes, filename, "application/pdf");
  }

  @Override
  public RenderedInvoice renderCreditNotePdf(FiscalDocument document, Sale sale) {
    Objects.requireNonNull(document, "document is required");
    Objects.requireNonNull(sale, "sale is required");
    SaleContext context = loadSaleContext(sale);
    String qrUrl = buildDocumentUrl(document.getId());
    Map<String, String> variables = buildCommonContext(
        sale,
        document.getCreatedAt(),
        context.company(),
        context.customer(),
        context.items(),
        qrUrl);
    String folio = Optional.ofNullable(document.getFinalFolio())
        .orElse(Optional.ofNullable(document.getNumber()).orElse("-"));
    variables.put("documentTitle", "NOTA DE CRÉDITO");
    variables.put("documentNumberLabel", "Folio");
    variables.put("documentNumber", safeText(folio));
    variables.put("secondaryNumberLabel", "Referencia");
    variables.put("secondaryNumber", sale.getId() != null ? htmlEscape(sale.getId().toString()) : "");
    variables.put("offlineLegend", "");
    variables.put("taxMode", document.getTaxMode() != null ? htmlEscape(document.getTaxMode().name()) : "");
    variables.put("creditLegend", htmlEscape("Documento emitido para anular o corregir una operación previa."));
    byte[] pdfBytes = renderPdf(placeholderHelper.replacePlaceholders(creditNoteTemplate, variables::get));
    String filename = "nota-credito-" + sanitizeFilename(folio, document.getId()) + ".pdf";
    return new RenderedInvoice(pdfBytes, filename, "application/pdf");
  }

  @Override
  public RenderedInvoice renderPurchaseOrderPdf(PurchaseOrderPayload payload) {
    Objects.requireNonNull(payload, "payload is required");
    PurchaseOrderPayload.CompanyInfo buyer = Objects.requireNonNull(payload.buyer(), "buyer is required");
    PurchaseOrderPayload.SupplierInfo supplier = Objects.requireNonNull(payload.supplier(), "supplier is required");
    Map<String, String> values = new LinkedHashMap<>();
    values.put("logoHtml", buildLogoHtml(buyer.name()));
    values.put("orderNumber", safeText(payload.orderNumber()));
    values.put("orderDate", formatDate(payload.orderDate()));
    values.put("expectedDelivery", formatDate(payload.expectedDeliveryDate()));
    values.put("buyerName", htmlEscape(buyer.name()));
    values.put("buyerTaxId", safeText(buyer.taxId()));
    values.put("buyerAddress", htmlEscape(buyer.address()));
    values.put("buyerPhone", htmlEscape(buyer.phone()));
    values.put("buyerEmail", htmlEscape(buyer.email()));
    values.put("supplierName", htmlEscape(supplier.name()));
    values.put("supplierTaxId", safeText(supplier.taxId()));
    values.put("supplierAddress", htmlEscape(supplier.address()));
    values.put("supplierPhone", htmlEscape(supplier.phone()));
    values.put("supplierEmail", htmlEscape(supplier.email()));
    values.put("deliveryAddress", htmlEscape(payload.deliveryAddress()));
    values.put("paymentTerms", htmlEscape(Optional.ofNullable(payload.paymentTerms()).orElse("")));
    values.put("notes", htmlEscape(Optional.ofNullable(payload.notes()).orElse("")));
    values.put("approvedBy", htmlEscape(Optional.ofNullable(payload.approvedBy()).orElse("")));
    values.put("itemsRows", buildPurchaseItemsRows(payload.items(), true));
    values.put("subtotal", formatMoney(payload.subtotal()));
    values.put("tax", formatMoney(payload.tax()));
    values.put("total", formatMoney(payload.total()));
    byte[] pdfBytes = renderPdf(placeholderHelper.replacePlaceholders(purchaseOrderTemplate, values::get));
    String filename = "orden-compra-" + sanitizeFilename(payload.orderNumber(), null) + ".pdf";
    return new RenderedInvoice(pdfBytes, filename, "application/pdf");
  }

  @Override
  public RenderedInvoice renderReceptionGuidePdf(PurchaseOrderPayload payload) {
    Objects.requireNonNull(payload, "payload is required");
    PurchaseOrderPayload.CompanyInfo buyer = Objects.requireNonNull(payload.buyer(), "buyer is required");
    PurchaseOrderPayload.SupplierInfo supplier = Objects.requireNonNull(payload.supplier(), "supplier is required");
    Map<String, String> values = new LinkedHashMap<>();
    values.put("logoHtml", buildLogoHtml(buyer.name()));
    values.put("orderNumber", safeText(payload.orderNumber()));
    LocalDate receptionDate = payload.expectedDeliveryDate() != null
        ? payload.expectedDeliveryDate()
        : LocalDate.now(clock);
    values.put("receptionDate", formatDate(receptionDate));
    values.put("buyerName", htmlEscape(buyer.name()));
    values.put("deliveryAddress", htmlEscape(payload.deliveryAddress()));
    values.put("supplierName", htmlEscape(supplier.name()));
    values.put("supplierTaxId", safeText(supplier.taxId()));
    values.put("itemsRows", buildPurchaseItemsRows(payload.items(), false));
    values.put("receiverName", htmlEscape(Optional.ofNullable(payload.approvedBy()).orElse("")));
    values.put("notes", htmlEscape(Optional.ofNullable(payload.notes()).orElse("")));
    byte[] pdfBytes = renderPdf(placeholderHelper.replacePlaceholders(receptionTemplate, values::get));
    String filename = "guia-recepcion-" + sanitizeFilename(payload.orderNumber(), null) + ".pdf";
    return new RenderedInvoice(pdfBytes, filename, "application/pdf");
  }

  private Map<String, String> buildCommonContext(Sale sale,
                                                 OffsetDateTime documentCreatedAt,
                                                 Company company,
                                                 Customer customer,
                                                 List<LineItem> items,
                                                 String qrUrl) {
    Map<String, String> values = new LinkedHashMap<>();
    BillingPdfProperties.Branding branding = pdfProperties.getBranding();
    values.put("primaryColor", defaultColor(branding.getPrimaryColor(), "#1f2937"));
    values.put("accentColor", defaultColor(branding.getAccentColor(), "#2563eb"));
    values.put("textColor", defaultColor(branding.getTextColor(), "#111827"));
    values.put("tableHeaderColor", defaultColor(branding.getTableHeaderColor(), "#e5e7eb"));
    values.put("logoHtml", buildLogoHtml(company));

    values.put("companyName", htmlEscape(company.getBusinessName()));
    values.put("companyRut", safeText(company.getRut()));
    values.put("companyTaxId", values.get("companyRut"));
    values.put("companyActivity", htmlEscape(company.getBusinessActivity()));
    values.put("companyAddress", htmlEscape(company.getAddress()));
    values.put("companyCommune", htmlEscape(company.getCommune()));
    values.put("companyPhone", htmlEscape(company.getPhone()));
    values.put("companyEmail", htmlEscape(company.getEmail()));

    values.put("customerName", customer != null ? htmlEscape(customer.getName()) : "Consumidor Final");
    values.put("customerAddress", customer != null ? htmlEscape(customer.getAddress()) : "Sin dirección registrada");
    values.put("customerPhone", customer != null ? htmlEscape(customer.getPhone()) : "");
    values.put("customerEmail", customer != null ? htmlEscape(customer.getEmail()) : "");
    String customerRut = customer != null ? safeText(customer.getRut()) : "";
    values.put("customerRut", customerRut);
    values.put("customerTaxId", customerRut);

    values.put("issuedAt", formatDateTime(sale.getIssuedAt(), documentCreatedAt));
    values.put("paymentMethod", safeText(sale.getPaymentMethod()));
    values.put("documentType", safeText(sale.getDocType()));
    values.put("saleId", sale.getId() != null ? sale.getId().toString() : "");

    values.put("itemsRows", buildItemsRows(items));
    values.put("totalsSubtotal", formatMoney(sale.getNet()));
    values.put("totalsVat", formatMoney(sale.getVat()));
    values.put("totalsTotal", formatMoney(sale.getTotal()));
    values.put("totalsExempt", formatMoney(BigDecimal.ZERO));
    values.put("itemsCount", Integer.toString(items.size()));

    values.put("qrImage", buildQrImgTag(qrUrl));
    values.put("documentUrl", htmlEscape(qrUrl));
    values.put("generatedOn", DATE_FORMATTER.format(OffsetDateTime.now(clock)));
    values.put("footerNote", htmlEscape(Optional.ofNullable(company.getReceiptFooterMessage()).orElse("")));

    return values;
  }

  private String buildItemsRows(List<LineItem> items) {
    if (items.isEmpty()) {
      return """
          <tr class="items-empty">
            <td colspan="6">Sin ítems registrados</td>
          </tr>
          """;
    }
    return items.stream()
        .map(item -> """
            <tr>
              <td class="idx">%d</td>
              <td class="desc">%s</td>
              <td class="qty">%s</td>
              <td class="price">%s</td>
              <td class="discount">%s</td>
              <td class="total">%s</td>
            </tr>
            """.formatted(
            item.index(),
            item.description(),
            item.quantity(),
            item.unitPrice(),
            item.discount(),
            item.total()))
        .collect(Collectors.joining("\n"));
  }

  private String buildDeliveryItemsRows(List<LineItem> items) {
    if (items.isEmpty()) {
      return """
          <tr class="items-empty">
            <td colspan="3">Sin ítems para despachar</td>
          </tr>
          """;
    }
    return items.stream()
        .map(item -> """
            <tr>
              <td class="idx">%d</td>
              <td class="desc">%s</td>
              <td class="qty">%s</td>
            </tr>
            """.formatted(item.index(), item.description(), item.quantity()))
        .collect(Collectors.joining("\n"));
  }

  private String buildPurchaseItemsRows(List<PurchaseOrderPayload.PurchaseOrderItem> items,
                                        boolean includePrices) {
    List<PurchaseOrderPayload.PurchaseOrderItem> safeItems = items != null ? items : List.of();
    if (safeItems.isEmpty()) {
      int columns = includePrices ? 6 : 4;
      return """
          <tr class="items-empty">
            <td colspan="%d">Sin ítems registrados</td>
          </tr>
          """.formatted(columns);
    }
    return IntStream.range(0, safeItems.size())
        .mapToObj(i -> {
          PurchaseOrderPayload.PurchaseOrderItem item = safeItems.get(i);
          String code = htmlEscape(Optional.ofNullable(item.productCode()).orElse("-"));
          String description = htmlEscape(Optional.ofNullable(item.description()).orElse("Ítem"));
          String quantity = formatQuantity(item.quantity());
          String unit = htmlEscape(Optional.ofNullable(item.unit()).orElse("unid."));
          if (!includePrices) {
            return """
                <tr>
                  <td>%d</td>
                  <td>%s</td>
                  <td class="text-center">%s</td>
                  <td class="text-center">%s</td>
                </tr>
                """.formatted(i + 1, code + " - " + description, quantity, unit);
          }
          String unitPrice = formatMoney(item.unitPrice());
          String subtotal = formatMoney(item.subtotal());
          return """
              <tr>
                <td>%d</td>
                <td>%s</td>
                <td class="text-center">%s</td>
                <td class="text-center">%s</td>
                <td class="text-right">%s</td>
                <td class="text-right">%s</td>
              </tr>
              """.formatted(i + 1, code + " - " + description, quantity, unit, unitPrice, subtotal);
        })
        .collect(Collectors.joining("\n"));
  }

  private SaleContext loadSaleContext(Sale sale) {
    Objects.requireNonNull(sale, "sale is required");
    Company company = companyRepository.findById(sale.getCompanyId())
        .orElseThrow(() -> new LocalInvoiceRenderingException("Company not found for sale " + sale.getId()));
    Customer customer = Optional.ofNullable(sale.getCustomerId())
        .flatMap(customerRepository::findById)
        .orElse(null);
    List<SaleItem> saleItems = saleItemRepository.findBySaleId(sale.getId());
    List<LineItem> items = IntStream.range(0, saleItems.size())
        .mapToObj(i -> toLineItem(i + 1, saleItems.get(i)))
        .toList();
    return new SaleContext(company, customer, items);
  }

  private LineItem toLineItem(int index, SaleItem item) {
    Product product = Optional.ofNullable(item.getProductId())
        .flatMap(productRepository::findById)
        .orElse(null);
    StringBuilder description = new StringBuilder();
    if (product != null) {
      description.append(htmlEscape(product.getName()));
      if (product.getSku() != null && !product.getSku().isBlank()) {
        description.append(" <span class=\"sku\">SKU: ")
            .append(htmlEscape(product.getSku()))
            .append("</span>");
      }
    } else {
      description.append("Producto ")
          .append(item.getProductId() != null ? htmlEscape(item.getProductId().toString()) : "");
    }

    BigDecimal qty = defaultZero(item.getQty());
    BigDecimal unitPrice = defaultZero(item.getUnitPrice());
    BigDecimal discount = defaultZero(item.getDiscount());
    BigDecimal lineTotal = unitPrice.multiply(qty).subtract(discount);
    if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
      lineTotal = BigDecimal.ZERO;
    }
    return new LineItem(
        index,
        description.toString(),
        formatQuantity(qty),
        formatMoney(unitPrice),
        discount.compareTo(BigDecimal.ZERO) > 0 ? formatMoney(discount) : "-",
        formatMoney(lineTotal));
  }

  private String buildLogoHtml(Company company) {
    return buildLogoHtml(company != null ? company.getBusinessName() : null);
  }

  private String buildLogoHtml(String fallbackName) {
    String dataUrl = resolveLogoDataUrl();
    if (dataUrl != null) {
      return "<img src=\"" + dataUrl + "\" alt=\"Logo\"/>";
    }
    String initials = fallbackName != null && !fallbackName.isBlank()
        ? htmlEscape(fallbackName.substring(0, Math.min(3, fallbackName.length())).toUpperCase(Locale.ROOT))
        : "LOGO";
    return "<div class=\"logo-placeholder\">" + initials + "</div>";
  }

  private String resolveLogoDataUrl() {
    if (logoLoaded.get()) {
      return cachedLogoDataUrl;
    }
    synchronized (this) {
      if (logoLoaded.get()) {
        return cachedLogoDataUrl;
      }
      String location = pdfProperties.getBranding().getLogoPath();
      if (location == null || location.isBlank()) {
        logoLoaded.set(true);
        return null;
      }
      Resource resource = resourceLoader.getResource(location);
      if (!resource.exists()) {
        logoLoaded.set(true);
        return null;
      }
      try (InputStream inputStream = resource.getInputStream();
           ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        StreamUtils.copy(inputStream, outputStream);
        cachedLogoDataUrl = "data:" + detectMediaType(resource) + ";base64:"
            + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        logoLoaded.set(true);
        return cachedLogoDataUrl;
      } catch (IOException ex) {
        logoLoaded.set(true);
        throw new LocalInvoiceRenderingException("Failed to load logo resource from " + location, ex);
      }
    }
  }

  private String detectMediaType(Resource resource) {
    String filename = resource.getFilename();
    if (filename == null) {
      return "image/png";
    }
    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (lower.endsWith(".svg")) {
      return "image/svg+xml";
    }
    return "image/png";
  }

  private String loadTemplate(String location) {
    Resource resource = resourceLoader.getResource(location);
    if (!resource.exists()) {
      throw new LocalInvoiceRenderingException("Template not found: " + location);
    }
    try {
      return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new LocalInvoiceRenderingException("Unable to read template: " + location, ex);
    }
  }

  private String buildDocumentUrl(UUID documentId) {
    if (documentId == null) {
      throw new LocalInvoiceRenderingException("Document ID is required to build PDF QR link");
    }
    String base = Optional.ofNullable(pdfProperties.getDocumentsBaseUrl())
        .filter(url -> !url.isBlank())
        .orElse("http://localhost:8080/api/v1");
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base + "/billing/documents/" + documentId;
  }

  private String buildQrImgTag(String url) {
    try {
      BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 220, 220);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
      String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
      return "<img src=\"" + dataUrl + "\" alt=\"QR\"/>";
    } catch (WriterException | IOException ex) {
      throw new LocalInvoiceRenderingException("Unable to render QR code", ex);
    }
  }

  private byte[] renderPdf(String html) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.toStream(outputStream);
      builder.run();
      return normalizePdf(outputStream.toByteArray());
    } catch (Exception ex) {
      throw new LocalInvoiceRenderingException("Unable to render PDF", ex);
    }
  }

  private byte[] normalizePdf(byte[] pdfBytes) {
    Calendar fixedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    fixedDate.setTimeInMillis(clock.instant().toEpochMilli());
    try (PDDocument document = PDDocument.load(pdfBytes);
         ByteArrayOutputStream normalized = new ByteArrayOutputStream()) {
      PDDocumentInformation info = document.getDocumentInformation();
      info.setCreationDate(fixedDate);
      info.setModificationDate(fixedDate);
      info.setProducer("PYMERP Local Renderer");
      document.setDocumentInformation(info);
      document.getDocumentCatalog().setMetadata(null);
      document.getDocumentCatalog().setLanguage("es-CL");
      document.getDocument().getTrailer().removeItem(COSName.ID);
      document.save(normalized);
      byte[] normalizedBytes = normalized.toByteArray();
      return scrubPdfMetadata(normalizedBytes, fixedDate.getTimeInMillis());
    } catch (IOException ex) {
      throw new LocalInvoiceRenderingException("Unable to normalize PDF metadata", ex);
    }
  }

  private byte[] scrubPdfMetadata(byte[] pdfBytes, long timestampMillis) {
    OffsetDateTime fixed = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneOffset.UTC);
    String pdfTimestamp = "D:" + fixed.format(PDF_DOC_DATE_FORMAT) + "+00'00'";
    String xmpTimestamp = fixed.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    String producer = "PYMERP Local Renderer";
    String content = new String(pdfBytes, StandardCharsets.ISO_8859_1);
    content = content.replaceAll("(?s)/CreationDate \\(D:[^)]*\\)", "/CreationDate (" + pdfTimestamp + ")");
    content = content.replaceAll("(?s)/ModDate \\(D:[^)]*\\)", "/ModDate (" + pdfTimestamp + ")");
    content = content.replaceAll("(?s)/Producer \\([^)]*\\)", "/Producer (" + producer + ")");
    content = content.replaceAll("(?s)/ID \\[<[^>]+>\\s*<[^>]+>\\]",
        "/ID [<00000000000000000000000000000000><00000000000000000000000000000000>]");
    content = content.replaceAll("(?s)<xmp:CreateDate>[^<]+</xmp:CreateDate>",
        "<xmp:CreateDate>" + xmpTimestamp + "</xmp:CreateDate>");
    content = content.replaceAll("(?s)<xmp:ModifyDate>[^<]+</xmp:ModifyDate>",
        "<xmp:ModifyDate>" + xmpTimestamp + "</xmp:ModifyDate>");
    content = content.replaceAll("(?s)<pdf:Producer>[^<]+</pdf:Producer>",
        "<pdf:Producer>" + producer + "</pdf:Producer>");
    return content.getBytes(StandardCharsets.ISO_8859_1);
  }

  private String formatDate(LocalDate date) {
    return date != null ? DATE_ONLY_FORMATTER.format(date) : "";
  }

  private BigDecimal calculateExemptTotal(Sale sale) {
    BigDecimal total = defaultZero(sale.getTotal());
    BigDecimal net = defaultZero(sale.getNet());
    BigDecimal vat = defaultZero(sale.getVat());
    BigDecimal exempt = total.subtract(net).subtract(vat);
    if (exempt.compareTo(BigDecimal.ZERO) < 0) {
      return BigDecimal.ZERO;
    }
    return exempt;
  }

  private String formatDateTime(OffsetDateTime saleIssuedAt, OffsetDateTime fallback) {
    OffsetDateTime source = saleIssuedAt != null ? saleIssuedAt : fallback;
    if (source == null) {
      source = OffsetDateTime.now(clock);
    }
    return DATE_FORMATTER.format(source);
  }

  private String formatMoney(BigDecimal value) {
    NumberFormat format = NumberFormat.getCurrencyInstance(LOCALE);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(2);
    return format.format(value != null ? value : BigDecimal.ZERO);
  }

  private String formatQuantity(BigDecimal quantity) {
    if (quantity == null) {
      return "0";
    }
    BigDecimal normalized = quantity.stripTrailingZeros();
    return normalized.scale() < 0 ? normalized.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
        : normalized.toPlainString();
  }

  private BigDecimal defaultZero(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private String sanitizeFilename(String preferred, UUID fallback) {
    String base = preferred;
    if (base == null || base.isBlank()) {
      base = fallback != null ? fallback.toString() : "document";
    }
    return base.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private String safeText(String value) {
    return value != null ? htmlEscape(value) : "";
  }

  private String htmlEscape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private String defaultColor(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

  private record SaleContext(Company company, Customer customer, List<LineItem> items) { }

  private record LineItem(int index,
                          String description,
                          String quantity,
                          String unitPrice,
                          String discount,
                          String total) { }
}
