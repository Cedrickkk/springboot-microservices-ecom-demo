package com.microservices.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseUtilTests {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void successKeepsExplicitStatusAndWireFormat() {
        var response = ApiResponseUtil.success(HttpStatus.CREATED, 7, "Created");
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertEquals(201, json.path("code").asInt());
        assertEquals("Created", json.path("status").asText());
        assertEquals(7, json.path("data").asInt());
        assertFalse(json.path("meta").path("timestamp").isMissingNode());
        assertDoesNotThrow(() -> UUID.fromString(response.getMeta().getRequestId()));
    }

    @Test
    void errorsKeepDetailsAndIndependentMetadata() {
        var first = ApiResponseUtil.<List<FieldErrorResponseDetail>>error(HttpStatus.BAD_REQUEST, "Invalid");
        first.setErrors(List.of(new FieldErrorResponseDetail("reference", "Required")));
        var second = ApiResponseUtil.error(HttpStatus.NOT_FOUND, "Missing");
        var json = mapper.readTree(mapper.writeValueAsString(first));
        assertEquals(400, json.path("code").asInt());
        assertEquals("reference", json.path("errors").get(0).path("field").asText());
        assertNotEquals(first.getMeta().getRequestId(), second.getMeta().getRequestId());
    }
}
