package info.skyblond.archivedag.service.impl;

import info.skyblond.archivedag.model.DuplicatedEntityException;
import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.bo.GroupDetailModel;
import info.skyblond.archivedag.model.entity.GroupMetaEntity;
import info.skyblond.archivedag.model.entity.GroupUserEntity;
import info.skyblond.archivedag.repo.GroupMetaRepository;
import info.skyblond.archivedag.repo.GroupUserRepository;
import info.skyblond.archivedag.util.GeneralKt;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.LinkedList;
import java.util.List;

@Service
public class GroupService {
    private final PatternService patternService;
    private final GroupMetaRepository groupMetaRepository;
    private final GroupUserRepository groupUserRepository;

    public GroupService(PatternService patternService, GroupMetaRepository groupMetaRepository, GroupUserRepository groupUserRepository) {
        this.patternService = patternService;
        this.groupMetaRepository = groupMetaRepository;
        this.groupUserRepository = groupUserRepository;
    }

    @Transactional
    public void createGroup(String groupName, String owner) {
        if (!this.patternService.isValidGroupName(groupName)) {
            throw new IllegalArgumentException("In valid group name. The group name must meet the regex: " + this.patternService.getGroupNameRegex());
        }
        GroupMetaEntity entity = new GroupMetaEntity(groupName, owner);

        if (this.groupMetaRepository.existsByGroupName(groupName)) {
            throw new DuplicatedEntityException(groupName);
        }
        this.groupMetaRepository.save(entity);
    }

    @Transactional
    public void deleteGroup(String groupName) {
        if (!this.groupMetaRepository.existsByGroupName(groupName)) {
            throw new EntityNotFoundException(groupName);
        }
        // delete all member
        this.groupUserRepository.deleteAllByGroupName(groupName);
        // delete meta data
        this.groupMetaRepository.deleteByGroupName(groupName);
    }

    public GroupDetailModel queryGroupMeta(String groupName) {
        GroupMetaEntity entity = this.groupMetaRepository.findByGroupName(groupName);
        if (entity == null) {
            return null;
        }

        return new GroupDetailModel(
                entity.getGroupName(),
                entity.getOwner(),
                GeneralKt.getUnixTimestamp(entity.getCreatedTime().getTime())
        );
    }

    @Transactional
    public void setGroupOwner(String groupName, String owner) {
        if (!this.groupMetaRepository.existsByGroupName(groupName)) {
            throw new EntityNotFoundException(groupName);
        }
        this.groupMetaRepository.updateGroupOwner(groupName, owner);
    }

    @Transactional
    public void addUserToGroup(String groupName, String username) {
        if (!this.groupMetaRepository.existsByGroupName(groupName)) {
            throw new EntityNotFoundException(groupName);
        }
        GroupUserEntity entity = new GroupUserEntity(groupName, username);
        if (this.groupUserRepository.exists(Example.of(entity))) {
            throw new DuplicatedEntityException("group member");
        }
        this.groupUserRepository.save(entity);
    }

    @Transactional
    public void removeUserFromGroup(String groupName, String username) {
        if (!this.groupMetaRepository.existsByGroupName(groupName)) {
            throw new EntityNotFoundException(groupName);
        }
        if (!this.groupUserRepository.existsByGroupNameAndUsername(groupName, username)) {
            throw new EntityNotFoundException("User " + username + " in " + groupName);
        }
        this.groupUserRepository.deleteByGroupNameAndUsername(groupName, username);
    }

    public List<String> listUserOwnedGroup(String username, Pageable pageable) {
        List<String> result = new LinkedList<>();
        this.groupMetaRepository.findAllByOwner(username, pageable)
                .forEach(m -> result.add(m.getGroupName()));
        return result;
    }

    public List<String> listUserJoinedGroup(String username, Pageable pageable) {
        List<String> result = new LinkedList<>();

        this.groupUserRepository.findAllByUsername(username, pageable)
                .forEach(r -> result.add(r.getGroupName()));
        return result;
    }

    public boolean userIsGroupMember(String groupName, String username) {
        return this.groupUserRepository.existsByGroupNameAndUsername(groupName, username);
    }

    public boolean userIsGroupOwner(String groupName, String username) {
        return this.groupMetaRepository.existsByGroupNameAndOwner(groupName, username);
    }

    public List<String> listGroupName(String keyword, Pageable pageable) {
        List<String> result = new LinkedList<>();
        this.groupMetaRepository.findAllByGroupNameContains(keyword, pageable)
                .forEach(m -> result.add(m.getGroupName()));
        return result;
    }
}
