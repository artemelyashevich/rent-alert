package com.rentalert.roommate_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.migration.MigrationCommand;
import com.rentalert.dto.event.migration.MigrationReply;
import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.MigrationControl;
import com.rentalert.kafka.migration.MonolithSource;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

class RoommateMigrationParticipantTests {

    private static final Pattern FROM_TABLE = Pattern.compile("FROM public\\.(\\w+)");
    private static final Pattern INSERT = Pattern.compile("INSERT INTO (\\w+) \\(([^)]*)\\) VALUES \\((.*)\\) ON CONFLICT");
    private static final String CAST_ARRAY = "CAST(? AS VARCHAR[])";

    private final JdbcTemplate target = mock(JdbcTemplate.class);
    private final JdbcTemplate source = mock(JdbcTemplate.class, withSettings().defaultAnswer(this::sourceDefault));
    private final ObjectProvider<MonolithSource> monolithSource = mock(ObjectProvider.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final MigrationControl migrationControl = mock(MigrationControl.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final ResultSet row = mock(ResultSet.class);
    private final UUID migrationId = UUID.randomUUID();
    private List<Long> matchPairViolations = List.of();
    private final RoommateMigrationParticipant participant = new RoommateMigrationParticipant();

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(participant, "target", target);
        ReflectionTestUtils.setField(participant, "monolithSource", monolithSource);
        ReflectionTestUtils.setField(participant, "targetTransactions", transactionTemplate);
        ReflectionTestUtils.setField(participant, "transactionTemplate", transactionTemplate);
        ReflectionTestUtils.setField(participant, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(participant, "migrationControl", migrationControl);
        ReflectionTestUtils.setField(participant, "outboxPublisher", outboxPublisher);
        when(row.getString("gender_pref")).thenReturn("{\"FEMALE\"}");
        when(monolithSource.getIfAvailable()).thenReturn(new MonolithSource(source));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(target.batchUpdate(anyString(), anyList())).thenReturn(new int[] {1});
        doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        doAnswer(invocation -> invocation.<TransactionCallback<Object>>getArgument(0).doInTransaction(null))
            .when(transactionTemplate).execute(any());
        doAnswer(invocation -> {
            invocation.<RowCallbackHandler>getArgument(2).processRow(row);
            return null;
        }).when(source).query(anyString(), any(Object[].class), any(RowCallbackHandler.class));
        when(migrationControl.tryWithLock(any(), any(), any())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        });
    }

    @Test
    void missingMonolithSourceFailsWithEnableHint() {
        when(monolithSource.getIfAvailable()).thenReturn(null);

        MigrationReply reply = runCommand(settings(List.of()));

        assertThat(reply.success()).isFalse();
        assertThat(reply.error()).isEqualTo("IllegalStateException: migration is disabled: set app.migration.enabled=true");
        verifyNoInteractions(target);
    }

    @Test
    void matchPairOrderViolationStopsBeforeAnyWrite() {
        stubMatchPairViolation();

        MigrationReply reply = runCommand(settings(List.of()));

        assertThat(reply.success()).isFalse();
        assertThat(reply.error()).contains("roommate_matches violates user_lo_id < user_hi_id").contains("ids [7, 9]");
        assertThat(selectedTables()).isEmpty();
        verifyNoInteractions(target);
    }

    @Test
    void matchPairOrderIsCheckedEvenWhenTableFilterExcludesMatches() {
        stubMatchPairViolation();

        MigrationReply reply = runCommand(settings(List.of("roommate_hidden")));

        assertThat(reply.success()).isFalse();
        assertThat(reply.error()).contains("ids [7, 9]");
        assertThat(selectedTables()).isEmpty();
        verifyNoInteractions(target);
    }

    @Test
    void tablesAreMigratedInScriptOrder() {
        MigrationReply reply = runCommand(settings(List.of()));

        assertThat(reply.success()).isTrue();
        assertThat(selectedTables()).containsExactly(
            "roommate_profiles",
            "roommate_listings",
            "roommate_matches",
            "roommate_match_participants",
            "roommate_match_events",
            "roommate_viewings",
            "roommate_shared_favorites",
            "roommate_hidden",
            "roommate_moderation_tasks");
        assertThat(reply.tables()).hasSize(9);
    }

    @Test
    void tableFilterSelectsOnlyRequestedTables() {
        MigrationReply reply = runCommand(settings(List.of("roommate_hidden")));

        assertThat(selectedTables()).containsExactly("roommate_hidden");
        assertThat(reply.tables()).extracting(TableResult::table).containsExactly("roommate_hidden");
    }

    @Test
    void insertStatementsHaveOnePlaceholderPerColumn() {
        runCommand(settings(List.of()));

        List<Matcher> inserts = insertStatements();
        assertThat(inserts).hasSize(9);
        for (Matcher insert : inserts) {
            int columns = insert.group(2).split(",").length;
            assertThat(insert.group(3).chars().filter(c -> c == '?').count())
                .as(insert.group(1))
                .isEqualTo(columns);
        }
    }

    @Test
    void insertStatementsCastArrayColumnsAndResolveConflictOnKey() {
        runCommand(settings(List.of()));

        String profiles = insertSql("roommate_profiles");
        String listings = insertSql("roommate_listings");
        String favorites = insertSql("roommate_shared_favorites");
        String hidden = insertSql("roommate_hidden");

        assertThat(StringUtils.countOccurrencesOf(profiles, CAST_ARRAY)).isEqualTo(2);
        assertThat(profiles).endsWith("ON CONFLICT (user_id) DO NOTHING");
        assertThat(StringUtils.countOccurrencesOf(listings, CAST_ARRAY)).isEqualTo(2);
        assertThat(listings).endsWith("ON CONFLICT (id) DO NOTHING");
        assertThat(favorites).doesNotContain("CAST(").endsWith("ON CONFLICT (match_id, listing_id) DO NOTHING");
        assertThat(hidden).doesNotContain("CAST(").endsWith("ON CONFLICT (user_id, subject_type, subject_id) DO NOTHING");
    }

    @Test
    void arrayColumnsAreReadAsTextAndWrittenAsRowValues() {
        runCommand(settings(List.of("roommate_profiles")));

        Object[] migratedRow = insertRows("roommate_profiles").get(0);
        assertThat(migratedRow[2]).isEqualTo("{\"FEMALE\"}");
    }

    @Test
    void listingPolicyPausesAllButTheMostRecentActiveListing() {
        runCommand(settings(List.of("roommate_listings")));

        assertThat(selectSql("roommate_listings"))
            .contains("DISTINCT ON (author_user_id)")
            .contains("CASE WHEN r.status = 'ACTIVE' AND r.id NOT IN (SELECT id FROM kept_active) THEN 'PAUSED' ELSE r.status END AS status");
    }

    @Test
    void participantPolicyClearsCommittedFlagOfAllButTheMostRecentRow() {
        runCommand(settings(List.of("roommate_match_participants")));

        assertThat(selectSql("roommate_match_participants"))
            .contains("DISTINCT ON (user_id, scenario)")
            .contains("(p.committed AND p.id IN (SELECT id FROM kept_committed)) AS committed");
    }

    @Test
    void sharedFavoritesAreFilteredByListingStatusName() {
        runCommand(settings(List.of("roommate_shared_favorites")));

        assertThat(selectSql("roommate_shared_favorites"))
            .contains("JOIN public.listing_status ls ON ls.id = f.listing_status_id")
            .contains("ls.name IN (" + RoommateMigrationTables.SHARED_FAVORITE_STATUSES + ")")
            .contains("ls.name AS listing_status");
    }

    @Test
    void viewingsSnapshotListingFieldsFromTheMonolith() {
        runCommand(settings(List.of("roommate_viewings")));

        assertThat(selectSql("roommate_viewings"))
            .contains("LEFT JOIN public.listings l ON l.id = v.listing_id")
            .contains("l.title AS listing_title")
            .contains("CASE WHEN array_length(l.photos, 1) > 0 THEN l.photos[1] END AS listing_cover_photo_url");
    }

    private void stubMatchPairViolation() {
        matchPairViolations = List.of(7L, 9L);
    }

    private Object sourceDefault(InvocationOnMock invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("query") && invocation.getArgument(0) instanceof String sql
            && sql.contains("user_lo_id >= user_hi_id")) {
            return matchPairViolations;
        }
        return Answers.RETURNS_DEFAULTS.answer(invocation);
    }

