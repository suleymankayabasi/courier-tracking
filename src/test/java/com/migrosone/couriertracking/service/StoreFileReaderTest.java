package com.migrosone.couriertracking.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.migrosone.couriertracking.domain.entity.Store;
import com.migrosone.couriertracking.geo.GeoPoint;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class StoreFileReaderTest {
	private final StoreFileReader reader = new StoreFileReader(JsonMapper.builder().build());

	@Test
	void readsBundledStoresFileWithTurkishCharactersIntact() {
		List<Store> stores = reader.read(new ClassPathResource("stores.json"));

		assertThat(stores).extracting(Store::getName).containsExactly(
				"Ataşehir MMM Migros", "Novada MMM Migros", "Beylikdüzü 5M Migros", "Ortaköy MMM Migros",
				"Caddebostan MMM Migros");
		assertThat(stores.getFirst().location()).isEqualTo(new GeoPoint(40.9923307, 29.1244229));
		assertThat(stores).allSatisfy(store -> assertThat(store.getId()).isNull());
	}

	@Test
	void failsWhenFileIsMissing() {
		assertThatThrownBy(() -> reader.read(new ClassPathResource("does-not-exist.json")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Store file not found");
	}

	@Test
	void failsOnMalformedJson() {
		assertThatThrownBy(() -> read("[{\"name\": "))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Malformed store file");
	}

	@Test
	void failsOnEmptyList() {
		assertThatThrownBy(() -> read("[]"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("contains no stores");
	}

	@Test
	void failsOnZeroByteFile() {
		assertThatThrownBy(() -> read(""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Malformed store file");
	}

	@Test
	void failsOnNullDocument() {
		assertThatThrownBy(() -> read("null"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("contains no stores");
	}

	@Test
	void failsOnNullStoreEntry() {
		assertThatThrownBy(() -> read("[null]"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Store entry 0 is null");
	}

	@Test
	void failsOnOutOfRangeCoordinate() {
		assertThatThrownBy(() -> read("""
				[{"name": "Far Away", "lat": 200.0, "lng": 29.0}]"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Store entry 0 (Far Away) is invalid");
	}

	@Test
	void failsOnMissingName() {
		assertThatThrownBy(() -> read("""
				[{"lat": 41.0, "lng": 29.0}]"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("has no name");
	}

	@Test
	void failsOnBlankName() {
		assertThatThrownBy(() -> read("""
				[{"name": "   ", "lat": 41.0, "lng": 29.0}]"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("has no name");
	}

	@Test
	void failsOnMissingLatitude() {
		assertThatThrownBy(() -> read("""
				[{"name": "No Latitude", "lng": 29.0}]"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("has no coordinates");
	}

	@Test
	void failsOnMissingCoordinates() {
		assertThatThrownBy(() -> read("""
				[{"name": "No Longitude", "lat": 41.0}]"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("has no coordinates");
	}

	@Test
	void failsOnDuplicateNames() {
		assertThatThrownBy(() -> read("""
				[{"name": "Same", "lat": 41.0, "lng": 29.0},
				 {"name": "Same", "lat": 42.0, "lng": 30.0}]"""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Duplicate store name");
	}

	private List<Store> read(String json) {
		return reader.read(new ByteArrayResource(json.getBytes(UTF_8)));
	}
}
