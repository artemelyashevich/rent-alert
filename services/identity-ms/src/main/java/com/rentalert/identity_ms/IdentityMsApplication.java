package com.rentalert.identity_ms;

import org.springframework.boot.SpringApplication;
import com.rentalert.identity_ms.config.ApplicationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(ApplicationProperties.class)
@SpringBootApplication
public class IdentityMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityMsApplication.class, args);
	}

}
