package info.skyblond.archivedag.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
public class PatternService {
    /**
     * ^[a-z]  -  Username must start with lower case a-z (1)
     * [_](?![_])  -  Can be a `_`, but not before another `_`(`__` is not allowed)
     * [a-z0-9]  -  Or can be a-z and 0-9
     * {2,18}  -  Middle part at least has 2 char, at most has 18 char
     * [a-z0-9]$  -  must end with a-z or 0-9 (1)
     * in total: 2 + 2~18 -> 4 ~ 20 chars
     */
    private static final Pattern usernamePattern = Pattern.compile(
            "^[a-z]([_](?![_])|[a-z0-9]){2,18}[a-z0-9]$");

    /**
     * ^group_  -  Group name must start with `group_`
     * [a-z0-9]  -  Followed by digits or alphabets (cannot be `_`)
     * [_](?![_])  -  Can be a `_`, but not before another `_`(`__` is not allowed)
     * [a-z0-9]  -  Or can be a-z and 0-9
     * {2,18}  -  Middle part at least has 2 char, at most has 18 char
     * [a-z0-9]$  -  must end with a-z or 0-9 (1)
     * in total: 2 + 2~18 -> 4 ~ 20 chars (not including the leading `group_`)
     */
    private static final Pattern groupNamePattern = Pattern.compile(
            "^group_[a-z0-9]([_](?![_])|[a-z0-9]){2,18}[a-z0-9]$"
    );

    public boolean isValidUsername(String username) {
        return usernamePattern.matcher(username).matches();
    }

    public String getUsernameRegex() {
        return usernamePattern.pattern();
    }

    public boolean isValidGroupName(String groupName) {
        return groupNamePattern.matcher(groupName).matches();
    }

    public String getGroupNameRegex() {
        return groupNamePattern.pattern();
    }
}
