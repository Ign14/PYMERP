package com.datakomerz.pymes.customers.application;

import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.dto.CustomerResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

  public CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(
      customer.getId(),
      customer.getName(),
      customer.getRut(),
      customer.getAddress(),
      customer.getLat(),
      customer.getLng(),
      customer.getPhone(),
      customer.getEmail(),
      customer.getSegment(),
      customer.getContactPerson(),
      customer.getNotes(),
      customer.getActive(),
      toInstant(customer.getCreatedAt()),
      toInstant(customer.getUpdatedAt())
    );
  }

  private Instant toInstant(OffsetDateTime value) {
    return Optional.ofNullable(value).map(OffsetDateTime::toInstant).orElse(null);
  }
}
