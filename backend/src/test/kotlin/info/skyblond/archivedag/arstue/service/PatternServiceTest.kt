package info.skyblond.archivedag.arstue.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
internal class PatternServiceTest {
    @Autowired
    lateinit var patternService: PatternService

    @Test
    fun testUsername() {
        // valid usernames
        val validUsernames = listOf(
            "root", //min 4 char
            "root0123456789012345", // max 20 char
            "r_o_01_3_5j7_9012_4k", // has `_` inside
        )
        validUsernames.forEach {
            assertTrue(patternService.isValidUsername(it))
        }

        // invalid usernames
        val invalidUsernames = listOf(
            "usr", // too short
            "u01234567890123456789", // too long
            "ro__ot", // has `__`
            "1user", // not start with a-z
            "uSer,", // contains invalid char
        )
        invalidUsernames.forEach {
            assertFalse(patternService.isValidUsername(it))
        }
    }

    @Test
    fun testGroupName() {
        // valid group names
        val validGroupNames = listOf(
            "group_root", //min 4 char
            "group_root0123456789012345", // max 20 char
            "group_r_o_01_3_5j7_9012_4k", // has `_` inside
        )
        validGroupNames.forEach {
            assertTrue(patternService.isValidGroupName(it))
        }

        // invalid group names
        val invalidGroupNames = listOf(
            "group_", // empty
            "user_", // not starting with `group_`
            "group_usr", // too short
            "group_u01234567890123456789", // too long
            "group_ro__ot", // has `__`
            "group__1user", // not start with a-z0-9
            "group_uSer,",  // contains invalid char
        )
        invalidGroupNames.forEach {
            assertFalse(patternService.isValidGroupName(it))
        }
    }
}
