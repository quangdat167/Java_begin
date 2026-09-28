package vn.dangquangdat.javabegin.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private final JwtEncoder jwtEncoder;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final Map<String, RefreshSession> refreshSessions = new ConcurrentHashMap<>();

    public TokenService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.access-ttl}") Duration accessTtl,
            @Value("${app.jwt.refresh-ttl}") Duration refreshTtl
    ) {
        this.jwtEncoder = jwtEncoder;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public TokenPair issue(DemoUserService.UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("softaibox-java-lab")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.email())
                .claim("role", user.role().name())
                .claim("userId", user.id())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        String refreshToken = UUID.randomUUID().toString();
        refreshSessions.put(refreshToken, new RefreshSession(user.email(), now.plus(refreshTtl)));
        return new TokenPair(accessToken, refreshToken, expiresAt, user.email());
    }

    public TokenPair rotate(String refreshToken, DemoUserService users) {
        RefreshSession session = refreshSessions.remove(refreshToken);
        if (session == null || session.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }
        return issue(users.findByEmail(session.email()));
    }

    private record RefreshSession(String email, Instant expiresAt) {
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            String subject
    ) {
    }
}
