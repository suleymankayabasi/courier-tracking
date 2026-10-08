package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.domain.entity.Store;
import com.migrosone.couriertracking.geo.GeoPoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StoreFileReader {
	private final JsonMapper jsonMapper;

	public StoreFileReader(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public List<Store> read(Resource storesFile) {
		if (!storesFile.exists()) {
			throw new IllegalStateException("Store file not found: " + storesFile.getDescription());
		}
		StoreDocument[] documents = parse(storesFile);
		if (documents == null || documents.length == 0) {
			throw new IllegalStateException("Store file contains no stores: " + storesFile.getDescription());
		}

		List<Store> stores = IntStream.range(0, documents.length)
				.mapToObj(index -> toStore(documents[index], index))
				.toList();
		Set<String> names = new HashSet<>();
		for (Store store : stores) {
			if (!names.add(store.getName())) {
				throw new IllegalStateException("Duplicate store name in store file: " + store.getName());
			}
		}
		return stores;
	}

	private StoreDocument[] parse(Resource storesFile) {
		try (InputStream inputStream = storesFile.getInputStream()) {
			return jsonMapper.readValue(inputStream, StoreDocument[].class);
		}
		catch (IOException | JacksonException ex) {
			throw new IllegalStateException("Malformed store file " + storesFile.getDescription(), ex);
		}
	}

	private static Store toStore(StoreDocument document, int index) {
		if (document == null) {
			throw new IllegalStateException("Store entry " + index + " is null");
		}
		if (document.name() == null || document.name().isBlank()) {
			throw new IllegalStateException("Store entry " + index + " has no name");
		}
		if (document.lat() == null || document.lng() == null) {
			throw new IllegalStateException("Store entry " + index + " (" + document.name() + ") has no coordinates");
		}
		try {
			return new Store(document.name(), new GeoPoint(document.lat(), document.lng()));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"Store entry " + index + " (" + document.name() + ") is invalid: " + ex.getMessage(), ex);
		}
	}

	record StoreDocument(String name, Double lat, Double lng) {
	}
}
