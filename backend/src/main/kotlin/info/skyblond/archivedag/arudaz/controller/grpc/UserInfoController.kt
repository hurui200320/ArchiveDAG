package info.skyblond.archivedag.arudaz.controller.grpc

import info.skyblond.archivedag.arstue.GroupService
import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.info.UserInfoServiceGrpc
import info.skyblond.archivedag.arudaz.protos.info.WhoAmIResponse
import info.skyblond.archivedag.arudaz.utils.getCurrentUsername
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize

@GrpcService
class UserInfoController(
    private val userManagementService: UserManagementService,
    private val groupService: GroupService
) : UserInfoServiceGrpc.UserInfoServiceImplBase() {

    @PreAuthorize("hasRole('VIEWER')")
    override fun whoAmI(request: Empty, responseObserver: StreamObserver<WhoAmIResponse>) {
        val username = getCurrentUsername()
        val roles = userManagementService.listUserRoles(username, Pageable.unpaged())
        val ownedGroups = groupService.listUserOwnedGroup(username, Pageable.unpaged())
        val joinedGroups = groupService.listUserJoinedGroup(username, Pageable.unpaged())
        responseObserver.onNext(
            WhoAmIResponse.newBuilder()
                .setUsername(username)
                .addAllRole(roles)
                .addAllOwnedGroup(ownedGroups)
                .addAllJoinedGroup(joinedGroups)
                .build()
        )
        responseObserver.onCompleted()
    }
}
