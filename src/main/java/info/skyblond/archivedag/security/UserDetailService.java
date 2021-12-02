package info.skyblond.archivedag.security;

import info.skyblond.archivedag.model.entity.UserEntity;
import info.skyblond.archivedag.repo.UserRepository;
import info.skyblond.archivedag.repo.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public UserDetailService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = this.userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found!");
        }

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        this.userRoleRepository.findAllByUsername(username)
                .stream().map(role -> new SimpleGrantedAuthority(role.getRole()))
                .forEach(grantedAuthorities::add);

        return new User(user.getUsername(), user.getPassword(),
                user.getStatus() == UserEntity.UserStatus.ENABLED,
                true, true,
                user.getStatus() != UserEntity.UserStatus.LOCKED,
                grantedAuthorities);
    }
}
