package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    private final EmbeddingService service =
            new EmbeddingService(Mockito.mock(org.springframework.ai.embedding.EmbeddingModel.class));

    @Test
    void vectorLiteralIsPgVectorFormat() {
        assertThat(EmbeddingService.toVectorLiteral(new float[]{0.1f, -0.2f, 0.3f}))
                .startsWith("[").endsWith("]").contains(",")
                .isEqualTo("[0.1,-0.2,0.3]");
    }

    @Test
    void documentIncludesNameCuisinesLocationAndDescription() {
        RestaurantEntity r = RestaurantEntity.builder()
                .id(1L).name("Sushi Zen")
                .cuisines(List.of("Sushi", "Japanese"))
                .city("New York").state("NY")
                .features(List.of("Outdoor Dining"))
                .description("Fresh omakase.")
                .latitude(40.0).longitude(-73.0)
                .build();
        String doc = service.toDocument(r);
        assertThat(doc)
                .contains("Sushi Zen")
                .contains("Sushi, Japanese")
                .contains("New York, NY")
                .contains("Outdoor Dining")
                .contains("Fresh omakase.");
    }
}
