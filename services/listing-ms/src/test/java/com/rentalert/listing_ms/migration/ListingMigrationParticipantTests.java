package com.rentalert.listing_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MonolithSource;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@SuppressWarnings("unchecked")
class ListingMigrationParticipantTests {

    private static final List<String> TABLE_ORDER = List.of(
        "listing_status", "listings", "favorites", "hidden_listings", "currency_rates");
    private static final Instant SINCE = Instant.parse("2026-03-01T00:00:00Z");
    private static final String PHOTOS_LITERAL = "{\"https://img/1.jpg\",\"https://img/2.jpg\"}";
    private static final String LISTINGS_INSERT = "INSERT INTO listings (id, source, source_id, source_url, title, "
        + "description, street, metro_station, city, price, currency, rooms, area_total, room_area, geo_lat, geo_lng, "
        + "floor, floors_total, furniture, photos, is_active, created_at, updated_at, parsed_at, updated_at_provider, "
        + "category) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS VARCHAR[]), ?, ?, ?, "
        + "?, ?, ?) ON CONFLICT (id) DO NOTHING";

    private final JdbcTemplate source = mock(JdbcTemplate.class);
    private final JdbcTemplate target = mock(JdbcTemplate.class);
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final ObjectProvider<MonolithSource> monolithSource = mock(ObjectProvider.class);

    @BeforeEach
    void setUp() {
        when(monolithSource.getIfAvailable()).thenReturn(new MonolithSource(source));
        doAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null))
            .when(transactions).execute(any());
        doAnswer(invocation -> {
            ((Consumer<?>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());
        when(target.batchUpdate(anyString(), anyList()))
            .thenAnswer(invocation -> new int[] {((List<?>) invocation.getArgument(1)).size()});
    }

    @Test
    void migratesTablesInDependencyOrder() {
        List<TableResult> results = migrate(settings(null, List.of()));

        assertThat(results).extracting(TableResult::table).containsExactlyElementsOf(TABLE_ORDER);
    }

    @Test
    void settingsTableFilterSkipsUnselectedTables() {
        List<TableResult> results = migrate(settings(null, List.of("favorites")));

        assertThat(results).extracting(TableResult::table).containsExactly("favorites");
    }

    @Test
    void failsWhenMonolithSourceIsNotConfigured() {
        when(monolithSource.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> migrate(settings(null, List.of())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("migration is disabled: set app.migration.enabled=true");
    }

    @Test
    void readsEveryTableFromPublicSchemaWithKeysetPagingAndSinceFilter() {
        migrate(settings(SINCE, List.of()));

        ArgumentCaptor<String> counts = ArgumentCaptor.forClass(String.class);
        verify(source, times(TABLE_ORDER.size())).queryForObject(counts.capture(), eq(Long.class), any());
        assertThat(counts.getAllValues()).map(ListingMigrationParticipantTests::normalize).containsExactly(
            "SELECT count(*) FROM public.listing_status WHERE 1 = 1 AND updated_at >= ?",
            "SELECT count(*) FROM public.listings WHERE 1 = 1 AND updated_at >= ?",
            "SELECT count(*) FROM public.favorites WHERE 1 = 1 AND updated_at >= ?",
            "SELECT count(*) FROM public.hidden_listings WHERE 1 = 1 AND created_at >= ?",
            "SELECT count(*) FROM public.currency_rates WHERE 1 = 1 AND updated_at >= ?");

        ArgumentCaptor<String> selects = ArgumentCaptor.forClass(String.class);
        verify(source, times(TABLE_ORDER.size())).query(selects.capture(), any(Object[].class),
            any(RowCallbackHandler.class));
        assertThat(selects.getAllValues()).map(ListingMigrationParticipantTests::normalize).containsExactly(
            "SELECT id, name, color, description, created_at, updated_at, shows_in_calendar FROM public.listing_status"
                + " WHERE id > ? AND updated_at >= ? ORDER BY id LIMIT ?",
            "SELECT id, source, source_id, source_url, title, description, street, metro_station, city, price,"
                + " currency, rooms, area_total, room_area, geo_lat, geo_lng, floor, floors_total, furniture, photos,"
                + " is_active,"
                + " created_at, updated_at, parsed_at, updated_at_provider, category FROM public.listings"
                + " WHERE id > ? AND updated_at >= ? ORDER BY id LIMIT ?",
            "SELECT user_id, listing_id, listing_status_id, viewing_date, created_at, updated_at FROM public.favorites"
                + " WHERE 1 = 1 AND updated_at >= ? ORDER BY user_id, listing_id LIMIT ?",
            "SELECT user_id, listing_id, created_at FROM public.hidden_listings"
                + " WHERE 1 = 1 AND created_at >= ? ORDER BY user_id, listing_id LIMIT ?",
            "SELECT id, rate_date, currency, per_currency, rate, created_at, updated_at FROM public.currency_rates"
                + " WHERE id > ? AND updated_at >= ? ORDER BY id LIMIT ?");
    }

    @Test
    void listingsReadsPhotosAsArrayLiteralAndWritesThemWithArrayCast() throws Exception {
        doAnswer(invocation -> {
            ((RowCallbackHandler) invocation.getArgument(2)).processRow(listingRow());
            return null;
        }).when(source).query(anyString(), any(Object[].class), any(RowCallbackHandler.class));

        migrate(settings(null, List.of("listings")));

        ArgumentCaptor<String> insert = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Object[]>> rows = ArgumentCaptor.forClass(List.class);
        verify(target).batchUpdate(insert.capture(), rows.capture());
        assertThat(insert.getValue()).isEqualTo(LISTINGS_INSERT);
        assertThat(rows.getValue()).hasSize(1);
        assertThat(rows.getValue().get(0)).hasSize(26);
        assertThat(rows.getValue().get(0)[0]).isEqualTo("id");
        assertThat(rows.getValue().get(0)[19]).isEqualTo(PHOTOS_LITERAL);
        assertThat(rows.getValue().get(0)[25]).isEqualTo("category");
    }

    @Test
    void resetsIdSequenceOnlyForTablesWithIdKey() {
        migrate(settings(null, List.of()));

        ArgumentCaptor<String> resets = ArgumentCaptor.forClass(String.class);
        verify(target, times(3)).execute(resets.capture());
        assertThat(resets.getAllValues()).containsExactly(
            resetSequence("listing_status"), resetSequence("listings"), resetSequence("currency_rates"));
    }

    private List<TableResult> migrate(MigrationSettings settings) {
        MigrationContext context = mock(MigrationContext.class);
        when(context.settings()).thenReturn(settings);
        return participant().run(context);
    }

    private Runner participant() {
        Runner participant = new Runner();
        ReflectionTestUtils.setField(participant, "target", target);
        ReflectionTestUtils.setField(participant, "monolithSource", monolithSource);
        ReflectionTestUtils.setField(participant, "targetTransactions", transactions);
        return participant;
    }

    private ResultSet listingRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rs.getString("photos")).thenReturn(PHOTOS_LITERAL);
        return rs;
    }

    private static String resetSequence(String table) {
        return "SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), COALESCE(MAX(id), 0) + 1, false) FROM "
            + table;
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static MigrationSettings settings(Instant since, List<String> tables) {
        return new MigrationSettings(MigrationSettings.DEFAULT_BATCH_SIZE, null, since, false, tables);
    }

    private static final class Runner extends ListingMigrationParticipant {

        List<TableResult> run(MigrationContext context) {
            return migrate(context);
        }
    }
}
