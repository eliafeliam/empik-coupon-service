package org.example.service.geo;

import org.example.exception.api.GeoIpLookupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class GeoIpServiceTest {

    @MockBean
    @SuppressWarnings("unused")
    private RestTemplate restTemplate;

    @Autowired
    @SuppressWarnings("unused")
    private GeoIpService geoIpService;

    private Cache cache;

    @Autowired
    @SuppressWarnings("unused")
    private CacheManager cacheManager;

    @BeforeEach
    @SuppressWarnings("all")
    //safe to use @SuppressWarnings("all") since the cache will not be null,
        // the library implementation is protected from returning null
    void setUp() {
        cache = cacheManager.getCache("geoIpCache");
        cache.clear();
    }

    @Test
    void getCountry_ValidIp_ReturnsCountryCode() {
        String ipAddress = "8.8.8.8";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("countryCode", "US");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class)))
                .thenReturn(responseEntity);

        String countryCode = geoIpService.getCountry(ipAddress);

        assertThat(countryCode).isEqualTo("US");
        verify(restTemplate, times(1)).getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class));
        assertThat(cache.get(ipAddress, String.class)).isEqualTo("US");
    }

    @Test
    void getCountry_CachedResult_ReturnsFromCacheWithoutApiCall() {
        String ipAddress = "8.8.8.8";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("countryCode", "US");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class)))
                .thenReturn(responseEntity);

        String countryCode1 = geoIpService.getCountry(ipAddress);
        String countryCode2 = geoIpService.getCountry(ipAddress);

        assertThat(countryCode1).isEqualTo("US");
        assertThat(countryCode2).isEqualTo("US");
        verify(restTemplate, times(1)).getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class));
        assertThat(cache.get(ipAddress, String.class)).isEqualTo("US");
    }

    @Test
    void getCountry_ApiThrowsException_ThrowsGeoIpLookupException() {
        String ipAddress = "8.8.8.8";
        when(restTemplate.getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class)))
                .thenThrow(new RestClientException("API error"));

        assertThatThrownBy(() -> geoIpService.getCountry(ipAddress))
                .isInstanceOf(GeoIpLookupException.class)
                .hasMessageContaining(ipAddress);
        verify(restTemplate, times(1)).getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class));
        assertThat(cache.get(ipAddress)).isNull();
    }

    @Test
    void getCountry_NullResponseBody_ThrowsGeoIpLookupException() {
        String ipAddress = "8.8.8.8";
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class)))
                .thenReturn(responseEntity);

        assertThatThrownBy(() -> geoIpService.getCountry(ipAddress))
                .isInstanceOf(GeoIpLookupException.class)
                .hasMessageContaining(ipAddress);
        verify(restTemplate, times(1)).getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class));
        assertThat(cache.get(ipAddress)).isNull();
    }

    @Test
    void getCountry_MissingCountryCode_ThrowsGeoIpLookupException() {
        String ipAddress = "8.8.8.8";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("otherField", "value");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class)))
                .thenReturn(responseEntity);

        assertThatThrownBy(() -> geoIpService.getCountry(ipAddress))
                .isInstanceOf(GeoIpLookupException.class)
                .hasMessageContaining(ipAddress);
        verify(restTemplate, times(1)).getForEntity(eq("http://dummy/8.8.8.8?fields=countryCode"), eq(Map.class));
        assertThat(cache.get(ipAddress)).isNull();
    }

    @Test
    void getCountry_EmptyIpAddress_ThrowsGeoIpLookupException() {
        String ipAddress = "";
        when(restTemplate.getForEntity(eq("http://dummy/?fields=countryCode"), eq(Map.class)))
                .thenThrow(new RestClientException("Invalid URL"));

        assertThatThrownBy(() -> geoIpService.getCountry(ipAddress))
                .isInstanceOf(GeoIpLookupException.class)
                .hasMessageContaining(ipAddress);
        verify(restTemplate, times(1)).getForEntity(eq("http://dummy/?fields=countryCode"), eq(Map.class));
        assertThat(cache.get(ipAddress)).isNull();
    }

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @SuppressWarnings("unused")
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("geoIpCache");
        }

        @Bean
        GeoIpService geoIpService(RestTemplate restTemplate) {
            return new GeoIpService(restTemplate, "http://dummy/", "countryCode");
        }
    }
}