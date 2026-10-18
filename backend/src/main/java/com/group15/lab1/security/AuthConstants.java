package com.group15.lab1.security;

/**
 * Shared constants for authentication token parsing and claims.
 */
public final class AuthConstants {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String TOKEN_SCHEME_BEARER = "Bearer";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_TYPE = "type";

    private AuthConstants() {
        // Utility class.
    }
}
