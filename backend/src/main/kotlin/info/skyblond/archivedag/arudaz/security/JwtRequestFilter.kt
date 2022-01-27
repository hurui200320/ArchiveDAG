package info.skyblond.archivedag.arudaz.security

import com.google.gson.Gson
import info.skyblond.archivedag.arudaz.model.ExceptionResponse.Companion.generateResp
import info.skyblond.archivedag.arudaz.service.JwtTokenService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AccountStatusException
import org.springframework.security.authentication.AccountStatusUserDetailsChecker
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtRequestFilter(
    private val jwtTokenService: JwtTokenService,
    private val userDetailsService: UserDetailsService,
    private val userDetailsChecker: AccountStatusUserDetailsChecker,
    private val gson: Gson
) : OncePerRequestFilter() {

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (SecurityContextHolder.getContext().authentication != null) {
            // skip if we have already got the authentication
            filterChain.doFilter(request, response)
            return
        }
        // check header
        val jwtHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (jwtHeader == null || !jwtHeader.startsWith("Bearer ")) {
            // not satisfied, keep calling the filter chain
            filterChain.doFilter(request, response)
            return
        }
        // check token
        val token = jwtHeader.substring(7) // skip `Bearer `
        val claimedUsername = jwtTokenService.getUsernameFromToken(token)
        if (claimedUsername == null) {
            // No valid username found in JWT token, but might be other tokens.
            // keep calling the filters
            filterChain.doFilter(request, response)
            return
        }
        // Get user details and set the authentication
        val userDetails = userDetailsService.loadUserByUsername(claimedUsername)
        // check the account status
        try {
            userDetailsChecker.check(userDetails)
        } catch (e: AccountStatusException) {
            // Invalid account status, reject this request
            writeToResp(generateResp(HttpStatus.FORBIDDEN, request, e), response)
            return
        }
        // everything is ok, set authentication
        val authenticationToken = UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.authorities
        )
        authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authenticationToken
        // keep calling the filter chain
        filterChain.doFilter(request, response)
    }

    private fun writeToResp(resp: ResponseEntity<*>, response: HttpServletResponse) {
        response.status = resp.statusCodeValue
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(gson.toJson(resp.body))
    }
}
