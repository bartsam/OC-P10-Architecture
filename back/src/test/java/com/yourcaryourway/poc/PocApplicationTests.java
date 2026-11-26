package com.yourcaryourway.poc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PocApplicationTests {

	/** Le contexte Spring démarre, migrations Flyway comprises. */
	@Test
	void contextLoads() {
	}
}