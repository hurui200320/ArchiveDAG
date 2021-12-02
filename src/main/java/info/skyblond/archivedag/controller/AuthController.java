package info.skyblond.archivedag.controller;

import info.skyblond.archivedag.model.JWTAuthResponse;
import info.skyblond.archivedag.model.JwtAuthRequest;
import info.skyblond.archivedag.security.JwtTokenManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/public/auth")
public class AuthController {
    private final JwtTokenManager jwtTokenManager;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthController(JwtTokenManager jwtTokenManager, AuthenticationManager authenticationManager, UserDetailsService userDetailsService) {
        this.jwtTokenManager = jwtTokenManager;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }


    @PostMapping
    public JWTAuthResponse signJwtToken(
            @RequestBody JwtAuthRequest request
    ) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword()
        ));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtTokenManager.generateToken(userDetails);
        return new JWTAuthResponse(token);
    }
}
