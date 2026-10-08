package com.migrosone.couriertracking.json;

import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonStoreReader {
	private final JsonMapper jsonMapper;

	public JsonStoreReader(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public List<Store> read(Resource storesFile) {
		StoreDocument[] documents = parse(storesFile);
		if (documents == null) {
			return List.of();
		}
		try {
			return Arrays.stream(documents).map(JsonStoreReader::toStore).toList();
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Invalid store in " + storesFile.getDescription() + ": " + ex.getMessage(), ex);
		}
	}

	private StoreDocument[] parse(Resource storesFile) {
		try (InputStream input = storesFile.getInputStream()) {
			return jsonMapper.readValue(input, StoreDocument[].class);
		}
		catch (IOException | JacksonException ex) {
			throw new IllegalStateException("Cannot read stores from " + storesFile.getDescription(), ex);
		}
	}

	private static Store toStore(StoreDocument document) {
		if (document == null) {
			throw new IllegalArgumentException("store entry must not be null");
		}
		return new Store(document.name(), new GeoPoint(document.lat(), document.lng()));
	}

	private record StoreDocument(String name, double lat, double lng) {
	}
}
