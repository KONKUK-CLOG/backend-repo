package konkuk.clog;

import konkuk.clog.domain.user.github.GithubOAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GithubOAuthProperties.class)
public class ClogApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClogApplication.class, args);
	}

}
