package io.github.mighil31.registrar.security;

import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Who is calling: their user id and the permissions from their token. */
public record Caller(ObjectId id, Set<String> permissions) {

    public static Caller of(JwtAuthenticationToken token) {
        return new Caller(
                new ObjectId(token.getToken().getSubject()),
                token.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    public boolean can(String permission) {
        return permissions.contains(permission);
    }
}
