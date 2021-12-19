package info.skyblond.archivedag.arudaz.security

import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JWTAuthenticationEntryPoint : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        // just send 401 with the error to client who accessed a secured api
        // without proper token attached.
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.message)
    }
}
