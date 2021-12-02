package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    UserEntity findByUsername(String username);

    Page<UserEntity> findAllByUsernameContaining(String keyword, Pageable pageable);

    boolean existsByUsername(String username);

    @Modifying
    @Query("update UserEntity u set u.password = ?2 where u.username = ?1")
    void updateUserPassword(String username, String password);

    @Modifying
    @Query("update UserEntity u set u.status = ?2 where u.username = ?1")
    void updateUserStatus(String username, UserEntity.UserStatus status);

    @Modifying
    void deleteByUsername(String username);
}
