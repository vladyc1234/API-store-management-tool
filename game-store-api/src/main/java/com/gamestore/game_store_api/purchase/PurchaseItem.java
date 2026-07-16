package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;
import java.util.Objects;

import com.gamestore.game_store_api.game.Game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "purchase_items", uniqueConstraints = @UniqueConstraint(
		name = "uk_purchase_items_purchase_game", columnNames = {"purchase_id", "game_id"}))
public class PurchaseItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "purchase_id", nullable = false)
	private Purchase purchase;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@NotBlank
	@Size(max = 200)
	@Column(name = "game_title", nullable = false, length = 200)
	private String gameTitle;

	@NotNull
	@DecimalMin("0.01")
	@Digits(integer = 10, fraction = 2)
	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Positive
	@Column(nullable = false)
	private int quantity;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 10, fraction = 2)
	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	protected PurchaseItem() {
	}

	PurchaseItem(Purchase purchase, Game game, int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		this.purchase = Objects.requireNonNull(purchase, "purchase must not be null");
		this.game = Objects.requireNonNull(game, "game must not be null");
		this.gameTitle = game.getTitle();
		this.unitPrice = game.getPrice();
		this.quantity = quantity;
		this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	boolean references(Game candidate) {
		return game == candidate || game.getId() != null && game.getId().equals(candidate.getId());
	}

	public Long getId() {
		return id;
	}

	public Game getGame() {
		return game;
	}

	public String getGameTitle() {
		return gameTitle;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
