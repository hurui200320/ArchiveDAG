package info.skyblond.archivedag.security;

import com.google.gson.Gson;
import info.skyblond.archivedag.model.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
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
    private final AccountStatusUserDetailsChecker userDetailsChecker;
    private final Gson gson;

    public JwtRequestFilter(JwtTokenManager jwtTokenManager, UserDetailsService userDetailsService, AccountStatusUserDetailsChecker userDetailsChecker, Gson gson) {
        this.jwtTokenManager = jwtTokenManager;
        this.userDetailsService = userDetailsService;
        this.userDetailsChecker = userDetailsChecker;
        this.gson = gson;
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
            // No valid username found in JWT token, but might be other tokens.
            // keep calling the filters
            filterChain.doFilter(request, response);
            return;
        }
        // Get user details and set the authentication
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(claimedUsername);
        // check the account status
        try {
            this.userDetailsChecker.check(userDetails);
        } catch (AccountStatusException e) {
            // If something wrong with the status, stop handle this request
            writeToResp(ExceptionResponse.generateResp(HttpStatus.FORBIDDEN, request, e), response);
            return;
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        // keep calling the filter chain
        filterChain.doFilter(request, response);
    }

    private void writeToResp(ResponseEntity<?> resp, HttpServletResponse response) throws IOException {
        response.setStatus(resp.getStatusCodeValue());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(gson.toJson(resp.getBody()));
    }
}
