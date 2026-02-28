package com.buyukozkan.boilerplate.service;

import com.buyukozkan.boilerplate.dto.AuthResponse;
import com.buyukozkan.boilerplate.dto.LoginRequest;
import com.buyukozkan.boilerplate.dto.RefreshTokenRequest;
import com.buyukozkan.boilerplate.dto.RegisterRequest;
import com.buyukozkan.boilerplate.entity.Role;
import com.buyukozkan.boilerplate.entity.User;
import com.buyukozkan.boilerplate.exception.DuplicateEmailException;
import com.buyukozkan.boilerplate.exception.InvalidTokenException;
import com.buyukozkan.boilerplate.repository.RoleRepository;
import com.buyukozkan.boilerplate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Role userRole = roleRepository.findByNameWithPermissions("USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Default role 'USER' not found. Run database migrations first."));

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                Set.of(userRole)
        );
        userRepository.save(user);

        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        );
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return new AuthResponse(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidTokenException();
        }

        String email = jwtService.extractUsername(refreshToken);
        List<SimpleGrantedAuthority> authorities = jwtService.extractAuthorities(refreshToken);

        return new AuthResponse(
                jwtService.generateAccessToken(email, authorities),
                jwtService.generateRefreshToken(email, authorities)
        );
    }
}
