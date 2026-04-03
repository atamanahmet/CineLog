package com.atamanahmet.cinelog.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.atamanahmet.cinelog.domain.POJO.Cast;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CastJsonConverter implements AttributeConverter<Cast, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Cast cast) {
        if (cast == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cast);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting Cast to JSON", e);
        }
    }

    @Override
    public Cast convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, Cast.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error reading Cast from JSON", e);
        }
    }
}
