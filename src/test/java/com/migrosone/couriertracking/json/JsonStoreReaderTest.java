package com.migrosone.couriertracking.json;

import static com.migrosone.couriertracking.support.TestFixtures.ATASEHIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

class JsonStoreReaderTest {
	private final JsonStoreReader reader = new JsonStoreReader(JsonMapper.builder().build());

	@Test
	void readsTheStoresFile() {
		assertThat(reader.read(new ClassPathResource("stores.json"))).hasSize(5).first().isEqualTo(ATASEHIR);
	}

	@ParameterizedTest
	@ValueSource(strings = {"[]", "null"})
	void readsNoStoresFromAnEmptyDocument(String content) {
		assertThat(reader.read(resourceOf(content))).isEmpty();
	}

	@Test
	void rejectsMissingFile() {
		assertThatThrownBy(() -> reader.read(new ClassPathResource("missing.json")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot read stores")
				.hasMessageContaining("missing.json");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"not json",
			"{\"name\":\"not an array\"}",
			"[{\"name\":\"store\",\"lat\":\"abc\",\"lng\":1}]",
			"[{\"name\":\"store without coordinates\"}]"})
	void rejectsMalformedFile(String content) {
		assertThatThrownBy(() -> reader.read(resourceOf(content)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot read stores");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"[null]",
			"[{\"lat\":1,\"lng\":2}]",
			"[{\"name\":\" \",\"lat\":1,\"lng\":2}]",
			"[{\"name\":\"store\",\"lat\":999,\"lng\":2}]"})
	void rejectsInvalidStore(String content) {
		assertThatThrownBy(() -> reader.read(resourceOf(content)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Invalid store");
	}

	private static Resource resourceOf(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}
}
