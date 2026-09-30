package com.rentalert.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ExceptionHandlingAutoConfigurationTests {

    @Test
    void isRegisteredAsAutoConfiguration() {
        var candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();

        assertThat(candidates).contains(ExceptionHandlingAutoConfiguration.class.getName());
    }

    @Test
    void registersControllerAdviceInServletApps() {
        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExceptionHandlingAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(GlobalExceptionController.class));
    }

    @Test
    void skipsReactiveApps() {
        new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExceptionHandlingAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionController.class));
    }

    @Test
    void mapsExceptionsToStatusAndLocalizedBody() throws Exception {
        var messages = new StaticMessageSource();
        messages.addMessage("ERROR_NO_SUCH_ENTITY", Locale.forLanguageTag("ru"), "Не найдено");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionController(messages))
            .build();

        mvc.perform(get("/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Не найдено"))
            .andExpect(jsonPath("$.details.errorCode").value("404"))
            .andExpect(jsonPath("$.details.errorId").exists());
        mvc.perform(get("/denied")).andExpect(status().isForbidden());
        mvc.perform(get("/conflict")).andExpect(status().isConflict());
        mvc.perform(get("/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("ERROR_TECHNICAL_ERROR"));
    }

    @RestController
    static class FailingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new AppNotFoundException();
        }

        @GetMapping("/denied")
        void denied() {
            throw new AccessDeniedException("no");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new DataIntegrityViolationException("dup");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }
    }
}
