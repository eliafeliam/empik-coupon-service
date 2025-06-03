package org.example.service.geo;

import org.example.exception.api.GeoIpLookupException;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Service
@CacheConfig(cacheNames = "geoIpCache")
public class GeoIpService implements CountryResolver {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String fields;

    public GeoIpService(
            RestTemplate restTemplate,
            @org.springframework.beans.factory.annotation.Value("${geoip.base-url}") String baseUrl,
            @org.springframework.beans.factory.annotation.Value("${geoip.fields}") String fields
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.fields = fields;
    }

    @Override
    @Cacheable
    public String getCountry(String ipAddress) {
        String url = baseUrl + ipAddress + "?fields=" + fields;
        ResponseEntity<Map> response;
        try {
            response = restTemplate.getForEntity(url, Map.class);
        } catch (Exception e) {
            throw new GeoIpLookupException(ipAddress);
        }
        Map<?, ?> body = response.getBody();
        if (body == null || body.get("countryCode") == null) {
            throw new GeoIpLookupException(ipAddress);
        }
        return body.get("countryCode").toString();
    }
}