package com.datakomerz.pymes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import com.datakomerz.pymes.config.TestJwtDecoderConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class PymesApplicationTests {

  @Test
  void contextLoads() {
  }
}
