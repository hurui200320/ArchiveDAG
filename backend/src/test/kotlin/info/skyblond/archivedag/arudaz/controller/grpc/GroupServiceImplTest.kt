package info.skyblond.archivedag.arudaz.controller.grpc

import info.skyblond.archivedag.arstue.GroupService
import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.group.*
import info.skyblond.archivedag.commons.EntityNotFoundException
import info.skyblond.archivedag.commons.PermissionDeniedException
import info.skyblond.archivedag.commons.getUnixTimestamp
import info.skyblond.archivedag.safeExecutable
import io.grpc.internal.testing.StreamRecorder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
internal class GroupServiceImplTest {
    @Autowired
    lateinit var groupController: GroupServiceImpl

    @Autowired
    lateinit var groupService: GroupService

    @Autowired
    lateinit var userManagementService: UserManagementService

    @BeforeEach
    fun setUp() {
        safeExecutable { userManagementService.createUser("test_user_admin", "password") }
        safeExecutable { userManagementService.createUser("test_user", "password") }
        safeExecutable { userManagementService.deleteUser("test_user_404") }
        safeExecutable { groupService.deleteGroup("group_test_group") }
        safeExecutable { groupService.deleteGroup("group_test_group2") }
        safeExecutable { groupService.deleteGroup("group_test_group_404") }
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testListAdmin() {
        createGroup("group_test_group", "test_user")
        createGroup("group_test_group2", "test_user")
        val request: ListGroupNameRequest = ListGroupNameRequest.newBuilder()
            .setKeyword("test")
            .setSize(20)
            .build()
        val responseObserver: StreamRecorder<GroupNameListResponse> = StreamRecorder.create()
        groupController.listGroupName(request, responseObserver)
        Assertions.assertNull(responseObserver.error)
        Assertions.assertEquals(1, responseObserver.values.size)
        val result: GroupNameListResponse = responseObserver.values[0]
        Assertions.assertEquals(2, result.groupNameList.size)
        Assertions.assertEquals(listOf("group_test_group", "group_test_group2"), result.groupNameList)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testListJoinedGroupAdmin() {
        createGroup("group_test_group", "test_user")
        createGroup("group_test_group2", "test_user")
        joinGroup("group_test_group2", "test_user")
        joinGroup("group_test_group", "test_user")
        val request: ListJoinedGroupRequest = ListJoinedGroupRequest.newBuilder()
            .setUsername("test_user")
            .setSize(20)
            .build()
        val responseObserver: StreamRecorder<GroupNameListResponse> = StreamRecorder.create()
        groupController.listJoinedGroup(request, responseObserver)
        Assertions.assertNull(responseObserver.error)
        Assertions.assertEquals(1, responseObserver.values.size)
        val result: GroupNameListResponse = responseObserver.values[0]
        Assertions.assertEquals(2, result.groupNameList.size)
        Assertions.assertEquals(listOf("group_test_group", "group_test_group2"), result.groupNameList)
    }


    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testListJoinedGroupUser() {
        createGroup("group_test_group", "test_user")
        createGroup("group_test_group2", "test_user")
        joinGroup("group_test_group2", "test_user")
        joinGroup("group_test_group", "test_user")
        var request: ListJoinedGroupRequest = ListJoinedGroupRequest.newBuilder()
            .setSize(20)
            .build()
        var responseObserver: StreamRecorder<GroupNameListResponse> = StreamRecorder.create()

        groupController.listJoinedGroup(request, responseObserver)
        Assertions.assertNull(responseObserver.error)
        Assertions.assertEquals(1, responseObserver.values.size)
        val result: GroupNameListResponse = responseObserver.values[0]
        Assertions.assertEquals(2, result.groupNameList.size)
        Assertions.assertEquals(listOf("group_test_group", "group_test_group2"), result.groupNameList)
        request = ListJoinedGroupRequest.newBuilder()
            .setSize(20)
            .setUsername("test_user_404")
            .build()
        StreamRecorder.create<GroupNameListResponse>().also { responseObserver = it }
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.listJoinedGroup(request, responseObserver)
        }
    }


    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testListOwnedGroupAdmin() {
        createGroup("group_test_group", "test_user")
        createGroup("group_test_group2", "test_user")
        joinGroup("group_test_group2", "test_user")
        joinGroup("group_test_group", "test_user")
        val request: ListOwnedGroupRequest = ListOwnedGroupRequest.newBuilder()
            .setUsername("test_user")
            .setSize(20)
            .build()
        val responseObserver: StreamRecorder<GroupNameListResponse> = StreamRecorder.create()
        groupController.listOwnedGroup(request, responseObserver)
        Assertions.assertNull(responseObserver.error)
        Assertions.assertEquals(1, responseObserver.values.size)
        val result: GroupNameListResponse = responseObserver.values[0]
        Assertions.assertEquals(2, result.groupNameList.size)
        Assertions.assertEquals(listOf("group_test_group", "group_test_group2"), result.groupNameList)
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testListOwnedGroupUser() {
        createGroup("group_test_group", "test_user")
        createGroup("group_test_group2", "test_user")
        joinGroup("group_test_group2", "test_user")
        joinGroup("group_test_group", "test_user")
        var request: ListOwnedGroupRequest = ListOwnedGroupRequest.newBuilder()
            .setSize(20)
            .build()
        var responseObserver: StreamRecorder<GroupNameListResponse> = StreamRecorder.create()
        groupController.listOwnedGroup(request, responseObserver)
        Assertions.assertNull(responseObserver.error)
        Assertions.assertEquals(1, responseObserver.values.size)
        val result: GroupNameListResponse = responseObserver.values[0]
        Assertions.assertEquals(2, result.groupNameList.size)
        Assertions.assertEquals(listOf("group_test_group", "group_test_group2"), result.groupNameList)
        request = ListOwnedGroupRequest.newBuilder()
            .setSize(20)
            .setUsername("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.listOwnedGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testLeaveGroupAdmin() {
        createGroup("group_test_group", "test_user")
        joinGroup("group_test_group", "test_user")
        var request: LeaveGroupRequest = LeaveGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .setLeavingMember("test_user")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.leaveGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        request = LeaveGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .setLeavingMember("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(EntityNotFoundException::class.java) {
            groupController.leaveGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testLeaveGroupUser() {
        createGroup("group_test_group", "test_user")
        joinGroup("group_test_group", "test_user")
        var request: LeaveGroupRequest = LeaveGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.leaveGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        joinGroup("group_test_group", "test_user_404")
        request = LeaveGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .setLeavingMember("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        groupController.leaveGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        createGroup("group_test_group2", "test_user_404")
        request = LeaveGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .setLeavingMember("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.leaveGroup(request, responseObserver)
        }
    }

    private fun joinGroup(groupName: String, username: String) {
        safeExecutable { groupService.addUserToGroup(groupName, username) }
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testJoinGroupAdmin() {
        createGroup("group_test_group", "test_user")
        var request: JoinGroupRequest = JoinGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.joinGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        request = JoinGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .setNewMember("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(EntityNotFoundException::class.java) {
            groupController.joinGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testJoinGroupUser() {
        createGroup("group_test_group", "test_user")
        var request: JoinGroupRequest = JoinGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.joinGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        createGroup("group_test_group2", "test_user_admin")
        request = JoinGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.joinGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testTransferGroupAdmin() {
        createGroup("group_test_group", "test_user")
        var request: TransferGroupRequest = TransferGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .setNewOwner("test_user_admin")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.transferGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        request = TransferGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .setNewOwner("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(EntityNotFoundException::class.java) {
            groupController.transferGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testTransferGroupUser() {
        createGroup("group_test_group", "test_user")
        var request: TransferGroupRequest = TransferGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .setNewOwner("test_user_admin")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.transferGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        createGroup("group_test_group2", "test_user_admin")
        request = TransferGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .setNewOwner("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.transferGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user", roles = ["VIEWER"])
    @Test
    fun testQueryGroup() {
        createGroup("group_test_group", "test_user")
        val request: QueryGroupRequest = QueryGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        val responseObserver: StreamRecorder<GroupDetailResponse> = StreamRecorder.create()
        groupController.queryGroup(request, responseObserver)
        Assertions.assertNull(responseObserver.error)
        Assertions.assertEquals(1, responseObserver.values.size)
        val result: GroupDetailResponse = responseObserver.values[0]
        Assertions.assertEquals("group_test_group", result.groupName)
        Assertions.assertEquals("test_user", result.owner)
        val d: Long = getUnixTimestamp() - result.createdTimestamp
        Assertions.assertTrue(d in 0..1)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testDeleteGroupAdmin() {
        createGroup("group_test_group", "test_user")
        var request: DeleteGroupRequest = DeleteGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.deleteGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        request = DeleteGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(EntityNotFoundException::class.java) {
            groupController.deleteGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testDeleteGroupUser() {
        createGroup("group_test_group", "test_user")
        var request: DeleteGroupRequest = DeleteGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.deleteGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        createGroup("group_test_group2", "test_user_404")
        request = DeleteGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.deleteGroup(request, responseObserver)
        }
        request = DeleteGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.deleteGroup(request, responseObserver)
        }
    }

    private fun createGroup(groupName: String, owner: String) {
        groupService.createGroup(groupName, owner)
    }

    @WithMockUser(username = "test_user_admin", roles = ["ADMIN"])
    @Test
    fun testCreateGroupAdmin() {
        var request: CreateGroupRequest = CreateGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .setOwner("test_user")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.createGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        request = CreateGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .setOwner("test_user_404") // 404 user
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(EntityNotFoundException::class.java) {
            groupController.createGroup(request, responseObserver)
        }
    }

    @WithMockUser(username = "test_user", roles = ["UPLOADER"])
    @Test
    fun testCreateGroupUser() {
        var request: CreateGroupRequest = CreateGroupRequest.newBuilder()
            .setGroupName("group_test_group")
            .build()
        var responseObserver = StreamRecorder.create<Empty>()
        groupController.createGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        request = CreateGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .setOwner("test_user")
            .build()
        responseObserver = StreamRecorder.create()
        groupController.createGroup(request, responseObserver)
        checkEmptyResponse(responseObserver)
        request = CreateGroupRequest.newBuilder()
            .setGroupName("group_test_group2")
            .setOwner("test_user_404")
            .build()
        responseObserver = StreamRecorder.create()
        Assertions.assertThrows(PermissionDeniedException::class.java) {
            groupController.createGroup(request, responseObserver)
        }
    }
}
