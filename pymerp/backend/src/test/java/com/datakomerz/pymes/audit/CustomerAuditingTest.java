package com.datakomerz.pymes.audit;

import static org.junit.jupiter.api.Assertions.*;

import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CustomerAuditingTest {

  @Autowired
  private CustomerRepository customerRepository;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithMockUser(username = "customer@pymerp.cl")
  void shouldPopulateAuditFieldsOnCustomerInsert() {
    Customer customer = buildCustomer();

    Customer saved = customerRepository.saveAndFlush(customer);

    assertEquals("customer@pymerp.cl", saved.getCreatedBy());
    assertEquals("customer@pymerp.cl", saved.getUpdatedBy());
    assertNotNull(saved.getCreatedAt());
    assertNotNull(saved.getUpdatedAt());
  }

  @Test
  void shouldKeepCreatedByWhenUpdatingWithDifferentUser() {
    Customer created = persistCustomerWithUser("creator@pymerp.cl");

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("updater@pymerp.cl", "password"));

    created.setPhone("+56 123456789");
    Customer updated = customerRepository.saveAndFlush(created);

    assertEquals("creator@pymerp.cl", updated.getCreatedBy());
    assertEquals("updater@pymerp.cl", updated.getUpdatedBy());
    assertTrue(updated.getUpdatedAt().isAfter(updated.getCreatedAt()));
  }

  private Customer buildCustomer() {
    Customer customer = new Customer();
    customer.setCompanyId(UUID.randomUUID());
    customer.setName("Audit Customer");
    customer.setActive(true);
    return customer;
  }

  private Customer persistCustomerWithUser(String username) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(username, "password"));
    Customer saved = customerRepository.saveAndFlush(buildCustomer());
    SecurityContextHolder.clearContext();
    return saved;
  }
}
