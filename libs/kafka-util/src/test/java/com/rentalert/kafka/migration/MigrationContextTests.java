package com.rentalert.kafka.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.migration.MigrationProgress;
import com.rentalert.dto.migration.MigrationSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MigrationContextTests {

    private final MigrationControl control = mock(MigrationControl.class);
    private final List<MigrationProgress> sent = new ArrayList<>();
    private final UUID migrationId = UUID.randomUUID();
    private final MigrationContext context = new MigrationContext(migrationId, 2, "listing-ms",
        new MigrationSettings(100, null, null, false, List.of()), control, sent::add);

    @Test
    void progressIsThrottledPerTableAndFinishIsAlwaysSent() {
        for (int processed = 1; processed <= 50; processed++) {
            context.progress("listing", processed, 100);
        }
        context.progress("photo", 1, 10);

        assertThat(sent).containsExactly(
            new MigrationProgress(migrationId, 2, "listing-ms", "listing", 1, 100),
            new MigrationProgress(migrationId, 2, "listing-ms", "photo", 1, 10));

        context.finishTable("listing", 50, 100);

        assertThat(sent).hasSize(3);
        assertThat(sent.get(2)).isEqualTo(new MigrationProgress(migrationId, 2, "listing-ms", "listing", 50, 100));
    }

    @Test
    void cancelledReflectsControlFlag() {
        when(control.isCancelled(migrationId)).thenReturn(true);

        assertThat(context.cancelled()).isTrue();
    }

    @Test
    void cancelledIsFalseWhenFlagStoreFails() {
        when(control.isCancelled(any())).thenThrow(new IllegalStateException("redis is down"));

        assertThat(context.cancelled()).isFalse();
    }
}
