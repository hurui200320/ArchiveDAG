package info.skyblond.archivedag.arudaz.config

import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arstue.entity.UserEntity
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

/**
 * Configuration for create default admin
 * */
@Configuration
class DefaultAdminConfig(
    private val userManagementService: UserManagementService
) {
    /**
     * If the admin user is not present, create and enable it.
     * The default password is `admin`.
     * */
    @PostConstruct
    fun createDefaultAdminAccount() {
        if (!userManagementService.userExists("admin")) {
            userManagementService.createUser("admin", "admin")
            userManagementService.changeStatus("admin", UserEntity.Status.ENABLED)
        }
        val adminAccount = userManagementService.queryUser("admin")
        // here we don't change the account status
        if (!adminAccount.roles.contains("ROLE_ADMIN")) {
            userManagementService.addRoleToUser("admin", "ROLE_ADMIN")
        }
        if (!adminAccount.roles.contains("ROLE_USER")) {
            userManagementService.addRoleToUser("admin", "ROLE_USER")
        }
    }
}
