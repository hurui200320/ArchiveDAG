package info.skyblond.archivedag.arstue

import info.skyblond.archivedag.arstue.FileRecordService.Companion.CHERRY_PICK_PERMISSION_BIT
import info.skyblond.archivedag.arstue.FileRecordService.Companion.CHERRY_PICK_PERMISSION_CHAR
import info.skyblond.archivedag.arstue.FileRecordService.Companion.FULL_PERMISSION
import info.skyblond.archivedag.arstue.FileRecordService.Companion.READ_CURRENT_PERMISSION_BIT
import info.skyblond.archivedag.arstue.FileRecordService.Companion.READ_CURRENT_PERMISSION_CHAR
import info.skyblond.archivedag.arstue.FileRecordService.Companion.READ_HISTORY_PERMISSION_BIT
import info.skyblond.archivedag.arstue.FileRecordService.Companion.READ_HISTORY_PERMISSION_CHAR
import info.skyblond.archivedag.arstue.FileRecordService.Companion.UPDATE_NAME_PERMISSION_BIT
import info.skyblond.archivedag.arstue.FileRecordService.Companion.UPDATE_NAME_PERMISSION_CHAR
import info.skyblond.archivedag.arstue.FileRecordService.Companion.UPDATE_REF_PERMISSION_BIT
import info.skyblond.archivedag.arstue.FileRecordService.Companion.UPDATE_REF_PERMISSION_CHAR
import info.skyblond.archivedag.arstue.FileRecordService.Companion.permissionIntToString
import info.skyblond.archivedag.arstue.FileRecordService.Companion.permissionStringToInt
import info.skyblond.archivedag.arstue.entity.RecordAccessControlEntity.Type
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles

/**
 * Only test core functions, CRUD functions are tested in controller
 * */
@SpringBootTest
@ActiveProfiles("test")
internal class FileRecordServiceTest {
    @Autowired
    lateinit var fileRecordService: FileRecordService

    @Test
    fun testPermissionStringAndInt() {
        assertEquals(
            READ_CURRENT_PERMISSION_BIT,
            permissionStringToInt(READ_CURRENT_PERMISSION_CHAR.toString())
        )
        assertEquals(
            READ_CURRENT_PERMISSION_CHAR.toString(),
            permissionIntToString(READ_CURRENT_PERMISSION_BIT)
        )

        assertEquals(
            READ_HISTORY_PERMISSION_BIT,
            permissionStringToInt(READ_HISTORY_PERMISSION_CHAR.toString())
        )
        assertEquals(
            READ_HISTORY_PERMISSION_CHAR.toString(),
            permissionIntToString(READ_HISTORY_PERMISSION_BIT)
        )

        assertEquals(
            UPDATE_REF_PERMISSION_BIT,
            permissionStringToInt(UPDATE_REF_PERMISSION_CHAR.toString())
        )
        assertEquals(
            UPDATE_REF_PERMISSION_CHAR.toString(),
            permissionIntToString(UPDATE_REF_PERMISSION_BIT)
        )

        assertEquals(
            UPDATE_NAME_PERMISSION_BIT,
            permissionStringToInt(UPDATE_NAME_PERMISSION_CHAR.toString())
        )
        assertEquals(
            UPDATE_NAME_PERMISSION_CHAR.toString(),
            permissionIntToString(UPDATE_NAME_PERMISSION_BIT)
        )

        assertEquals(
            CHERRY_PICK_PERMISSION_BIT,
            permissionStringToInt(CHERRY_PICK_PERMISSION_CHAR.toString())
        )
        assertEquals(
            CHERRY_PICK_PERMISSION_CHAR.toString(),
            permissionIntToString(CHERRY_PICK_PERMISSION_BIT)
        )

        assertEquals("rhunc", permissionIntToString(FULL_PERMISSION))
        assertEquals(FULL_PERMISSION, permissionStringToInt("rhunc"))

        assertEquals(
            "rhc",
            permissionIntToString(READ_CURRENT_PERMISSION_BIT or READ_HISTORY_PERMISSION_BIT or CHERRY_PICK_PERMISSION_BIT)
        )
        assertEquals(
            READ_CURRENT_PERMISSION_BIT or READ_HISTORY_PERMISSION_BIT or CHERRY_PICK_PERMISSION_BIT,
            permissionStringToInt("rhc")
        )
    }

