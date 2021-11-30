package info.skyblond.archivedag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
@Configuration
@EnableWebSecurity
// This annotation can be used on both GRpcSecurityConfig and this config.
// But can only occur once. I'd prefer here.
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/public").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .and()
                .logout().permitAll();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return username -> {
            log.info("Get user details for {}", username);
            if (username.startsWith("user_")) {
                return new User(username, this.passwordEncoder().encode("654321"),
                        AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
            }
            if (username.startsWith("admin_")) {
                return new User(username, this.passwordEncoder().encode("123456"),
                        AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ADMIN"));
            }
            throw new UsernameNotFoundException("User not found!");
        };
    }
}
