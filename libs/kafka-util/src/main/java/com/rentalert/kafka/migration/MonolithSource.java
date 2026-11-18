package com.rentalert.kafka.migration;

import org.springframework.jdbc.core.JdbcTemplate;

public record MonolithSource(JdbcTemplate jdbcTemplate) {

}
