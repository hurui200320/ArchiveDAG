package info.skyblond.archivedag.controller.grpc;

import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.WebMvcConfig;
import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.PermissionDeniedException;
import info.skyblond.archivedag.protos.common.Empty;
import info.skyblond.archivedag.protos.group.GroupDetail;
import info.skyblond.archivedag.protos.group.GroupNameList;
import info.skyblond.archivedag.protos.group.GroupRequestMessage;
import info.skyblond.archivedag.service.impl.GroupService;
import info.skyblond.archivedag.service.impl.UserManagementService;
import info.skyblond.archivedag.util.GeneralKt;
import io.grpc.internal.testing.StreamRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static info.skyblond.archivedag.controller.grpc.GrpcTestUtils.checkEmptyResponse;
import static info.skyblond.archivedag.controller.grpc.GrpcTestUtils.safeExecutable;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {EmbeddedRedisConfiguration.class, WebMvcConfig.class}
)
@ActiveProfiles("test")
class GroupServiceImplTest {
    @Autowired
    GroupServiceImpl groupController;
    @Autowired
    GroupService groupService;
    @Autowired
    UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        safeExecutable(() -> this.userManagementService.createUser("test_user_admin", "password"));
        safeExecutable(() -> this.userManagementService.createUser("test_user", "password"));
        safeExecutable(() -> this.userManagementService.deleteUser("test_user_404"));
        safeExecutable(() -> this.groupService.deleteGroup("group_test_group"));
        safeExecutable(() -> this.groupService.deleteGroup("group_test_group2"));
        safeExecutable(() -> this.groupService.deleteGroup("group_test_group_404"));
    }


    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testListAdmin() {
        createGroup("group_test_group", "test_user");
        createGroup("group_test_group2", "test_user");
        GroupRequestMessage request = GroupRequestMessage.newBuilder()
                .setGroupName("test")
                .setSize(20)
                .build();
        StreamRecorder<GroupNameList> responseObserver = StreamRecorder.create();
        groupController.listGroupName(request, responseObserver);
        assertNull(responseObserver.getError());
        assertEquals(1, responseObserver.getValues().size());
        GroupNameList result = responseObserver.getValues().get(0);
        assertEquals(2, result.getGroupNameList().size());
        assertEquals(List.of("group_test_group", "group_test_group2"), result.getGroupNameList());
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testListJoinedGroupAdmin() {
        createGroup("group_test_group", "test_user");
        createGroup("group_test_group2", "test_user");
        joinGroup("group_test_group2", "test_user");
        joinGroup("group_test_group", "test_user");
        GroupRequestMessage request = GroupRequestMessage.newBuilder()
                .setUsername("test_user")
                .setSize(20)
                .build();
        StreamRecorder<GroupNameList> responseObserver = StreamRecorder.create();
        groupController.listJoinedGroup(request, responseObserver);
        assertNull(responseObserver.getError());
        assertEquals(1, responseObserver.getValues().size());
        GroupNameList result = responseObserver.getValues().get(0);
        assertEquals(2, result.getGroupNameList().size());
        assertEquals(List.of("group_test_group", "group_test_group2"), result.getGroupNameList());
    }

    @WithMockUser(username = "test_user", roles = "VIEWER")
    @Test
    void testListJoinedGroupUser() {
        createGroup("group_test_group", "test_user");
        createGroup("group_test_group2", "test_user");
        joinGroup("group_test_group2", "test_user");
        joinGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setSize(20)
                    .build();
            StreamRecorder<GroupNameList> responseObserver = StreamRecorder.create();
        };
        groupController.listJoinedGroup(ref.request, ref.responseObserver);
        assertNull(ref.responseObserver.getError());
        assertEquals(1, ref.responseObserver.getValues().size());
        GroupNameList result = ref.responseObserver.getValues().get(0);
        assertEquals(2, result.getGroupNameList().size());
        assertEquals(List.of("group_test_group", "group_test_group2"), result.getGroupNameList());

        ref.request = GroupRequestMessage.newBuilder()
                .setSize(20)
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.listJoinedGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testListOwnedGroupAdmin() {
        createGroup("group_test_group", "test_user");
        createGroup("group_test_group2", "test_user");
        joinGroup("group_test_group2", "test_user");
        joinGroup("group_test_group", "test_user");
        GroupRequestMessage request = GroupRequestMessage.newBuilder()
                .setUsername("test_user")
                .setSize(20)
                .build();
        StreamRecorder<GroupNameList> responseObserver = StreamRecorder.create();
        groupController.listOwnedGroup(request, responseObserver);
        assertNull(responseObserver.getError());
        assertEquals(1, responseObserver.getValues().size());
        GroupNameList result = responseObserver.getValues().get(0);
        assertEquals(2, result.getGroupNameList().size());
        assertEquals(List.of("group_test_group", "group_test_group2"), result.getGroupNameList());
    }

    @WithMockUser(username = "test_user", roles = "VIEWER")
    @Test
    void testListOwnedGroupUser() {
        createGroup("group_test_group", "test_user");
        createGroup("group_test_group2", "test_user");
        joinGroup("group_test_group2", "test_user");
        joinGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setSize(20)
                    .build();
            StreamRecorder<GroupNameList> responseObserver = StreamRecorder.create();
        };
        groupController.listOwnedGroup(ref.request, ref.responseObserver);
        assertNull(ref.responseObserver.getError());
        assertEquals(1, ref.responseObserver.getValues().size());
        GroupNameList result = ref.responseObserver.getValues().get(0);
        assertEquals(2, result.getGroupNameList().size());
        assertEquals(List.of("group_test_group", "group_test_group2"), result.getGroupNameList());

        ref.request = GroupRequestMessage.newBuilder()
                .setSize(20)
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.listOwnedGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testLeaveGroupAdmin() {
        createGroup("group_test_group", "test_user");
        joinGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .setUsername("test_user")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.leaveGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group")
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(EntityNotFoundException.class, () ->
                groupController.joinGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user", roles = "UPLOADER")
    @Test
    void testLeaveGroupUser() {
        createGroup("group_test_group", "test_user");
        joinGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.leaveGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        joinGroup("group_test_group", "test_user_404");
        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group")
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        groupController.leaveGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        createGroup("group_test_group2", "test_user_404");
        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.leaveGroup(ref.request, ref.responseObserver));
    }

    private void joinGroup(String groupName, String username) {
        safeExecutable(() -> this.groupService.addUserToGroup(groupName, username));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testJoinGroupAdmin() {
        createGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.joinGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group")
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(EntityNotFoundException.class, () ->
                groupController.joinGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user", roles = "UPLOADER")
    @Test
    void testJoinGroupUser() {
        createGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.joinGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        createGroup("group_test_group2", "test_user_admin");
        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.joinGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testTransferGroupAdmin() {
        createGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .setUsername("test_user_admin")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.transferGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group")
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(EntityNotFoundException.class, () ->
                groupController.transferGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user", roles = "UPLOADER")
    @Test
    void testTransferGroupUser() {
        createGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .setUsername("test_user_admin")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.transferGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        createGroup("group_test_group2", "test_user_admin");
        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.transferGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user", roles = "VIEWER")
    @Test
    void testQueryGroup() {
        createGroup("group_test_group", "test_user");
        GroupRequestMessage request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group")
                .build();
        StreamRecorder<GroupDetail> responseObserver = StreamRecorder.create();
        groupController.queryGroup(request, responseObserver);
        assertNull(responseObserver.getError());
        assertEquals(1, responseObserver.getValues().size());
        GroupDetail result = responseObserver.getValues().get(0);
        assertEquals("group_test_group", result.getGroupName());
        assertEquals("test_user", result.getOwner());
        long d = GeneralKt.getUnixTimestamp() - result.getCreatedTimestamp();
        assertTrue(d <= 1 && d >= 0);
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testDeleteGroupAdmin() {
        createGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.deleteGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(EntityNotFoundException.class, () ->
                groupController.deleteGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user", roles = "UPLOADER")
    @Test
    void testDeleteGroupUser() {
        createGroup("group_test_group", "test_user");
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.deleteGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        createGroup("group_test_group2", "test_user_404");
        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.deleteGroup(ref.request, ref.responseObserver));

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.deleteGroup(ref.request, ref.responseObserver));
    }

    private void createGroup(String groupName, String owner) {
        this.groupService.createGroup(groupName, owner);
    }

    @WithMockUser(username = "test_user_admin", roles = "ADMIN")
    @Test
    void testCreateGroupAdmin() {
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .setUsername("test_user")
                .build();
        ref.responseObserver = StreamRecorder.create();
        groupController.createGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .setUsername("test_user_404") // 404 user
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(EntityNotFoundException.class, () ->
                groupController.createGroup(ref.request, ref.responseObserver));
    }

    @WithMockUser(username = "test_user", roles = "UPLOADER")
    @Test
    void testCreateGroupUser() {
        var ref = new Object() {
            GroupRequestMessage request = GroupRequestMessage.newBuilder()
                    .setGroupName("group_test_group")
                    .build();
            StreamRecorder<Empty> responseObserver = StreamRecorder.create();
        };
        groupController.createGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .setUsername("test_user")
                .build();
        ref.responseObserver = StreamRecorder.create();
        groupController.createGroup(ref.request, ref.responseObserver);
        checkEmptyResponse(ref.responseObserver);

        ref.request = GroupRequestMessage.newBuilder()
                .setGroupName("group_test_group2")
                .setUsername("test_user_404")
                .build();
        ref.responseObserver = StreamRecorder.create();
        assertThrows(PermissionDeniedException.class, () ->
                groupController.createGroup(ref.request, ref.responseObserver));
    }

}
