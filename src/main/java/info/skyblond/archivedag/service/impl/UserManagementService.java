package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.model.DuplicatedEntityException;
import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.PermissionDeniedException;
import info.skyblond.archivedag.model.UserDetailModel;
import info.skyblond.archivedag.model.entity.UserEntity;
import info.skyblond.archivedag.model.entity.UserRoleEntity;
import info.skyblond.archivedag.repo.UserRepository;
import info.skyblond.archivedag.repo.UserRoleRepository;
import info.skyblond.archivedag.util.SecurityUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.LinkedList;
import java.util.List;

@Service
public class UserManagementService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatternService patternService;

    public UserManagementService(UserRepository userRepository, UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder, PatternService patternService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.patternService = patternService;
    }

    public List<String> listUsername(String keyword, Pageable pageable) {
        List<String> result = new LinkedList<>();
        this.userRepository.findAllByUsernameContaining(keyword, pageable)
                .forEach(u -> result.add(u.getUsername()));
        return result;
    }

    public UserDetailModel queryUser(String username) {
        UserEntity entity = this.userRepository.findByUsername(username);
        if (entity == null) {
            return null;
        }
        List<String> roles = new LinkedList<>();

        this.userRoleRepository.findAllByUsername(username)
                .forEach(r -> roles.add(r.getRole()));

        return new UserDetailModel(
                entity.getUsername(),
                entity.getStatus(),
                roles
        );
    }

    public List<String> listUserRoles(String username, Pageable pageable) {
        List<String> roles = new LinkedList<>();
        this.userRoleRepository.findAllByUsername(username, pageable)
                .forEach(r -> roles.add(r.getRole()));
        return roles;
    }

    @Transactional
    public void changePassword(String username, String password) {
        if (this.userRepository.findByUsername(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        String encodedPassword = this.passwordEncoder.encode(password);
        this.userRepository.updateUserPassword(username, encodedPassword);
    }

    @Transactional
    public void changeStatus(String username, UserEntity.UserStatus status) {
        if (this.userRepository.findByUsername(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        this.userRepository.updateUserStatus(username, status);
    }

    @Transactional
    public void createUser(String username, String password) {
        if (!this.patternService.isValidUsername(username)) {
            throw new IllegalArgumentException("In valid username. The username must meet the regex: " + this.patternService.getUsernameRegex());
        }
        UserEntity entity = new UserEntity(username, this.passwordEncoder.encode(password));
        // Here we should use lock to ensure the user still doesn't exist
        // after this execution, but seems like a lock can only be applied
        // to select statement in JPA, and transaction cannot ensure this
        // unless the isolation is sequence.
        if (this.userRepository.existsByUsername(username)) {
            throw new DuplicatedEntityException("user");
        }
        // The issue for save is: If the username not exists, then it's an insert
        // But if the username exists, it updates.
        this.userRepository.save(entity);
    }

    @Transactional
    public void deleteUser(String username) {
        if (this.userRepository.findByUsername(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        // TODO check other resources, like cert
        this.userRoleRepository.deleteAllByUsername(username);
        this.userRepository.deleteByUsername(username);
    }

    @Transactional
    public void addRoleToUser(String username, String role) {
        if (this.userRepository.findByUsername(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        UserRoleEntity entity = new UserRoleEntity(username, role);
        if (this.userRoleRepository.exists(Example.of(entity))) {
            throw new DuplicatedEntityException("role for user");
        }
        this.userRoleRepository.save(entity);
    }

    @Transactional
    public void removeRoleFromUser(String username, String role) {
        if (this.userRepository.findByUsername(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        if (!this.userRoleRepository.existsByUsernameAndRole(username, role)) {
            throw new EntityNotFoundException("Role " + role + " for user " + username);
        }
        this.userRoleRepository.deleteByUsernameAndRole(username, role);
    }
}
