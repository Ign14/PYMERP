package com.datakomerz.pymes.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(int page, int size, long total, List<T> data) {

  public static <T> PagedResponse<T> from(Page<T> page) {
    return new PagedResponse<>(page.getNumber(), page.getSize(), page.getTotalElements(), page.getContent());
  }
}
