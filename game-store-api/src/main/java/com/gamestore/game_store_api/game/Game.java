package com.gamestore.game_store_api.game;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "games", uniqueConstraints = @UniqueConstraint(name = "uk_games_sku", columnNames = "sku"))
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 50)
	@Column(nullable = false, length = 50)
	private String sku;

	@NotBlank
	@Size(max = 200)
	@Column(nullable = false, length = 200)
	private String title;

	@Size(max = 5000)
	@Column(length = 5000)
	private String description;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 8, fraction = 2)
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@PositiveOrZero
	@Column(name = "stock_quantity", nullable = false)
	private int stockQuantity;

	@Column(nullable = false)
	private boolean active = true;

	@Version
	@Column(nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Game() {
	}

	public Game(String sku, String title, String description, BigDecimal price, int stockQuantity) {
		this.sku = requireText(sku, "sku").toUpperCase(Locale.ROOT);
		this.title = requireText(title, "title");
		this.description = normalizeDescription(description);
		this.price = requireMoney(price);
		if (stockQuantity < 0) {
			throw new IllegalArgumentException("stockQuantity must not be negative");
		}
		this.stockQuantity = stockQuantity;
	}

	public void changePrice(BigDecimal newPrice) {
		this.price = requireMoney(newPrice);
	}

	public void increaseStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		this.stockQuantity = Math.addExact(this.stockQuantity, quantity);
	}

	public void decreaseStock(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		if (quantity > stockQuantity) {
			throw new IllegalStateException("insufficient stock");
		}
		this.stockQuantity -= quantity;
	}

	public void deactivate() {
		this.active = false;
	}

	public Long getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public boolean isActive() {
		return active;
	}

	public long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	private static BigDecimal requireMoney(BigDecimal value) {
		if (value == null || value.signum() < 0) {
			throw new IllegalArgumentException("price must not be negative");
		}
		return value.setScale(2, RoundingMode.UNNECESSARY);
	}

	private static String normalizeDescription(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}
}
