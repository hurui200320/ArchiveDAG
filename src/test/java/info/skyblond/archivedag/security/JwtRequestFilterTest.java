package info.skyblond.archivedag.security;

import com.google.gson.Gson;
import info.skyblond.archivedag.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class JwtRequestFilterTest {

    @Autowired
    JwtTokenManager tokenManager;
    @Autowired
    AccountStatusUserDetailsChecker userDetailsChecker;
    @Autowired
    Gson gson;

    @MockBean
    UserDetailsService userDetailsService;

    JwtRequestFilter requestFilter;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        requestFilter = new JwtRequestFilter(tokenManager, userDetailsService, userDetailsChecker, gson);
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    private UserDetails mockUser() {
        return new User("test_user", "", List.of());
    }

    private Authentication mockAuth(UserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void testAlreadyAuthed() {
        Mockito.when(userDetailsService.loadUserByUsername("test_user"))
                .thenThrow(new IllegalStateException("Shouldn't be here!"));
        // already authed
        SecurityContextHolder.getContext().setAuthentication(mockAuth(mockUser()));
        assertDoesNotThrow(() -> this.requestFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testNoHeader() {
        Mockito.when(userDetailsService.loadUserByUsername("test_user"))
                .thenThrow(new IllegalStateException("Shouldn't be here!"));
        SecurityContextHolder.getContext().setAuthentication(null);
        // No header
        request.removeHeader(HttpHeaders.AUTHORIZATION);
        assertDoesNotThrow(() -> this.requestFilter.doFilterInternal(request, response, filterChain));
        filterChain.reset();
        // Bad header
        SecurityContextHolder.getContext().setAuthentication(null);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Something");
        assertDoesNotThrow(() -> this.requestFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testCorrectToken() {
        UserDetails userDetails = mockUser();
        Mockito.when(userDetailsService.loadUserByUsername("test_user"))
                .thenReturn(userDetails);
        String token = this.tokenManager.generateToken(userDetails);
        SecurityContextHolder.getContext().setAuthentication(null);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        assertNull(SecurityUtils.getCurrentAuthentication());
        assertDoesNotThrow(() -> this.requestFilter.doFilterInternal(request, response, filterChain));
        assertEquals("test_user", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void testInvalidToken() {
        Mockito.when(userDetailsService.loadUserByUsername("test_user"))
                .thenThrow(new IllegalStateException("Shouldn't be here!"));

        SecurityContextHolder.getContext().setAuthentication(null);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer Something but not JWT");
        assertDoesNotThrow(() -> this.requestFilter.doFilterInternal(request, response, filterChain));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDisabledUser() {
        UserDetails userDetails = new User("test_user", "",
                false, true, true,
                true, List.of());
        assertFalse(userDetails.isEnabled());
        Mockito.when(userDetailsService.loadUserByUsername("test_user"))
                .thenReturn(userDetails);
        String token = this.tokenManager.generateToken(userDetails);
        SecurityContextHolder.getContext().setAuthentication(null);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        assertDoesNotThrow(() -> this.requestFilter.doFilterInternal(request, response, filterChain));
        assertNull(SecurityUtils.getCurrentAuthentication());
    }
}
