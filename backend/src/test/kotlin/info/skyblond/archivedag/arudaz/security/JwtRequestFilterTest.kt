package info.skyblond.archivedag.arudaz.security

import com.google.gson.Gson
import info.skyblond.archivedag.arudaz.service.JwtTokenService
import info.skyblond.archivedag.arudaz.utils.getCurrentAuthentication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AccountStatusUserDetailsChecker
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
internal class JwtRequestFilterTest {

    @Autowired
    lateinit var tokenManager: JwtTokenService

    @Autowired
    lateinit var userDetailsChecker: AccountStatusUserDetailsChecker

    @Autowired
    lateinit var gson: Gson

    @MockBean
    lateinit var userDetailsService: UserDetailsService

    lateinit var requestFilter: JwtRequestFilter
    lateinit var request: MockHttpServletRequest
    lateinit var response: MockHttpServletResponse
    lateinit var filterChain: MockFilterChain

    @BeforeEach
    fun setUp() {
        request = MockHttpServletRequest()
        requestFilter = JwtRequestFilter(tokenManager, userDetailsService, userDetailsChecker, gson)
        response = MockHttpServletResponse()
        filterChain = MockFilterChain()
    }

    @Test
    fun testDisabledUser() {
        val userDetails: UserDetails = User(
            "test_user", "",
            false, true, true,
            true, listOf()
        )
        assertFalse(userDetails.isEnabled)
        Mockito.`when`(userDetailsService.loadUserByUsername("test_user"))
            .thenReturn(userDetails)
        val token = tokenManager.generateToken(userDetails)
        SecurityContextHolder.getContext().authentication = null
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        assertDoesNotThrow {
            requestFilter.doFilterInternal(request, response, filterChain)
        }
        assertNull(getCurrentAuthentication())
    }

    @Test
    fun testInvalidToken() {
        Mockito.`when`(userDetailsService.loadUserByUsername("test_user"))
            .thenThrow(IllegalStateException("Shouldn't be here!"))
        SecurityContextHolder.getContext().authentication = null
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer Something but not JWT")
        assertDoesNotThrow {
            requestFilter.doFilterInternal(request, response, filterChain)
        }
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun testCorrectToken() {
        val userDetails = mockUser()
        Mockito.`when`(userDetailsService.loadUserByUsername("test_user"))
            .thenReturn(userDetails)
        val token = tokenManager.generateToken(userDetails)
        SecurityContextHolder.getContext().authentication = null
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        assertNull(getCurrentAuthentication())
        assertDoesNotThrow {
            requestFilter.doFilterInternal(request, response, filterChain)
        }
        assertEquals("test_user", SecurityContextHolder.getContext().authentication.name)
    }

    @Test
    fun testHeader() {
        Mockito.`when`(userDetailsService.loadUserByUsername("test_user"))
            .thenThrow(IllegalStateException("Shouldn't be here!"))
        SecurityContextHolder.getContext().authentication = null
        // No header
        request.removeHeader(HttpHeaders.AUTHORIZATION)
        assertDoesNotThrow {
            requestFilter.doFilterInternal(request, response, filterChain)
        }
        filterChain.reset()
        // Bad header
        SecurityContextHolder.getContext().authentication = null
        request.addHeader(HttpHeaders.AUTHORIZATION, "Something")
        assertDoesNotThrow {
            requestFilter.doFilterInternal(request, response, filterChain)
        }
    }

    @Test
    fun testAlreadyAuthed() {
        Mockito.`when`(userDetailsService.loadUserByUsername("test_user"))
            .thenThrow(IllegalStateException("Shouldn't be here!"))
        // already authed
        SecurityContextHolder.getContext().authentication = mockAuth(mockUser())
        assertDoesNotThrow {
            requestFilter.doFilterInternal(request, response, filterChain)
        }
    }

    private fun mockUser(): UserDetails {
        return User("test_user", "", listOf())
    }

    private fun mockAuth(userDetails: UserDetails): Authentication {
        return UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.authorities
        )
    }
}
