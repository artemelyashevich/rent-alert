package com.rentalert.admin_ms;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.admin_ms.exception.ProxiedErrorDecoder;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.NotificationMsService;
import com.rentalert.internal.client.service.OpsMsService;
import com.rentalert.internal.client.service.ProcessingMsService;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "app.kafka.outbox.enabled=false",
    "app.auth.access-secret=test-access-secret-0123456789abcdef",
    "app.auth.refresh-secret=test-refresh-secret-0123456789abcdef",
    "app.internal.token=test-internal-token"
})
class AdminMsApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void startsWithoutDatabase() {
        assertThat(context.getBeanNamesForType(javax.sql.DataSource.class)).isEmpty();
        assertThat(context.getBean(IdentityMsService.class)).isNotNull();
        assertThat(context.getBean(OpsMsService.class)).isNotNull();
        assertThat(context.getBean(ProcessingMsService.class)).isNotNull();
        assertThat(context.getBean(NotificationMsService.class)).isNotNull();
        assertThat(context.getBean(ErrorDecoder.class)).isInstanceOf(ProxiedErrorDecoder.class);
    }
}
