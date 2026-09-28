package com.rentalert.notification_ms;

import com.rentalert.notification_ms.config.HttpClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({HttpClientProperties.class})
@SpringBootApplication
public class NotificationMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationMsApplication.class, args);
	}

}
