package com.rentalert.roommate_ms.dto;

import java.util.List;

public record RoommateFeedPage<T>(List<T> items, String nextCursor, boolean hasMore) {

}
