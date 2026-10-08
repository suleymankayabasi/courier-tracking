package com.migrosone.couriertracking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.migrosone.couriertracking.model.Store;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

class JsonFileStoreRepositoryTest {
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void loadsTheStoresFile() {
		StoreRepository repository = new JsonFileStoreRepository(jsonMapper, new ClassPathResource("stores.json"));

		assertThat(repository.findAll())
				.hasSize(5)
				.first()
				.isEqualTo(new Store("Ataşehir MMM Migros", 40.9923307, 29.1244229));
	}

	@Test
	void missingFileIsRejected() {
		Resource missing = new ClassPathResource("missing.json");

		assertThatThrownBy(() -> new JsonFileStoreRepository(jsonMapper, missing))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot read stores")
				.hasMessageContaining("missing.json");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"not json",
			"{\"name\":\"not an array\"}",
			"[{\"name\":\"x\",\"lat\":\"abc\",\"lng\":1}]",
			"[{\"name\":\"no coordinates\"}]",
			"[{\"lat\":1,\"lng\":2}]",
			"[{\"name\":\" \",\"lat\":1,\"lng\":2}]",
			"[{\"name\":\"x\",\"lat\":999,\"lng\":2}]"})
	void invalidStoreIsRejected(String content) {
		assertThatThrownBy(() -> new JsonFileStoreRepository(jsonMapper, resource(content)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Cannot read stores");
	}

	@ParameterizedTest
	@ValueSource(strings = {"[]", "null"})
	void fileWithoutStoresIsRejected(String content) {
		assertThatThrownBy(() -> new JsonFileStoreRepository(jsonMapper, resource(content)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("contains no stores");
	}

	@Test
	void duplicateStoreNameIsRejected() {
		String content = "[{\"name\":\"a\",\"lat\":1,\"lng\":2},{\"name\":\"a\",\"lat\":3,\"lng\":4}]";

		assertThatThrownBy(() -> new JsonFileStoreRepository(jsonMapper, resource(content)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Duplicate store name: a");
	}

	private static Resource resource(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}
}
