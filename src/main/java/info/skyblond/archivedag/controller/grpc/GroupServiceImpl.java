package info.skyblond.archivedag.controller.grpc;

import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.PermissionDeniedException;
import info.skyblond.archivedag.protos.common.Empty;
import info.skyblond.archivedag.protos.group.GroupDetail;
import info.skyblond.archivedag.protos.group.GroupNameList;
import info.skyblond.archivedag.protos.group.GroupRequestMessage;
import info.skyblond.archivedag.protos.group.GroupServiceGrpc;
import info.skyblond.archivedag.service.impl.GroupService;
import info.skyblond.archivedag.service.impl.UserManagementService;
import info.skyblond.archivedag.util.SecurityUtils;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static info.skyblond.archivedag.util.SecurityUtils.getCurrentUsername;

@GrpcService
public class GroupServiceImpl extends GroupServiceGrpc.GroupServiceImplBase {
    private final GroupService groupService;
    private final UserManagementService userManagementService;

    public GroupServiceImpl(GroupService groupService, UserManagementService userManagementService) {
        this.groupService = groupService;
        this.userManagementService = userManagementService;
    }

    @Override
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    public void createGroup(GroupRequestMessage request, StreamObserver<Empty> responseObserver) {
        String username = request.getUsername();
        if (username.isBlank()) {
            username = getCurrentUsername();
        }
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !getCurrentUsername().equals(username)) {
            // not admin && username is not current user
            throw new PermissionDeniedException("You can only create group for yourself");
        }
        if (this.userManagementService.queryUser(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        this.groupService.createGroup(request.getGroupName(), username);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    public void deleteGroup(GroupRequestMessage request, StreamObserver<Empty> responseObserver) {
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !this.groupService.userIsGroupOwner(request.getGroupName(), getCurrentUsername())) {
            // not admin && not the owner
            throw new PermissionDeniedException("You can only delete your own group");
        }
        this.groupService.deleteGroup(request.getGroupName());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('VIEWER')")
    public void queryGroup(GroupRequestMessage request, StreamObserver<GroupDetail> responseObserver) {
        responseObserver.onNext(this.groupService
                .queryGroupMeta(request.getGroupName())
                .toProto());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    public void transferGroup(GroupRequestMessage request, StreamObserver<Empty> responseObserver) {
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !this.groupService.userIsGroupOwner(request.getGroupName(), getCurrentUsername())) {
            // not admin, not owner
            throw new PermissionDeniedException("You can only transfer your own group");
        }
        String username = request.getUsername();
        if (this.userManagementService.queryUser(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        this.groupService.setGroupOwner(request.getGroupName(), username);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    public void joinGroup(GroupRequestMessage request, StreamObserver<Empty> responseObserver) {
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !this.groupService.userIsGroupOwner(request.getGroupName(), getCurrentUsername())) {
            // not admin, not owner
            throw new PermissionDeniedException("You can only add member to group owned by you");
        }
        String username = request.getUsername();
        if (username.isBlank()) {
            username = getCurrentUsername();
        }
        if (this.userManagementService.queryUser(username) == null) {
            throw new EntityNotFoundException("User " + username);
        }
        this.groupService.addUserToGroup(request.getGroupName(), username);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    public void leaveGroup(GroupRequestMessage request, StreamObserver<Empty> responseObserver) {
        String username = request.getUsername();
        if (username.isBlank()) {
            username = getCurrentUsername();
        }
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !getCurrentUsername().equals(username)
                && !this.groupService.userIsGroupOwner(request.getGroupName(), getCurrentUsername())) {
            // not admin, not self, not owner
            throw new PermissionDeniedException("You can only leave group for yourself");
        }
        this.groupService.removeUserFromGroup(request.getGroupName(), username);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN')")
    public void listOwnedGroup(GroupRequestMessage request, StreamObserver<GroupNameList> responseObserver) {
        String username = request.getUsername();
        if (username.isBlank()) {
            username = getCurrentUsername();
        }
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !getCurrentUsername().equals(username)) {
            // not admin, not self
            throw new PermissionDeniedException("You can only query for yourself");
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        List<String> result = this.groupService.listUserOwnedGroup(username, pageable);
        responseObserver.onNext(GroupNameList.newBuilder().addAllGroupName(result).build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN')")
    public void listJoinedGroup(GroupRequestMessage request, StreamObserver<GroupNameList> responseObserver) {
        String username = request.getUsername();
        if (username.isBlank()) {
            username = getCurrentUsername();
        }
        if (!SecurityUtils.checkCurrentUserIsAdmin()
                && !getCurrentUsername().equals(username)) {
            // not admin, not self
            throw new PermissionDeniedException("You can only query for yourself");
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        List<String> result = this.groupService.listUserJoinedGroup(username, pageable);
        responseObserver.onNext(GroupNameList.newBuilder().addAllGroupName(result).build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void listGroupName(GroupRequestMessage request, StreamObserver<GroupNameList> responseObserver) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        List<String> result = this.groupService.listGroupName(request.getGroupName(), pageable);
        responseObserver.onNext(GroupNameList.newBuilder().addAllGroupName(result).build());
        responseObserver.onCompleted();
    }
}
