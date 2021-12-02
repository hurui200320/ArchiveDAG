package info.skyblond.archivedag.security;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtTokenManager jwtTokenManager;
    private final UserDetailsService userDetailsService;

    public JwtRequestFilter(JwtTokenManager jwtTokenManager, UserDetailsService userDetailsService) {
        this.jwtTokenManager = jwtTokenManager;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            // skip if we have already got the authentication
            filterChain.doFilter(request, response);
            return;
        }
        // check header
        String jwtHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (jwtHeader == null || !jwtHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // check token
        String token = jwtHeader.substring(7); // skip `Bearer `
        String claimedUsername = this.jwtTokenManager.getUsernameFromToken(token);
        if (claimedUsername == null) {
            filterChain.doFilter(request, response);
            return;
        }
        // Get user details and set the authentication
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(claimedUsername);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        // keep calling the filter chain
        filterChain.doFilter(request, response);
    }
}
