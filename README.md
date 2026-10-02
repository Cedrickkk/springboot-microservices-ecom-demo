# Spring Boot Microservices Ecommerce Demo

A simple microservices application for experimenting and demonstrating the microservices architecture using Spring Boot.

## Shared API responses

`shared/common` is a regular JAR containing response envelopes, validation error
details, and `ApiResponseUtil` in `com.microservices.common.response`. Each service
keeps its own exception handler, mapper, and domain exceptions. The library has no
Spring components and requires no component scanning or imports to register beans.

Add this dependency to services that need the shared response format:

```xml
<dependency>
    <groupId>com.microservices</groupId>
    <artifactId>common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

From the repository root, build customer-service and its shared dependency:

```sh
mvn -pl services/customer-service -am verify
```

Install the library locally before building or running a service independently:

```sh
mvn -pl shared/common install
```

Rebuild consuming services after changing shared code. For new services, add their
directory to the root POM's modules. `ApiResponseUtil.success(data, message)` uses
HTTP 200; use `success(HttpStatus.CREATED, data, message)` for a different status
and set the same HTTP status on the controller's `ResponseEntity`.
