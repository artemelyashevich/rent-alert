package com.rentalert.listing_ms.migration;

import com.rentalert.kafka.migration.AbstractMonolithMigrationParticipant;
import com.rentalert.kafka.migration.MigrationTable;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ListingMigrationParticipant extends AbstractMonolithMigrationParticipant {

    public ListingMigrationParticipant() {
        super("listing-ms");
    }

    @Override
    protected List<MigrationTable> tables() {
        return List.of(
            MigrationTable.builder()
                .name("listing_status")
                .sourceTable("public.listing_status")
                .columns(List.of("id", "name", "color", "description", "created_at", "updated_at",
                    "shows_in_calendar"))
                .sinceColumn("updated_at")
                .build(),
            MigrationTable.builder()
                .name("listings")
                .sourceTable("public.listings")
                .columns(List.of("id", "source", "source_id", "source_url", "title", "description", "street",
                    "metro_station", "city", "price", "currency", "rooms", "area_total", "room_area", "geo_lat",
                    "geo_lng", "floor", "floors_total", "furniture", "photos", "is_active", "created_at", "updated_at",
                    "parsed_at", "updated_at_provider", "category"))
                .sinceColumn("updated_at")
                .arrayColumns(Set.of("photos"))
                .build(),
            MigrationTable.builder()
                .name("favorites")
                .sourceTable("public.favorites")
                .columns(List.of("user_id", "listing_id", "listing_status_id", "viewing_date", "created_at",
                    "updated_at"))
                .keyColumns(List.of("user_id", "listing_id"))
                .sinceColumn("updated_at")
                .build(),
            MigrationTable.builder()
                .name("hidden_listings")
                .sourceTable("public.hidden_listings")
                .columns(List.of("user_id", "listing_id", "created_at"))
                .keyColumns(List.of("user_id", "listing_id"))
                .sinceColumn("created_at")
                .build(),
            MigrationTable.builder()
                .name("currency_rates")
                .sourceTable("public.currency_rates")
                .columns(List.of("id", "rate_date", "currency", "per_currency", "rate", "created_at", "updated_at"))
                .sinceColumn("updated_at")
                .build());
    }
}
