package com.networknt.token;

import com.networknt.cache.CacheManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CacheTokenClientTest {
    @Test
    void shouldReturnCachedTokenWithoutCallingDelegate() {
        TokenClient delegate = mock(TokenClient.class);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.get("token_vault_cache", "1:ssn-value")).thenReturn("tok_123");

        CacheTokenClient client = new CacheTokenClient(delegate, cacheManager);
        String token = client.tokenize("ssn-value", 1);

        assertEquals("tok_123", token);
        verify(delegate, never()).tokenize(anyString(), anyInt());
    }

    @Test
    void shouldUseDelegateWhenNoCacheManagerConfigured() {
        TokenClient delegate = mock(TokenClient.class);
        when(delegate.tokenize("ssn-value", 1)).thenReturn("tok_456");

        CacheTokenClient client = new CacheTokenClient(delegate, null);
        String token = client.tokenize("ssn-value", 1);

        assertEquals("tok_456", token);
        verify(delegate).tokenize("ssn-value", 1);
    }
}
