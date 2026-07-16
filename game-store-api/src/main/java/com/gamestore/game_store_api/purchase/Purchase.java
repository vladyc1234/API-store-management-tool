package com.gamestore.game_store_api.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.gamestore.game_store_api.game.Game;
import com.gamestore.game_store_api.user.Role;
import com.gamestore.game_store_api.user.UserAccount;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "purchases")
public class Purchase {

	public static final String V1_CURRENCY = "EUR";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "buyer_id", nullable = false)
	private UserAccount buyer;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PurchaseStatus status = PurchaseStatus.PENDING;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 10, fraction = 2)
	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

	@NotNull
	@Column(nullable = false, length = 3)
	private String currency = V1_CURRENCY;

	@OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private final List<PurchaseItem> items = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Purchase() {
	}

	public Purchase(UserAccount buyer) {
		this.buyer = Objects.requireNonNull(buyer, "buyer must not be null");
		if (buyer.getRole() != Role.BUYER) {
			throw new IllegalArgumentException("purchases can only belong to buyers");
		}
	}

	public void addItem(Game game, int quantity) {
		ensurePending();
		Objects.requireNonNull(game, "game must not be null");
		if (!game.isActive()) {
			throw new IllegalStateException("inactive games cannot be purchased");
		}
		if (items.stream().anyMatch(item -> item.references(game))) {
			throw new IllegalArgumentException("game is already present in this purchase");
		}

		var item = new PurchaseItem(this, game, quantity);
		items.add(item);
		totalAmount = totalAmount.add(item.getLineTotal());
	}

	public void complete() {
		ensurePending();
		if (items.isEmpty()) {
			throw new IllegalStateException("an empty purchase cannot be completed");
		}
		status = PurchaseStatus.COMPLETED;
	}

	public void cancel() {
		ensurePending();
		status = PurchaseStatus.CANCELLED;
	}

	public Long getId() {
		return id;
	}

	public UserAccount getBuyer() {
		return buyer;
	}

	public PurchaseStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public List<PurchaseItem> getItems() {
		return List.copyOf(items);
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	private void ensurePending() {
		if (status != PurchaseStatus.PENDING) {
			throw new IllegalStateException("only pending purchases can be changed");
		}
	}
}
