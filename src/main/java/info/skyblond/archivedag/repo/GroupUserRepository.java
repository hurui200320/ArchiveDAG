package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.GroupUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface GroupUserRepository extends JpaRepository<GroupUserEntity, String> {

    @Modifying
    void deleteAllByGroupName(String groupName);

    boolean existsByGroupNameAndUsername(String groupName, String username);

    @Modifying
    void deleteByGroupNameAndUsername(String groupName, String username);

    Page<GroupUserEntity> findAllByUsername(String username, Pageable pageable);
}
