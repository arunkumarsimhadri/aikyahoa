package com.aikya.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_name", nullable = false, length = 32)
    private String blockName;

    @Column(name = "unit_number", nullable = false, unique = true, length = 32)
    private String unitNumber;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber = 1;

    @Column(name = "area_sqft", nullable = false)
    private Integer areaSqft;

    @Column(name = "bhk_type", nullable = false, length = 16)
    private String bhkType = "3BHK";

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Column(name = "owner_phone", length = 20)
    private String ownerPhone;

    @Column(name = "tenant_user_id")
    private Long tenantUserId;

    @Column(name = "tenant_name", length = 150)
    private String tenantName;

    @Column(name = "tenant_phone", length = 20)
    private String tenantPhone;

    @Column(name = "occupancy_status", nullable = false, length = 32)
    private String occupancyStatus = "OWNER_OCCUPIED";

    @Column(name = "intercom_number", length = 16)
    private String intercomNumber;

    @Column(name = "parking_slots", length = 64)
    private String parkingSlots;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Unit() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public Integer getAreaSqft() {
        return areaSqft;
    }

    public void setAreaSqft(Integer areaSqft) {
        this.areaSqft = areaSqft;
    }

    public String getBhkType() {
        return bhkType;
    }

    public void setBhkType(String bhkType) {
        this.bhkType = bhkType;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public Long getTenantUserId() {
        return tenantUserId;
    }

    public void setTenantUserId(Long tenantUserId) {
        this.tenantUserId = tenantUserId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantPhone() {
        return tenantPhone;
    }

    public void setTenantPhone(String tenantPhone) {
        this.tenantPhone = tenantPhone;
    }

    public String getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(String occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public String getIntercomNumber() {
        return intercomNumber;
    }

    public void setIntercomNumber(String intercomNumber) {
        this.intercomNumber = intercomNumber;
    }

    public String getParkingSlots() {
        return parkingSlots;
    }

    public void setParkingSlots(String parkingSlots) {
        this.parkingSlots = parkingSlots;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
