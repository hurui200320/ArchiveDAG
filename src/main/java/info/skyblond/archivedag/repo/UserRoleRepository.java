package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, String> {
    List<UserRoleEntity> findAllByUsername(String username);

    boolean existsByUsernameAndRole(String username, String role);

    @Modifying
    void deleteByUsernameAndRole(String username, String role);
}
