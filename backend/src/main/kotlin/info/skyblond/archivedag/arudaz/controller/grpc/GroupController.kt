package info.skyblond.archivedag.arudaz.controller.grpc

import info.skyblond.archivedag.arstue.GroupService
import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.group.*
import info.skyblond.archivedag.arudaz.utils.checkCurrentUserIsAdmin
import info.skyblond.archivedag.arudaz.utils.getCurrentUsername
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.PermissionDeniedException
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize

@GrpcService
class GroupController(
    private val groupService: GroupService,
    private val userManagementService: UserManagementService
) : GroupServiceGrpc.GroupServiceImplBase() {

    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    override fun createGroup(request: CreateGroupRequest, responseObserver: StreamObserver<Empty>) {
        var username = request.owner
        if (username.isBlank()) {
            username = getCurrentUsername()
        }
        if (!checkCurrentUserIsAdmin() && getCurrentUsername() != username) {
            // not admin && username is not current user
            throw PermissionDeniedException("You can only create group for yourself")
        }
        if (!userManagementService.userExists(username)) {
            throw EntityNotFoundException("User $username")
        }
        groupService.createGroup(request.groupName, username)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    override fun deleteGroup(request: DeleteGroupRequest, responseObserver: StreamObserver<Empty>) {
        if (!checkCurrentUserIsAdmin()
            && !groupService.userIsGroupOwner(request.groupName, getCurrentUsername())
        ) {
            // not admin && not the owner
            throw PermissionDeniedException("You can only delete your own group")
        }
        groupService.deleteGroup(request.groupName)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasRole('VIEWER')")
    override fun queryGroup(request: QueryGroupRequest, responseObserver: StreamObserver<GroupDetailResponse>) {
        responseObserver.onNext(
            groupService.queryGroupMeta(request.groupName).toProto()
        )
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    override fun transferGroup(request: TransferGroupRequest, responseObserver: StreamObserver<Empty>) {
        if (!checkCurrentUserIsAdmin()
            && !groupService.userIsGroupOwner(request.groupName, getCurrentUsername())
        ) {
            // not admin, not owner
            throw PermissionDeniedException("You can only transfer your own group")
        }
        val username = request.newOwner
        if (!userManagementService.userExists(username)) {
            throw EntityNotFoundException("User $username")
        }
        groupService.setGroupOwner(request.groupName, username)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    override fun joinGroup(request: JoinGroupRequest, responseObserver: StreamObserver<Empty>) {
        if (!checkCurrentUserIsAdmin()
            && !groupService.userIsGroupOwner(request.groupName, getCurrentUsername())
        ) {
            // not admin, not owner
            throw PermissionDeniedException("You can only add member to group owned by you")
        }
        if (!userManagementService.userExists(request.newMember)) {
            throw EntityNotFoundException("User ${request.newMember}")
        }
        groupService.addUserToGroup(request.groupName, request.newMember)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    override fun leaveGroup(request: LeaveGroupRequest, responseObserver: StreamObserver<Empty>) {
        var username = request.leavingMember
        if (username.isBlank()) {
            username = getCurrentUsername()
        }
        if (!checkCurrentUserIsAdmin()
            && getCurrentUsername() != username
            && !groupService.userIsGroupOwner(request.groupName, getCurrentUsername())
        ) {
            // not admin, not self, not owner
            throw PermissionDeniedException("You can only leave group for yourself")
        }
        groupService.removeUserFromGroup(request.groupName, username)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN')")
    override fun listOwnedGroup(
        request: ListOwnedGroupRequest,
        responseObserver: StreamObserver<GroupNameListResponse>
    ) {
        var username = request.username
        if (username.isBlank()) {
            username = getCurrentUsername()
        }
        if (!checkCurrentUserIsAdmin()
            && getCurrentUsername() != username
        ) {
            // not admin, not self
            throw PermissionDeniedException("You can only query for yourself")
        }
        val result = groupService.listUserOwnedGroup(username, Pageable.unpaged())
        responseObserver.onNext(GroupNameListResponse.newBuilder().addAllGroupName(result).build())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN')")
    override fun listJoinedGroup(
        request: ListJoinedGroupRequest,
        responseObserver: StreamObserver<GroupNameListResponse>
    ) {
        var username = request.username
        if (username.isBlank()) {
            username = getCurrentUsername()
        }
        if (!checkCurrentUserIsAdmin() && getCurrentUsername() != username) {
            // not admin, not self
            throw PermissionDeniedException("You can only query for yourself")
        }
        val result = groupService.listUserJoinedGroup(username, Pageable.unpaged())
        responseObserver.onNext(GroupNameListResponse.newBuilder().addAllGroupName(result).build())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasAnyRole('VIEWER', 'ADMIN')")
    override fun listGroupMember(
        request: ListGroupMemberRequest,
        responseObserver: StreamObserver<UsernameListResponse>
    ) {
        val groupName = request.groupName
        if (!checkCurrentUserIsAdmin()
            && !groupService.userIsGroupOwner(groupName, getCurrentUsername())
        ) {
            // not admin, not owner
            throw PermissionDeniedException("You can only query for yourself")
        }
        val result = groupService.listGroupMember(groupName, Pageable.unpaged())
        responseObserver.onNext(UsernameListResponse.newBuilder().addAllUsername(result).build())
        responseObserver.onCompleted()
    }

    @PreAuthorize("hasRole('VIEWER')")
    override fun listGroupName(request: ListGroupNameRequest, responseObserver: StreamObserver<GroupNameListResponse>) {
        val pageable: Pageable = Pageable.ofSize(request.limit)
        val result = groupService.listGroupName(request.keyword, pageable)
        responseObserver.onNext(GroupNameListResponse.newBuilder().addAllGroupName(result).build())
        responseObserver.onCompleted()
    }
}
