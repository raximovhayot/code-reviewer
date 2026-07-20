package uz.uzinfoweb.code_reviewer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest
class CodeReviewerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void verifyModulithStructure() {
		ApplicationModules.of(CodeReviewerApplication.class).verify();
	}

}
