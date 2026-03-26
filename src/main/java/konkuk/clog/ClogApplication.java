package konkuk.clog;

import konkuk.clog.domain.user.github.GithubOAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableConfigurationProperties(GithubOAuthProperties.class)
@EnableMongoRepositories(basePackages = "konkuk.clog.domain.chat.repository")
public class ClogApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClogApplication.class, args);
	}

}
