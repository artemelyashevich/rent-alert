package com.rentalert.config_server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.environment.PropertyValueDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvPlaceholderResolverTest {

    @TempDir
    Path dir;

    private DotEnvPlaceholderResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        Path envFile = dir.resolve(".env");
        Files.writeString(envFile, """
            # comment
            HMAC_KEY=abc+/def==
            QUOTED_URL="https://example.com/api"
            PROCESSING_DB_USER=processing
            EMPTY_VALUE=
            """);
        resolver = new DotEnvPlaceholderResolver(envFile);
    }

    @Test
    void resolvesVariableFromEnvFile() {
        assertThat(resolver.resolve("${HMAC_KEY:}")).isEqualTo("abc+/def==");
        assertThat(resolver.resolve("${HMAC_KEY}")).isEqualTo("abc+/def==");
    }

    @Test
    void stripsQuotesLikeDockerCompose() {
        assertThat(resolver.resolve("${QUOTED_URL:}")).isEqualTo("https://example.com/api");
    }

    @Test
    void keepsUnknownPlaceholderForClient() {
        assertThat(resolver.resolve("${DB_URL:jdbc:postgresql://localhost:5440/processing}"))
            .isEqualTo("${DB_URL:jdbc:postgresql://localhost:5440/processing}");
        assertThat(resolver.resolve("${UNKNOWN}")).isEqualTo("${UNKNOWN}");
    }

    @Test
    void keepsEmptyVariableForClient() {
        assertThat(resolver.resolve("${EMPTY_VALUE:}")).isEqualTo("${EMPTY_VALUE:}");
    }

    @Test
    void resolvesInsideDefaultOfUnknownPlaceholder() {
        assertThat(resolver.resolve("${DB_USERNAME:${PROCESSING_DB_USER:processing_ms}}"))
            .isEqualTo("${DB_USERNAME:processing}");
    }

    @Test
    void resolvesSeveralPlaceholdersInOneValue() {
        assertThat(resolver.resolve("${QUOTED_URL}/users?user=${PROCESSING_DB_USER}&x=${UNKNOWN:1}"))
            .isEqualTo("https://example.com/api/users?user=processing&x=${UNKNOWN:1}");
    }

    @Test
    void resolvesServedEnvironmentKeepingOtherValues() {
        Environment environment = new Environment("identity-ms", "default");
        environment.add(new PropertySource("identity-ms.yml", Map.of(
            "app.crypto.hmac-key", "${HMAC_KEY:}",
            "server.port", 8081,
            "app.described", new PropertyValueDescriptor("${PROCESSING_DB_USER}", "origin"))));

        Environment result = resolver.decrypt(environment);

        Map<?, ?> source = result.getPropertySources().getFirst().getSource();
        assertThat(source.get("app.crypto.hmac-key")).isEqualTo("abc+/def==");
        assertThat(source.get("server.port")).isEqualTo(8081);
        PropertyValueDescriptor described = (PropertyValueDescriptor) source.get("app.described");
        assertThat(described.getValue()).isEqualTo("processing");
        assertThat(described.getOrigin()).isEqualTo("origin");
    }

    @Test
    void findsDefaultEnvFileFromRepositoryRootAndFromModule() throws IOException {
        Path envFile = Files.createDirectories(dir.resolve("docker")).resolve(".env");
        Files.writeString(envFile, "HMAC_KEY=abc");
        Path module = Files.createDirectories(dir.resolve("services").resolve("config-server"));

        assertThat(DotEnvPlaceholderResolver.findDefaultEnvFile(dir)).isEqualTo(envFile);
        assertThat(DotEnvPlaceholderResolver.findDefaultEnvFile(module)).isEqualTo(envFile);
    }

    @Test
    void leavesEnvironmentUntouchedWithoutEnvFile() {
        DotEnvPlaceholderResolver noFile = new DotEnvPlaceholderResolver(dir.resolve("missing.env"));
        Environment environment = new Environment("identity-ms", "default");

        assertThat(noFile.decrypt(environment)).isSameAs(environment);
        assertThat(noFile.resolve("${HMAC_KEY:}")).isEqualTo("${HMAC_KEY:}");
    }
}
