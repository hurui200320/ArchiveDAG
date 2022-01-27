package info.skyblond.archivedag.arudaz.controller.http

import info.skyblond.archivedag.arudaz.model.JWTAuthResponse
import info.skyblond.archivedag.arudaz.model.JwtAuthRequest
import info.skyblond.archivedag.arudaz.service.JwtTokenService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/public/auth")
class AuthController(
    private val jwtTokenService: JwtTokenService,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService
) {
    @PostMapping
    fun signJwtToken(
        @RequestBody request: JwtAuthRequest
    ): JWTAuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                request.username, request.password
            )
        )
        val userDetails = userDetailsService.loadUserByUsername(request.username)
        val token = jwtTokenService.generateToken(userDetails)
        return JWTAuthResponse(token)
    }
}
