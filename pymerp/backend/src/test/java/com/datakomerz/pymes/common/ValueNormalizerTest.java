package com.datakomerz.pymes.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ValueNormalizerTest {

  private final ValueNormalizer normalizer = new ValueNormalizer();

  @Test
  void normalizeTrimsAndNullifiesEmpty() {
    assertEquals("value", normalizer.normalize("  value "));
    assertNull(normalizer.normalize("   "));
    assertNull(normalizer.normalize(null));
  }

  @Test
  void normalizeSearchIgnoresOnlyWhitespace() {
    assertEquals("query", normalizer.normalizeSearch(" query "));
    assertNull(normalizer.normalizeSearch("   "));
  }

  @Test
  void normalizeSegmentStripsUnassignedCodes() {
    String code = "__UNASSIGNED__";
    assertNull(normalizer.normalizeSegment(code, code));
    assertNull(normalizer.normalizeSegment("__UNASSIGNED__", code));
    assertEquals("Normal", normalizer.normalizeSegment(" normal ", code));
  }
}
