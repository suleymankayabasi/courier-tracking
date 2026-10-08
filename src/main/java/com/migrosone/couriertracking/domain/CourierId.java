package com.migrosone.couriertracking.domain;

public final class CourierId {

	public static final int MAX_LENGTH = 64;

	public static final String ALLOWED_PATTERN = "[A-Za-z0-9._:-]*|\\s*";

	public static final String ALLOWED_MESSAGE = "courierId may only contain letters, digits, '.', '_', ':' and '-'";

	private CourierId() {
	}
}
