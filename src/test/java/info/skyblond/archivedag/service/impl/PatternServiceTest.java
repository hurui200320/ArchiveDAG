package info.skyblond.archivedag.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PatternServiceTest {
    @Autowired
    PatternService patternService;

    @Test
    void testUsername() {
        // valid usernames
        List<String> validUsernames = List.of(
                //min 4 char
                "root",
                // max 20 char
                "root0123456789012345",
                // has `_` inside
                "r_o_01_3_5j7_9012_4k"
        );
        validUsernames.forEach(it -> assertTrue(patternService.isValidUsername(it)));

        // invalid usernames
        List<String> invalidUsernames = List.of(
                // too short
                "usr",
                // too long
                "u01234567890123456789",
                // has `__`
                "ro__ot",
                // not start with a-z
                "1user",
                // contains invalid char
                "uSer,"
        );
        invalidUsernames.forEach(it -> assertFalse(patternService.isValidUsername(it)));
    }

    @Test
    void testGroupName() {
        // valid group names
        List<String> validGroupNames = List.of(
                //min 4 char
                "group_root",
                // max 20 char
                "group_root0123456789012345",
                // has `_` inside
                "group_r_o_01_3_5j7_9012_4k"
        );
        validGroupNames.forEach(it -> assertTrue(patternService.isValidGroupName(it)));

        // invalid group names
        List<String> invalidGroupNames = List.of(
                // empty
                "group_",
                // not starting with `group_`
                "user_",
                // too short
                "group_usr",
                // too long
                "group_u01234567890123456789",
                // has `__`
                "group_ro__ot",
                // not start with a-z0-9
                "group__1user",
                // contains invalid char
                "group_uSer,"
        );
        invalidGroupNames.forEach(it -> assertFalse(patternService.isValidGroupName(it)));
    }
}
