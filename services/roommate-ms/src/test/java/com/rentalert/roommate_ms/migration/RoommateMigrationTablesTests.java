package com.rentalert.roommate_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoommateMigrationTablesTests {

    @Test
    void tablesFollowMigrationScriptOrder() {
        List<String> names = RoommateMigrationTables.all().stream().map(table -> table.name()).toList();

        assertThat(names).containsExactly(
            "roommate_profiles",
            "roommate_listings",
            "roommate_matches",
            "roommate_match_participants",
            "roommate_match_events",
            "roommate_viewings",
            "roommate_shared_favorites",
            "roommate_hidden",
            "roommate_moderation_tasks");
    }

    @Test
    void sharedFavoriteStatusesAreTheListingStatusNames() {
        assertThat(RoommateMigrationTables.SHARED_FAVORITE_STATUSES)
            .isEqualTo("'INTERESTED','WAITING_RESPONSE','VIEWING_SCHEDULED','RESERVED','VIEWED','AGREED'");
    }
}
