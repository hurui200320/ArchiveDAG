package info.skyblond.archivedag.repo;

import info.skyblond.archivedag.model.entity.GroupMetaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GroupMetaRepository extends JpaRepository<GroupMetaEntity, String> {
    boolean existsByGroupName(String groupName);

    boolean existsByGroupNameAndOwner(String groupName, String owner);

    @Modifying
    void deleteByGroupName(String groupName);

    GroupMetaEntity findByGroupName(String groupName);

    @Modifying
    @Query("update GroupMetaEntity m set m.owner = ?2 where m.groupName = ?1")
    void updateGroupOwner(String groupName, String owner);

    Page<GroupMetaEntity> findAllByOwner(String owner, Pageable pageable);


    Page<GroupMetaEntity> findAllByGroupNameContains(String keyword, Pageable pageable);
}
