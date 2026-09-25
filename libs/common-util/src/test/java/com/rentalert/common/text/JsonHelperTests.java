package com.rentalert.common.text;

import java.time.LocalDate;
import java.util.List;

import com.rentalert.common.exception.AppException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonHelperTests {

	@Test
	void roundTripsWithJavaTimeAndSkipsNulls() {
		var sample = new Sample("flat", null, LocalDate.of(2026, 9, 25));

		String json = JsonHelper.toJson(sample);

		assertThat(json).doesNotContain("note").contains("\"2026-09-25\"");
		assertThat(JsonHelper.fromJson(json, Sample.class)).isEqualTo(sample);
	}

	@Test
	void readsGenericTypesAndIgnoresUnknownProperties() {
		List<Sample> list = JsonHelper.fromJson("[{\"name\":\"a\",\"extra\":1}]", new TypeReference<List<Sample>>() { });

		assertThat(list).containsExactly(new Sample("a", null, null));
	}

	@Test
	void wrapsInvalidJsonInAppException() {
		assertThatThrownBy(() -> JsonHelper.fromJson("{", new TypeReference<List<Sample>>() { }))
				.isInstanceOf(AppException.class);
	}

	record Sample(String name, String note, LocalDate date) {
	}
}