    private MigrationReply runCommand(MigrationSettings settings) {
        participant.onCommand(new MigrationCommand(migrationId, 1, "roommate-ms", settings));
        ArgumentCaptor<Object> reply = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.MIGRATION_REPLIES), eq(migrationId.toString()), reply.capture());
        return (MigrationReply) reply.getValue();
    }

    private List<String> selectedTables() {
        return selectSql().stream()
            .map(sql -> {
                Matcher matcher = FROM_TABLE.matcher(sql);
                return matcher.find() ? matcher.group(1) : "?";
            })
            .toList();
    }

    private String selectSql(String table) {
        return selectSql().stream()
            .filter(sql -> sql.contains("FROM public." + table + " "))
            .findFirst()
            .orElseThrow();
    }

    private List<String> selectSql() {
        return mockingDetails(source).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("query")
                && invocation.getArguments().length == 3
                && invocation.getArguments()[2] instanceof RowCallbackHandler)
            .map(invocation -> (String) invocation.getArguments()[0])
            .toList();
    }

    private List<Matcher> insertStatements() {
        return mockingDetails(target).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("batchUpdate"))
            .map(invocation -> {
                Matcher matcher = INSERT.matcher((String) invocation.getArguments()[0]);
                assertThat(matcher.find()).isTrue();
                return matcher;
            })
            .toList();
    }

    private String insertSql(String table) {
        return mockingDetails(target).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("batchUpdate"))
            .map(invocation -> (String) invocation.getArguments()[0])
            .filter(sql -> sql.startsWith("INSERT INTO " + table + " "))
            .findFirst()
            .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> insertRows(String table) {
        return mockingDetails(target).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("batchUpdate")
                && ((String) invocation.getArguments()[0]).startsWith("INSERT INTO " + table + " "))
            .findFirst()
            .map(invocation -> (List<Object[]>) invocation.getArguments()[1])
            .orElseThrow();
    }

    private static MigrationSettings settings(List<String> tables) {
        return new MigrationSettings(100, null, null, false, tables);
    }
}
