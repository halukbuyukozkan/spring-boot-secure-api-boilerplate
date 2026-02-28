package com.buyukozkan.boilerplate.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT Secret cannot be null or empty defined in application.yml");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT Secret must be at least 256 bits (32 bytes) long!");
            }
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT Signing Key initialized successfully.");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT Secret must be a valid Base64 encoded string!", e);
        }
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), toRoleStrings(userDetails.getAuthorities()), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), toRoleStrings(userDetails.getAuthorities()), refreshTokenExpiration);
    }

    public String generateAccessToken(String email, List<? extends GrantedAuthority> authorities) {
        return buildToken(email, toRoleStrings(authorities), accessTokenExpiration);
    }

    public String generateRefreshToken(String email, List<? extends GrantedAuthority> authorities) {
        return buildToken(email, toRoleStrings(authorities), refreshTokenExpiration);
    }

    private String buildToken(String email, List<String> roles, long expiration) {
        return Jwts.builder()
                .subject(email)
                .claim("authorities", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private List<String> toRoleStrings(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenValid(String token) {
        final Claims claims = extractAllClaims(token);
        final String subject = claims.getSubject();
        final Date expiration = claims.getExpiration();
        return subject != null && !subject.isBlank() && expiration != null && expiration.after(new Date());
    }

    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        Object rolesClaim = extractClaim(token, claims -> claims.get("authorities"));
        if (!(rolesClaim instanceof List<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
