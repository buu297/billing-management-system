package com.buu297.billing.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BillItem {
    private String itemId;
    private String billId;
    private ItemType itemType;
    private String description;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public BillItem() {}

    public BillItem(String itemId, String billId, ItemType itemType, String description, BigDecimal amount) {
        this.itemId = itemId;
        this.billId = billId;
        this.itemType = itemType;
        this.description = description;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