    @Test
    fun testPermissionUpdate() {
        val recordId = fileRecordService.createRecord("name", "owner")
        assertEquals(0, fileRecordService.listAccessRules(recordId, Pageable.unpaged()).size)
        // new permission for this record
        fileRecordService.setAccessRule(
            recordId, Type.USER, "user",
            permissionStringToInt("r")
        )
        assertEquals(1, fileRecordService.listAccessRules(recordId, Pageable.unpaged()).size)
        assertEquals(
            "r", permissionIntToString(
                fileRecordService.queryPermission(
                    recordId, "user", listOf()
                )
            )
        )
        // overwrite it
        fileRecordService.setAccessRule(
            recordId, Type.USER, "user",
            permissionStringToInt("rhc")
        )
        assertEquals(1, fileRecordService.listAccessRules(recordId, Pageable.unpaged()).size)
        assertEquals(
            "rhc", permissionIntToString(
                fileRecordService.queryPermission(
                    recordId, "user", listOf()
                )
            )
        )
    }

    @Test
    fun testListUserShared() {
        val recordIdList = Array(5) {
            fileRecordService.createRecord("test user shared $it", "owner")
        }
        recordIdList.forEach {
            fileRecordService.setAccessRule(it, Type.USER, "user", READ_CURRENT_PERMISSION_BIT)
        }
        val list = fileRecordService.listUserSharedRecords("user", Pageable.unpaged())
        assertArrayEquals(recordIdList, list.toTypedArray())
    }

    @Test
    fun testListGroupShared() {
        val recordIdList = Array(5) {
            fileRecordService.createRecord("test group shared $it", "owner")
        }
        recordIdList.forEach {
            fileRecordService.setAccessRule(it, Type.GROUP, "group", READ_CURRENT_PERMISSION_BIT)
        }
        val list = fileRecordService.listGroupSharedRecords("group", Pageable.unpaged())
        assertArrayEquals(recordIdList, list.toTypedArray())
    }

    @Test
    fun testListPublicShared() {
        val recordIdList = Array(5) {
            fileRecordService.createRecord("test public shared $it", "owner")
        }
        recordIdList.forEach {
            fileRecordService.setAccessRule(it, Type.OTHER, it.toString(), READ_CURRENT_PERMISSION_BIT)
        }
        val list = fileRecordService.listPublicSharedRecords(Pageable.unpaged())
        for (r in recordIdList) {
            // in other test there might be public shared entity
            // so make sure everything in recordIdList is in the result list
            assertTrue(r in list)
        }
    }

    @Test
    fun testPermissionResolve() {
        val recordId = fileRecordService.createRecord("name", "owner")
        // other::r
        fileRecordService.setAccessRule(
            recordId, Type.OTHER, "", READ_CURRENT_PERMISSION_BIT
        )
        // group:group_a:rh
        fileRecordService.setAccessRule(
            recordId, Type.GROUP, "group_a", READ_CURRENT_PERMISSION_BIT or READ_HISTORY_PERMISSION_BIT
        )
        // group:group_b:rc
        fileRecordService.setAccessRule(
            recordId, Type.GROUP, "group_b", READ_CURRENT_PERMISSION_BIT or CHERRY_PICK_PERMISSION_BIT
        )
        // user:user_a:rhuc
        fileRecordService.setAccessRule(
            recordId, Type.USER, "user_a",
            READ_CURRENT_PERMISSION_BIT or READ_HISTORY_PERMISSION_BIT or UPDATE_REF_PERMISSION_BIT or CHERRY_PICK_PERMISSION_BIT
        )

        // for group_c:user_b, it should be r (other)
        assertEquals(
            READ_CURRENT_PERMISSION_BIT,
            fileRecordService.queryPermission(recordId, "user_b", listOf("group_c"))
        )

        // for group_a:user_c, it should be rh (group_a)
        assertEquals(
            READ_CURRENT_PERMISSION_BIT or READ_HISTORY_PERMISSION_BIT,
            fileRecordService.queryPermission(recordId, "user_c", listOf("group_a"))
        )

        // for group_b:user_d, it should be rc (group_b)
        assertEquals(
            READ_CURRENT_PERMISSION_BIT or CHERRY_PICK_PERMISSION_BIT,
            fileRecordService.queryPermission(recordId, "user_d", listOf("group_b"))
        )

        // for user_e, he/she is in both group_a and group_b, it should be rhc (group a+b)
        assertEquals(
            READ_CURRENT_PERMISSION_BIT or READ_HISTORY_PERMISSION_BIT or CHERRY_PICK_PERMISSION_BIT,
            fileRecordService.queryPermission(recordId, "user_e", listOf("group_a", "group_b"))
        )

        // for user_a, it should be rhuc
        assertEquals(
            READ_CURRENT_PERMISSION_BIT or READ_HISTORY_PERMISSION_BIT or UPDATE_REF_PERMISSION_BIT or CHERRY_PICK_PERMISSION_BIT,
            fileRecordService.queryPermission(recordId, "user_a", listOf())
        )

        // test owner
        assertEquals(
            FULL_PERMISSION,
            fileRecordService.queryPermission(recordId, "owner", listOf())
        )
    }
}
