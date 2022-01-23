package info.skyblond.archivedag.arudaz.security

import info.skyblond.archivedag.arstue.entity.UserEntity
import info.skyblond.archivedag.arstue.repo.UserRepository
import info.skyblond.archivedag.arstue.repo.UserRoleRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username) ?: throw UsernameNotFoundException("User not found!")
        val grantedAuthorities: MutableList<GrantedAuthority> = ArrayList()
        userRoleRepository.findAllByUsername(username)
            .map { SimpleGrantedAuthority(it.role) }
            .forEach { grantedAuthorities.add(it) }
        return User(
            user.username, user.password,
            user.status === UserEntity.Status.ENABLED,
            true, true,
            user.status !== UserEntity.Status.LOCKED,
            grantedAuthorities
        )
    }
}
