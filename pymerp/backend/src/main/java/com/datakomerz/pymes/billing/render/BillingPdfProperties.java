package com.datakomerz.pymes.billing.render;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "billing.pdf")
public class BillingPdfProperties {

  private String documentsBaseUrl = "http://localhost:8080/api/v1";
  private Branding branding = new Branding();

  public String getDocumentsBaseUrl() {
    return documentsBaseUrl;
  }

  public void setDocumentsBaseUrl(String documentsBaseUrl) {
    this.documentsBaseUrl = documentsBaseUrl;
  }

  public Branding getBranding() {
    return branding;
  }

  public void setBranding(Branding branding) {
    this.branding = branding;
  }

  public static class Branding {

    private String logoPath;
    private String primaryColor = "#1f2937";
    private String accentColor = "#2563eb";
    private String textColor = "#111827";
    private String tableHeaderColor = "#e5e7eb";

    public String getLogoPath() {
      return logoPath;
    }

    public void setLogoPath(String logoPath) {
      this.logoPath = logoPath;
    }

    public String getPrimaryColor() {
      return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
      this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
      return accentColor;
    }

    public void setAccentColor(String accentColor) {
      this.accentColor = accentColor;
    }

    public String getTextColor() {
      return textColor;
    }

    public void setTextColor(String textColor) {
      this.textColor = textColor;
    }

    public String getTableHeaderColor() {
      return tableHeaderColor;
    }

    public void setTableHeaderColor(String tableHeaderColor) {
      this.tableHeaderColor = tableHeaderColor;
    }
  }
}
