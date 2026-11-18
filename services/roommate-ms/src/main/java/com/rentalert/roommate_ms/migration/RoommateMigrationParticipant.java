package com.rentalert.roommate_ms.migration;

import com.rentalert.kafka.migration.AbstractMonolithMigrationParticipant;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MigrationTable;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoommateMigrationParticipant extends AbstractMonolithMigrationParticipant {

    public RoommateMigrationParticipant() {
        super("roommate-ms");
    }

    @Override
    protected List<MigrationTable> tables() {
        return RoommateMigrationTables.all();
    }

    @Override
    protected void preflight(MigrationContext context, JdbcTemplate source) {
        warnIfAny("roommate_listings: authors with more than one ACTIVE listing (the most recently updated stays ACTIVE, others are copied as PAUSED)",
            "SELECT count(*) FROM (SELECT author_user_id FROM public.roommate_listings WHERE status = 'ACTIVE' GROUP BY author_user_id HAVING count(*) > 1) d");
        warnIfAny("roommate_match_participants: user and scenario pairs with more than one committed row (the most recently updated stays committed, others are copied as not committed)",
            "SELECT count(*) FROM (SELECT user_id, scenario FROM public.roommate_match_participants WHERE committed GROUP BY user_id, scenario HAVING count(*) > 1) d");
        requireNone("roommate_matches violates user_lo_id < user_hi_id (fix in the monolith before migrating)",
            "SELECT id FROM public.roommate_matches WHERE user_lo_id >= user_hi_id ORDER BY id");
        warnIfAny("roommate_shared_favorites: listing status without a ListingStatusName match, such rows will be skipped",
            "SELECT count(*) FROM public.roommate_shared_favorites f JOIN public.listing_status ls ON ls.id = f.listing_status_id WHERE ls.name NOT IN ("
                + RoommateMigrationTables.SHARED_FAVORITE_STATUSES + ")");
    }
}
