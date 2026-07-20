package uz.uzinfoweb.code_reviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CodeReviewerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeReviewerApplication.class, args);
	}

}
