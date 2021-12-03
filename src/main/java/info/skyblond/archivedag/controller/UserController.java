package info.skyblond.archivedag.controller;

import info.skyblond.archivedag.model.*;
import info.skyblond.archivedag.service.impl.UserManagementService;
import info.skyblond.archivedag.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserManagementService userService;
    private final AuthenticationManager authenticationManager;

    public UserController(UserManagementService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/listUsername")
    @PreAuthorize("hasRole('USER')")
    public List<String> listUsername(
            @PathParam("keyword") String keyword,
            Pageable pageable
    ) {
        SecurityUtils.requireSortPropertiesInRange(pageable, List.of("username"));
        return this.userService.listUsername(keyword, pageable);
    }

    @GetMapping("/queryUser")
    @PreAuthorize("hasRole('USER')")
    public UserDetailModel queryUser(
            @PathParam("username") String username) {
        return this.userService.queryUser(username);
    }

    @GetMapping("/listUserRoles")
    @PreAuthorize("hasRole('USER')")
    public List<String> listUserRoles(
            @PathParam("username") String username,
            Pageable pageable
    ) {
        SecurityUtils.requireSortPropertiesInRange(pageable, List.of("role"));
        return this.userService.listUserRoles(username, pageable);
    }

    @PostMapping("/changePassword")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> changePassword(
            @RequestBody UserChangePasswordRequest request
    ) {
        // check permission
        if (SecurityUtils.checkCurrentUserHasRole("ROLE_USER")
                && !SecurityUtils.getCurrentUsername().equals(request.getUsername())) {
            // if is user, and username not match
            throw new PermissionDeniedException("You can only change your own password");
        }
        // check old password
        this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getOldPassword()
        ));
        // apply changes
        this.userService.changePassword(request.getUsername(), request.getNewPassword());
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PostMapping("/changeStatus")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> changeStatus(
            @RequestBody UserChangeStatusRequest request
    ) {
        // check permission
        if (SecurityUtils.checkCurrentUserHasRole("ROLE_USER")
                && !SecurityUtils.getCurrentUsername().equals(request.getUsername())) {
            // if is user, and username not match
            throw new PermissionDeniedException("You can only change your own status");
        }
        // check password
        this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword()
        ));
        // apply changes
        this.userService.changeStatus(request.getUsername(), request.getNewStatus());
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PostMapping("/createUser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            @RequestBody CreateUserRequest request
    ){
        this.userService.createUser(request.getUsername(), request.getPassword());
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @DeleteMapping("/deleteUser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(
            @PathParam("username") String username
    ){
        this.userService.deleteUser(username);
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PostMapping("/addUserRole")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addUserRole(
            @RequestBody UserRoleRequest request
    ){
        // Only accept validate roles
        SecurityUtils.requireValidateRole(request.getRole());
        this.userService.addRoleToUser(request.getUsername(), request.getRole());
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @DeleteMapping("/removeUserRole")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeUserRole(
            @RequestBody UserRoleRequest request
    ){
        this.userService.removeRoleFromUser(request.getUsername(), request.getRole());
        // return 204
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

}