package com.aikya.platform.entity;

import com.aikya.common.BillingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "associations", schema = "public")
public class Association {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "name_te", length = 200)
    private String nameTe;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "schema_name", nullable = false, unique = true, length = 64)
    private String schemaName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 100)
    private String city = "Hyderabad";

    @Column(nullable = false, length = 100)
    private String state = "Telangana";

    @Column(nullable = false, length = 16)
    private String pincode = "500081";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", nullable = false, length = 32)
    private BillingType billingType = BillingType.AREA_BASED;

    @Column(name = "rate_per_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerSqft = new BigDecimal("3.50");

    @Column(name = "flat_maintenance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal flatMaintenanceAmount = new BigDecimal("3500.00");

    @Column(name = "sinking_fund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal sinkingFundAmount = new BigDecimal("500.00");

    @Column(name = "water_charge_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal waterChargePerUnit = new BigDecimal("450.00");

    @Column(name = "late_fee_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal lateFeePercentage = new BigDecimal("2.00");

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Association() {
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameTe() {
        return nameTe;
    }

    public void setNameTe(String nameTe) {
        this.nameTe = nameTe;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public BillingType getBillingType() {
        return billingType;
    }

    public void setBillingType(BillingType billingType) {
        this.billingType = billingType;
    }

    public BigDecimal getRatePerSqft() {
        return ratePerSqft;
    }

    public void setRatePerSqft(BigDecimal ratePerSqft) {
        this.ratePerSqft = ratePerSqft;
    }

    public BigDecimal getFlatMaintenanceAmount() {
        return flatMaintenanceAmount;
    }

    public void setFlatMaintenanceAmount(BigDecimal flatMaintenanceAmount) {
        this.flatMaintenanceAmount = flatMaintenanceAmount;
    }

    public BigDecimal getSinkingFundAmount() {
        return sinkingFundAmount;
    }

    public void setSinkingFundAmount(BigDecimal sinkingFundAmount) {
        this.sinkingFundAmount = sinkingFundAmount;
    }

    public BigDecimal getWaterChargePerUnit() {
        return waterChargePerUnit;
    }

    public void setWaterChargePerUnit(BigDecimal waterChargePerUnit) {
        this.waterChargePerUnit = waterChargePerUnit;
    }

    public BigDecimal getLateFeePercentage() {
        return lateFeePercentage;
    }

    public void setLateFeePercentage(BigDecimal lateFeePercentage) {
        this.lateFeePercentage = lateFeePercentage;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
