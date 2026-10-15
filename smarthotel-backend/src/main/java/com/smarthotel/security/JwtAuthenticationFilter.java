package com.smarthotel.security;

import com.smarthotel.entity.SysUser;
import com.smarthotel.mapper.SysUserMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            String token =
                    authorizationHeader.substring(7);

            if (jwtTokenProvider.validateToken(token)) {

                Claims claims =
                        jwtTokenProvider.parseToken(token);

                String username =
                        claims.getSubject();

                String role =
                        claims.get("role", String.class);

                Long userId =
                        claims.get("userId", Long.class);

                SysUser account = userId == null ? null : sysUserMapper.selectById(userId);
                if (account == null || !"ACTIVE".equals(account.getStatus())
                        || !username.equals(account.getUsername())
                        || !jwtTokenProvider.matchesCredentialVersion(claims, account.getPassword())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                List<SimpleGrantedAuthority> authorities =
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                userId,
                                authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
