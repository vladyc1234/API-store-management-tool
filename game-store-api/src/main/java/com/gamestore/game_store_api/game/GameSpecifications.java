package com.gamestore.game_store_api.game;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

public final class GameSpecifications {

	private GameSpecifications() {
	}

	public static Specification<Game> catalog(String text, String genre, String platform,
			BigDecimal minimumPrice, BigDecimal maximumPrice, Boolean active) {
		return textContains(text)
				.and(equalsIgnoreCase("genre", genre))
				.and(equalsIgnoreCase("platform", platform))
				.and(priceAtLeast(minimumPrice))
				.and(priceAtMost(maximumPrice))
				.and(hasActiveState(active));
	}

	private static Specification<Game> textContains(String value) {
		return (root, query, builder) -> {
			if (value == null || value.isBlank()) {
				return builder.conjunction();
			}
			var pattern = "%" + value.trim().toLowerCase(java.util.Locale.ROOT) + "%";
			return builder.or(
					builder.like(builder.lower(root.get("title")), pattern),
					builder.like(builder.lower(root.get("sku")), pattern),
					builder.like(builder.lower(root.get("description")), pattern));
		};
	}

	private static Specification<Game> equalsIgnoreCase(String field, String value) {
		return (root, query, builder) -> value == null || value.isBlank()
				? builder.conjunction()
				: builder.equal(builder.lower(root.get(field)), value.trim().toLowerCase(java.util.Locale.ROOT));
	}

	private static Specification<Game> priceAtLeast(BigDecimal value) {
		return (root, query, builder) -> value == null
				? builder.conjunction() : builder.greaterThanOrEqualTo(root.get("price"), value);
	}

	private static Specification<Game> priceAtMost(BigDecimal value) {
		return (root, query, builder) -> value == null
				? builder.conjunction() : builder.lessThanOrEqualTo(root.get("price"), value);
	}

	private static Specification<Game> hasActiveState(Boolean value) {
		return (root, query, builder) -> value == null
				? builder.conjunction() : builder.equal(root.get("active"), value);
	}
}
