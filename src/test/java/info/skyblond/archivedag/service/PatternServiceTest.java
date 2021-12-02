package info.skyblond.archivedag.service;

import info.skyblond.archivedag.service.impl.PatternService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
        //min 4 char
        assertTrue(patternService.isValidUsername("root"));
        // max 20 char
        assertTrue(patternService.isValidUsername("root0123456789012345"));
        // has `_` inside
        assertTrue(patternService.isValidUsername("r_o_01_3_5j7_9012_4k"));

        // invalid usernames
        // too short
        assertFalse(patternService.isValidUsername("usr"));
        // too long
        assertFalse(patternService.isValidUsername("u01234567890123456789"));
        // has `__`
        assertFalse(patternService.isValidUsername("ro__ot"));
        // not start with a-z
        assertFalse(patternService.isValidUsername("1user"));
        // contains invalid char
        assertFalse(patternService.isValidUsername("uSer,"));
    }
}
