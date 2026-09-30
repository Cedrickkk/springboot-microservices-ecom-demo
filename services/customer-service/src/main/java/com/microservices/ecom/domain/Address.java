package com.microservices.ecom.domain;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Address {
    private String street;
    private String houseNumber;
    private String zipCode;
}
