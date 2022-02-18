package info.skyblond.archivedag.arudaz.controller.grpc

import info.skyblond.archivedag.arstue.UserManagementService
import info.skyblond.archivedag.arudaz.protos.common.Empty
import info.skyblond.archivedag.arudaz.protos.info.WhoAmIResponse
import info.skyblond.archivedag.safeExecutable
import io.grpc.internal.testing.StreamRecorder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
internal class UserInfoControllerTest {
    @Autowired
    lateinit var userInfoController: UserInfoController

    @Autowired
    lateinit var userManagementService: UserManagementService

    @WithMockUser(username = "test_user_404", roles = ["VIEWER"])
    @Test
    fun testWhoAmI() {
        safeExecutable { userManagementService.deleteUser("test_user_404") }
        val result = assertDoesNotThrow {
            val responseObserver: StreamRecorder<WhoAmIResponse> = StreamRecorder.create()
            userInfoController.whoAmI(Empty.getDefaultInstance(), responseObserver)
            Assertions.assertNull(responseObserver.error)
            assertEquals(1, responseObserver.values.size)
            responseObserver.values[0]
        }
        assertEquals("test_user_404", result.username)
        assertEquals(listOf<String>(), result.roleList)
    }
}
