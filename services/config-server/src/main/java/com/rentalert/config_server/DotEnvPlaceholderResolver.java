package com.rentalert.config_server;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.environment.PropertyValueDescriptor;
import org.springframework.cloud.config.server.encryption.EnvironmentEncryptor;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class DotEnvPlaceholderResolver implements EnvironmentEncryptor {

    private static final String PREFIX = "${";
    private static final String SUFFIX = "}";
    private static final Path DEFAULT_ENV_FILE = Path.of("docker", ".env");

    private final Map<String, String> values;

    @Autowired
    public DotEnvPlaceholderResolver(@Value("${app.env-file:}") String envFile) {
        this(envFile.isBlank() ? findDefaultEnvFile(Path.of("").toAbsolutePath()) : Path.of(envFile));
    }

    DotEnvPlaceholderResolver(Path envFile) {
        this.values = load(envFile);
    }

    private static int findPlaceholderEnd(String text, int start) {
        int depth = 0;
        for (int i = start + PREFIX.length(); i < text.length(); i++) {
            if (text.startsWith(PREFIX, i)) {
                depth++;
                i += PREFIX.length() - 1;
            } else if (text.startsWith(SUFFIX, i)) {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return -1;
    }

    // The server may be started from the repository root or from its module: look for docker/.env upwards
    static Path findDefaultEnvFile(Path workingDir) {
        for (Path dir = workingDir; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(DEFAULT_ENV_FILE);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return workingDir.resolve(DEFAULT_ENV_FILE);
    }

    private static Map<String, String> load(Path envFile) {
        if (!Files.isRegularFile(envFile)) {
            log.warn("Env file {} not found, config placeholders are left for the clients", envFile.toAbsolutePath());
            return Map.of();
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read env file " + envFile.toAbsolutePath(), e);
        }
        Map<String, String> result = new HashMap<>();
        properties.forEach((key, value) -> {
            String text = unquote(value.toString().strip());
            // An empty variable shouldn't hide the one set in the client's own environment
            if (!text.isEmpty()) {
                result.put(key.toString(), text);
            }
        });
        log.info("Loaded {} variables from env file {}", result.size(), envFile.toAbsolutePath());
        return result;
    }

    // docker compose strips the quotes around a value, so do the same
    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            if ((first == '"' || first == '\'') && value.charAt(value.length() - 1) == first) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    @Override
    public Environment decrypt(Environment environment) {
        if (values.isEmpty()) {
            return environment;
        }
        Environment result = new Environment(environment);
        for (PropertySource source : environment.getPropertySources()) {
            Map<Object, Object> map = new LinkedHashMap<>(source.getSource());
            map.replaceAll((key, value) -> resolveValue(value));
            result.add(new PropertySource(source.getName(), map));
        }
        return result;
    }

    private Object resolveValue(Object value) {
        if (value instanceof PropertyValueDescriptor descriptor) {
            return new PropertyValueDescriptor(resolveValue(descriptor.getValue()), descriptor.getOrigin());
        }
        return value instanceof String text ? resolve(text) : value;
    }

    String resolve(String text) {
        int start = text.indexOf(PREFIX);
        if (start < 0) {
            return text;
        }
        int end = findPlaceholderEnd(text, start);
        if (end < 0) {
            return text;
        }
        String placeholder = text.substring(start + PREFIX.length(), end);
        int separator = placeholder.indexOf(':');
        String name = separator < 0 ? placeholder : placeholder.substring(0, separator);
        String replacement = values.get(name);
        if (replacement == null) {
            // Keep the placeholder for the client, but fill the variables inside its default
            replacement = separator < 0
                ? PREFIX + placeholder + SUFFIX
                : PREFIX + name + ":" + resolve(placeholder.substring(separator + 1)) + SUFFIX;
        }
        return text.substring(0, start) + replacement + resolve(text.substring(end + SUFFIX.length()));
    }
}
