package com.rentalert.processing_ms.migration;

import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.AbstractMonolithMigrationParticipant;
import com.rentalert.kafka.migration.ColumnRowMapper;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MigrationTable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ProcessingMigrationParticipant extends AbstractMonolithMigrationParticipant {

    private static final String CITY_INSERT_WITHOUT_ID = "INSERT INTO cities (provider, name, city_code, created_at, updated_at,"
        + " is_active) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
    private static final String SCHEDULER_SETTINGS_INSERT_WITHOUT_ID = "INSERT INTO scheduler_settings (provider_name,"
        + " created_at, updated_at, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to,"
        + " price_currency, region_id, sort_by, sort_order, category_id, room_category_id)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

    private static final List<String> CITY_COLUMNS = List.of("id", "provider", "name", "city_code", "created_at", "updated_at",
        "is_active");
    private static final List<String> SCHEDULER_SETTINGS_COLUMNS = List.of("id", "provider_name", "created_at", "updated_at",
        "scheduler_enabled", "notifications_enabled", "fixed_rate_ms", "page_size", "price_from", "price_to", "price_currency",
        "region_id", "sort_by", "sort_order", "category_id", "room_category_id");

    private static final MigrationTable CITIES = table("cities", CITY_COLUMNS).conflictTarget(List.of()).build();
    private static final MigrationTable CITIES_WITHOUT_ID = table("cities", CITY_COLUMNS)
        .customInsertSql(CITY_INSERT_WITHOUT_ID)
        .customRowMapper(withoutLeadingId(CITY_COLUMNS))
        .build();
    private static final MigrationTable SCHEDULER_SETTINGS = table("scheduler_settings", SCHEDULER_SETTINGS_COLUMNS)
        .conflictTarget(List.of())
        .build();
    private static final MigrationTable SCHEDULER_SETTINGS_WITHOUT_ID = table("scheduler_settings", SCHEDULER_SETTINGS_COLUMNS)
        .customInsertSql(SCHEDULER_SETTINGS_INSERT_WITHOUT_ID)
        .customRowMapper(withoutLeadingId(SCHEDULER_SETTINGS_COLUMNS))
        .build();
    private static final Map<String, MigrationTable> NATURAL_KEY_SECOND_PASSES = Map.of(
        "cities", CITIES_WITHOUT_ID,
        "scheduler_settings", SCHEDULER_SETTINGS_WITHOUT_ID);

    private static final List<MigrationTable> TABLES = List.of(
        CITIES,
        SCHEDULER_SETTINGS,
        table("schedule_tasks", List.of("id", "last_processed_id", "attempt", "provider", "scrap", "last_sync_at", "created_at",
            "updated_at")).build(),
        table("scheduled_task_records", List.of("id", "provider", "task_key", "status", "error_message", "processed_at",
            "created_at", "updated_at")).build(),
        table("currency_rates", List.of("id", "rate_date", "currency", "per_currency", "rate", "created_at", "updated_at"))
            .build(),
        table("listings", List.of("id", "source", "source_id", "source_url", "title", "description", "street", "metro_station",
            "city", "price", "currency", "rooms", "area_total", "room_area", "geo_lat", "geo_lng", "floor", "floors_total",
            "furniture", "photos", "is_active", "created_at", "updated_at", "parsed_at", "updated_at_provider", "category"))
            .arrayColumns(Set.of("photos"))
            .build());

    public ProcessingMigrationParticipant() {
        super("processing-ms");
    }

    @Override
    protected List<MigrationTable> tables() {
        return TABLES;
    }

    @Override
    protected List<TableResult> runTable(MigrationTable table, MigrationContext context) {
        MigrationTable withoutId = NATURAL_KEY_SECOND_PASSES.get(table.name());
        if (withoutId == null) {
            return super.runTable(table, context);
        }
        List<TableResult> withId = super.runTable(table, context);
        List<TableResult> withoutIdResults = super.runTable(withoutId, context);
        if (withId.isEmpty() || withoutIdResults.isEmpty()) {
            return List.of();
        }
        return List.of(merge(withId.get(0), withoutIdResults.get(0)));
    }

    static TableResult merge(TableResult withId, TableResult withoutId) {
        return new TableResult(withId.table(), withId.total(), withId.migrated() + withoutId.migrated(),
            withId.skipped() - withoutId.migrated(), withId.failed());
    }

    private static MigrationTable.MigrationTableBuilder table(String name, List<String> columns) {
        return MigrationTable.builder()
            .name(name)
            .sourceTable("public." + name)
            .columns(columns)
            .sinceColumn("updated_at");
    }

    private static RowMapper<Object[]> withoutLeadingId(List<String> columns) {
        ColumnRowMapper mapper = ColumnRowMapper.of(columns, Set.of());
        return (rs, rowNum) -> Arrays.copyOfRange(mapper.mapRow(rs, rowNum), 1, columns.size());
    }
}
