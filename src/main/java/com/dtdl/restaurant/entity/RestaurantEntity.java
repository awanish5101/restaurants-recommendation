package com.dtdl.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * A restaurant in the catalog, backed by the {@code restaurants} Postgres table.
 * {@code cuisines} and {@code features} are stored as JSONB arrays. The embedding
 * column is mapped in a later phase once the RAG layer is wired.
 */
@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> cuisines = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> features = new ArrayList<>();

    private Integer priceRange;
    private Double overallRating;
    private Integer numberOfRatings;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String neighborhood;
    private String city;
    private String state;
    private String phone;
    private String websiteUrl;

    /** Distance (km) from the query point; set per-request, never persisted. */
    @Transient
    private Double distanceKm;
}
