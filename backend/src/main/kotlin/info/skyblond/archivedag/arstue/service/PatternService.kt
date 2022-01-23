package info.skyblond.archivedag.arstue.service

import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class PatternService {

    /**
     * ^[a-z]  -  Username must start with lower case a-z (1)
     * [_](?![_])  -  Can be a `_`, but not before another `_`(`__` is not allowed)
     * [a-z0-9]  -  Or can be a-z and 0-9
     * {2,18}  -  Middle part at least has 2 char, at most has 18 char
     * [a-z0-9]$  -  must end with a-z or 0-9 (1)
     * in total: 2 + 2~18 -> 4 ~ 20 chars
     */
    private val usernamePattern = Pattern.compile(
        "^[a-z]([_](?![_])|[a-z0-9]){2,18}[a-z0-9]$"
    )

    /**
     * ^group_  -  Group name must start with `group_`
     * [a-z0-9]  -  Followed by digits or alphabets (cannot be `_`)
     * [_](?![_])  -  Can be a `_`, but not before another `_`(`__` is not allowed)
     * [a-z0-9]  -  Or can be a-z and 0-9
     * {2,18}  -  Middle part at least has 2 char, at most has 18 char
     * [a-z0-9]$  -  must end with a-z or 0-9 (1)
     * in total: 2 + 2~18 -> 4 ~ 20 chars (not including the leading `group_`)
     */
    private val groupNamePattern = Pattern.compile(
        "^group_[a-z0-9]([_](?![_])|[a-z0-9]){2,18}[a-z0-9]$"
    )

    fun isValidUsername(username: String): Boolean {
        return usernamePattern.matcher(username).matches()
    }

    val usernameRegex: String
        get() = usernamePattern.pattern()

    fun isValidGroupName(groupName: String): Boolean {
        return groupNamePattern.matcher(groupName).matches()
    }

    val groupNameRegex: String
        get() = groupNamePattern.pattern()

}
