package org.example.exception.api;

import org.springframework.http.HttpStatus;

public class GeoIpLookupException extends ApiException {

    public GeoIpLookupException(String ipAddress) {
        super("GeoIpLookupFailed", "Failed to resolve country for IP: " + ipAddress);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
