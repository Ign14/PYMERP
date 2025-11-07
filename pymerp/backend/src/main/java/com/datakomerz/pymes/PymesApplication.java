package com.datakomerz.pymes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.company.billing.persistence.CryptoProperties;
import com.company.billing.persistence.CryptoConfiguration;
import com.company.billing.persistence.DefaultCryptoService;
import com.datakomerz.pymes.billing.config.BillingOfflineProperties;
import com.datakomerz.pymes.billing.config.BillingWebhookProperties;
import com.datakomerz.pymes.config.AppProperties;

@SpringBootApplication
@EntityScan(basePackages = {"com.datakomerz.pymes", "com.company.billing.persistence"})
@EnableJpaRepositories(
    basePackages = {"com.datakomerz.pymes", "com.company.billing.persistence"},
    repositoryBaseClass = com.datakomerz.pymes.multitenancy.TenantAwareJpaRepository.class
)
@EnableConfigurationProperties({
    AppProperties.class,
    CryptoProperties.class,
    BillingWebhookProperties.class,
    BillingOfflineProperties.class
})
@Import({CryptoConfiguration.class, DefaultCryptoService.class})
@EnableScheduling
@EnableAsync
public class PymesApplication {
  public static void main(String[] args) {
    SpringApplication.run(PymesApplication.class, args);
  }
}
