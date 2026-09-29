package com.rentalert.core_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@Table(name = "scheduler_settings")
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerSettings extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_name", nullable = false)
    private com.rentalert.dto.listing.ScheduleProvider providerName;

    @Column(name = "scheduler_enabled", nullable = false)
    private Boolean schedulerEnabled = false;
    
    @Column(name = "notifications_enabled", nullable = false)
    private Boolean notificationsEnabled = false;
    
    @Column(name = "fixed_rate_ms", nullable = false)
    private Long fixedRateMs = 60000L;
    
    @Column(name = "page_size", nullable = false)
    private Integer pageSize = 50;
    
    @Column(name = "price_from")
    private Integer priceFrom;
    
    @Column(name = "price_to")
    private Integer priceTo;
    
    @Column(name = "price_currency", length = 10)
    private String priceCurrency = "BYN";
    
    @Column(name = "region_id", nullable = false, length = 255)
    private String regionId = "0";
    
    @Column(name = "sort_by", nullable = false, length = 50)
    private String sortBy = "lst.d";
    
    @Column(name = "sort_order", nullable = false, length = 10)
    private String sortOrder = "DESC";
    
    @Column(name = "category_id", nullable = false)
    private Integer categoryId = 2;

    /** Категория внешнего провайдера для выборки объявлений типа "комната" (ROOM). */
    @Column(name = "room_category_id")
    private Integer roomCategoryId;
}