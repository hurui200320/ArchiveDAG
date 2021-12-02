package info.skyblond.archivedag.controller;

import info.skyblond.archivedag.service.impl.UserManagementService;
import info.skyblond.archivedag.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.server.PathParam;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserManagementService userService;

    public UserController(UserManagementService userService) {
        this.userService = userService;
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
}
