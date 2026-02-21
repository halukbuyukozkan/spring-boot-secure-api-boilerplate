package com.buyukozkan.boilerplate.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
