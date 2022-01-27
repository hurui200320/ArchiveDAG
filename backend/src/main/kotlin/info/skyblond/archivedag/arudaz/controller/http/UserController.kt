package info.skyblond.archivedag.arudaz.controller.http

import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arstue.model.UserDetailModel
import info.skyblond.archivedag.arudaz.model.CreateUserRequest
import info.skyblond.archivedag.arudaz.model.UserChangePasswordRequest
import info.skyblond.archivedag.arudaz.model.UserChangeStatusRequest
import info.skyblond.archivedag.arudaz.model.UserRoleRequest
import info.skyblond.archivedag.arudaz.utils.checkCurrentUserIsAdmin
import info.skyblond.archivedag.arudaz.utils.getCurrentUsername
import info.skyblond.archivedag.arudaz.utils.requireSortPropertiesInRange
import info.skyblond.archivedag.arudaz.utils.requireValidateRole
import info.skyblond.archivedag.commons.PermissionDeniedException
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserManagementService
) {
    @GetMapping("/whoami")
    fun getUsername(): String {
        return getCurrentUsername()
    }

    @GetMapping("/listUsername")
    @PreAuthorize("hasRole('USER')")
    fun listUsername(
        @RequestParam(name = "keyword", defaultValue = "") keyword: String,
        pageable: Pageable
    ): List<String> {
        requireSortPropertiesInRange(pageable, listOf("username"))
        return userService.listUsername(keyword, pageable)
    }

    @GetMapping("/queryUser")
    @PreAuthorize("hasRole('USER')")
    fun queryUser(
        @RequestParam(value = "username", required = false) username: String?
    ): UserDetailModel {
        return userService.queryUser(username ?: getCurrentUsername())
    }

    @GetMapping("/listUserRoles")
    @PreAuthorize("hasRole('USER')")
    fun listUserRoles(
        @RequestParam(value = "username", required = false) username: String?,
        pageable: Pageable
    ): List<String> {
        requireSortPropertiesInRange(pageable, listOf("role"))
        return userService.listUserRoles(username ?: getCurrentUsername(), pageable)
    }

    @PostMapping("/changePassword")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun changePassword(
        @RequestBody request: UserChangePasswordRequest
    ): ResponseEntity<*> {
        // check permission
        if (!checkCurrentUserIsAdmin() && getCurrentUsername() != request.username) {
            // if is not admin, and username not match
            throw PermissionDeniedException("You can only change your own password")
        }
        // apply changes
        userService.changePassword(request.username, request.newPassword)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }

    @PostMapping("/changeStatus")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun changeStatus(
        @RequestBody request: UserChangeStatusRequest
    ): ResponseEntity<*> {
        // check permission
        if (!checkCurrentUserIsAdmin() && getCurrentUsername() != request.username) {
            // if is not admin, and username not match
            throw PermissionDeniedException("You can only change your own status")
        }
        // apply changes
        userService.changeStatus(request.username, request.newStatus)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }

    @PostMapping("/createUser")
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(
        @RequestBody request: CreateUserRequest
    ): ResponseEntity<*> {
        userService.createUser(request.username, request.password)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }

    @DeleteMapping("/deleteUser")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteUser(
        @RequestParam("username") username: String
    ): ResponseEntity<*> {
        userService.deleteUser(username)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }

    @PostMapping("/addUserRole")
    @PreAuthorize("hasRole('ADMIN')")
    fun addUserRole(
        @RequestBody request: UserRoleRequest
    ): ResponseEntity<*> {
        // Only accept validate roles
        requireValidateRole(request.role)
        userService.addRoleToUser(request.username, request.role)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }

    @DeleteMapping("/removeUserRole")
    @PreAuthorize("hasRole('ADMIN')")
    fun removeUserRole(
        @RequestBody request: UserRoleRequest
    ): ResponseEntity<*> {
        userService.removeRoleFromUser(request.username, request.role)
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body<Any>(null)
    }
}
