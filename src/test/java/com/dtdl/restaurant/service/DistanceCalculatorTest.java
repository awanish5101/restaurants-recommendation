package com.dtdl.restaurant.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceCalculatorTest {

    @Test
    void samePointIsZero() {
        assertThat(DistanceCalculator.haversine(40.7580, -73.9855, 40.7580, -73.9855))
                .isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void nycToLaIsAboutFourThousandKm() {
        // Times Square -> Downtown LA is ~3936 km
        double km = DistanceCalculator.haversine(40.7580, -73.9855, 34.0522, -118.2437);
        assertThat(km).isBetween(3900.0, 3980.0);
    }

    @Test
    void shortHopIsSmall() {
        // ~1.1 km north
        double km = DistanceCalculator.haversine(40.7580, -73.9855, 40.7680, -73.9855);
        assertThat(km).isBetween(1.0, 1.2);
    }
}
