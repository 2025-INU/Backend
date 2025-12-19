package dev.promise4.GgUd;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires Redis cache configuration - to be fixed when implementing cache layer")
class GgUdApplicationTests {

	@Test
	void contextLoads() {
	}

}
