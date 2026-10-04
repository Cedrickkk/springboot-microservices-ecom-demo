package com.microservices.ecom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerResponse(String id, String firstname, String lastname, String email) {
}
