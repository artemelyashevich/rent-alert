package com.rentalert.roommate_ms.migration;

import com.rentalert.kafka.migration.MigrationTable;
import com.rentalert.roommate_ms.domain.ListingStatusName;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class RoommateMigrationTables {

    static final String SHARED_FAVORITE_STATUSES = Arrays.stream(ListingStatusName.values())
        .map(status -> "'" + status.name() + "'")
        .collect(Collectors.joining(","));

    private static final Set<String> PROFILE_ARRAY_COLUMNS = Set.of("gender_pref", "lifestyle_tags");
    private static final Set<String> LISTING_ARRAY_COLUMNS = Set.of("gender_pref", "house_rules");

    private RoommateMigrationTables() {
    }

    static List<MigrationTable> all() {
        return List.of(
            MigrationTable.builder()
                .name("roommate_profiles")
                .sourceTable("public.roommate_profiles")
                .columns(columns("user_id, gender, gender_pref, birth_year, budget_min, budget_max, currency, city, move_in_from, move_in_to, lifestyle_tags, schedule, smoking, pets, guests, cleanliness, bio_submitted, bio_approved, bio_mod_status, is_active, is_searchable, profile_version, reports_count, last_digest_at, created_at, updated_at"))
                .keyColumns(List.of("user_id"))
                .sinceColumn("updated_at")
                .arrayColumns(PROFILE_ARRAY_COLUMNS)
                .build(),
            MigrationTable.builder()
                .name("roommate_listings")
                .sourceTable("public.roommate_listings")
                .fromSource("public.roommate_listings r")
                .columns(columns("id, author_user_id, listing_id, city, address_exact, address_public, district, metro_station, geo_lat, geo_lng, rent_share, currency, room_count, flatmates_count, available_from, house_rules, description_submitted, description_approved, description_mod_status, status, gender_pref, age_min, age_max, reports_count, created_at, updated_at"))
                .sinceColumn("r.updated_at")
                .arrayColumns(LISTING_ARRAY_COLUMNS)
                .customSelectSql("WITH kept_active AS (SELECT DISTINCT ON (author_user_id) id FROM public.roommate_listings WHERE status = 'ACTIVE' ORDER BY author_user_id, updated_at DESC, id DESC) SELECT r.id, r.author_user_id, r.listing_id, r.city, r.address_exact, r.address_public, r.district, r.metro_station, r.geo_lat, r.geo_lng, r.rent_share, r.currency, r.room_count, r.flatmates_count, r.available_from, r.house_rules, r.description_submitted, r.description_approved, r.description_mod_status, CASE WHEN r.status = 'ACTIVE' AND r.id NOT IN (SELECT id FROM kept_active) THEN 'PAUSED' ELSE r.status END AS status, r.gender_pref, r.age_min, r.age_max, r.reports_count, r.created_at, r.updated_at FROM public.roommate_listings r WHERE r.id > ? {since} ORDER BY r.id LIMIT ?")
                .build(),
            MigrationTable.builder()
                .name("roommate_matches")
                .sourceTable("public.roommate_matches")
                .columns(columns("id, scenario, user_lo_id, user_hi_id, context_ref, roommate_listing_id, lo_decision, hi_decision, status, status_version, initiator_user_id, contacts_unlocked_at, chat_ref, expires_at, lo_moved, hi_moved, created_at, updated_at"))
                .sinceColumn("updated_at")
                .build(),
            MigrationTable.builder()
                .name("roommate_match_participants")
                .sourceTable("public.roommate_match_participants")
                .fromSource("public.roommate_match_participants p")
                .columns(columns("id, match_id, user_id, scenario, role, committed, created_at, updated_at"))
                .sinceColumn("p.updated_at")
                .customSelectSql("WITH kept_committed AS (SELECT DISTINCT ON (user_id, scenario) id FROM public.roommate_match_participants WHERE committed ORDER BY user_id, scenario, updated_at DESC, id DESC) SELECT p.id, p.match_id, p.user_id, p.scenario, p.role, (p.committed AND p.id IN (SELECT id FROM kept_committed)) AS committed, p.created_at, p.updated_at FROM public.roommate_match_participants p WHERE p.id > ? {since} ORDER BY p.id LIMIT ?")
                .build(),
            MigrationTable.builder()
                .name("roommate_match_events")
                .sourceTable("public.roommate_match_events")
                .columns(columns("id, match_id, from_status, to_status, actor_user_id, note, created_at, updated_at"))
                .sinceColumn("updated_at")
                .build(),
            MigrationTable.builder()
                .name("roommate_viewings")
                .sourceTable("public.roommate_viewings")
                .fromSource("public.roommate_viewings v")
                .columns(columns("id, match_id, listing_id, address, viewing_date, created_by_user_id, status, listing_title, listing_street, listing_metro_station, listing_price, listing_currency, listing_cover_photo_url, created_at, updated_at"))
                .sinceColumn("v.updated_at")
                .customSelectSql("SELECT v.id, v.match_id, v.listing_id, v.address, v.viewing_date, v.created_by_user_id, v.status, l.title AS listing_title, l.street AS listing_street, l.metro_station AS listing_metro_station, l.price AS listing_price, l.currency AS listing_currency, CASE WHEN array_length(l.photos, 1) > 0 THEN l.photos[1] END AS listing_cover_photo_url, v.created_at, v.updated_at FROM public.roommate_viewings v LEFT JOIN public.listings l ON l.id = v.listing_id WHERE v.id > ? {since} ORDER BY v.id LIMIT ?")
                .build(),
            MigrationTable.builder()
                .name("roommate_shared_favorites")
                .sourceTable("public.roommate_shared_favorites")
                .fromSource("public.roommate_shared_favorites f JOIN public.listing_status ls ON ls.id = f.listing_status_id")
                .whereSource("ls.name IN (" + SHARED_FAVORITE_STATUSES + ")")
                .columns(columns("match_id, listing_id, listing_status, viewing_date, added_by_user_id, created_at, updated_at"))
                .keyColumns(List.of("match_id", "listing_id"))
                .sinceColumn("f.updated_at")
                .selectExpressions(Map.of(
                    "match_id", "f.match_id",
                    "listing_id", "f.listing_id",
                    "listing_status", "ls.name AS listing_status",
                    "viewing_date", "f.viewing_date",
                    "added_by_user_id", "f.added_by_user_id",
                    "created_at", "f.created_at",
                    "updated_at", "f.updated_at"))
                .build(),
            MigrationTable.builder()
                .name("roommate_hidden")
                .sourceTable("public.roommate_hidden")
                .columns(columns("user_id, subject_type, subject_id, created_at"))
                .keyColumns(List.of("user_id", "subject_type", "subject_id"))
                .sinceColumn("created_at")
                .build(),
            MigrationTable.builder()
                .name("roommate_moderation_tasks")
                .sourceTable("public.roommate_moderation_tasks")
                .columns(columns("id, subject_type, subject_id, field, submitted_text, text_hash, status, reviewer_id, decision_reason, created_at, updated_at"))
                .sinceColumn("updated_at")
                .build());
    }

    private static List<String> columns(String list) {
        return Arrays.stream(list.split(",")).map(String::trim).toList();
    }
}
