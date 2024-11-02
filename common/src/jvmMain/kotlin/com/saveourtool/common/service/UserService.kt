package com.saveourtool.common.service

import com.saveourtool.common.domain.Role
import com.saveourtool.common.domain.UserSaveStatus
import com.saveourtool.common.entities.OriginalLogin
import com.saveourtool.common.entities.User
import com.saveourtool.common.evententities.UserEvent
import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.repository.LnkUserOrganizationRepository
import com.saveourtool.common.repository.LnkUserProjectRepository
import com.saveourtool.common.repository.OriginalLoginRepository
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.storage.AvatarKey
import com.saveourtool.common.storage.AvatarStorage
import com.saveourtool.common.utils.*

import org.slf4j.Logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.scheduler.Schedulers
import java.lang.IllegalArgumentException
import java.util.NoSuchElementException

/**
 * Service for user
 */
@Service
@Suppress("LongParameterList")
class UserService(
    private val userRepository: UserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val originalLoginRepository: OriginalLoginRepository,
    private val lnkUserProjectRepository: LnkUserProjectRepository,
    private val avatarStorage: AvatarStorage,
) {
    /**
     * @param user user for update
     * @return updated user
     */
    fun saveUser(user: User) = userRepository.save(user)

    /**
     * @param username
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByName(username: String) = userRepository.findByName(username)

    /**
     * @param role
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByRole(role: String) = userRepository.findByRole(role)

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByOriginalLogin(username: String, source: String) =
            originalLoginRepository.findByNameAndSource(username, source)?.user

    /**
     * @param name
     * @return user with [name]
     */
    fun getUserByName(name: String): User = userRepository.findByName(name).orNotFound { "Not found user by name $name" }

    /**
     * @param id
     * @return user with [id]
     */
    fun findById(id: Long): User = userRepository.findByIdOrNull(id).orNotFound { "Not found user by id $id" }

    /**
     * @param ids
     * @return users with [ids]
     */
    fun findAllByIdIn(ids: List<Long>): List<User> = userRepository.findAllByIdIn(ids)

    /**
     * @param authentication
     * @return global [Role] of authenticated user
     */
    fun getGlobalRole(authentication: Authentication): Role = getUserByName(authentication.username())
        .role
        ?.let { Role.fromSpringSecurityRole(it) }
        ?: Role.VIEWER

    /**
     * @param organizationName
     * @param userName
     * @return role for user in organization by user ID and organization name
     */
    fun findRoleByUserNameAndOrganizationName(userName: String, organizationName: String) = lnkUserOrganizationRepository
        .findByUserNameAndOrganizationName(userName, organizationName)
        ?.role
        ?: Role.NONE

    /**
     * @param authentication
     * @param organizationName
     * @return the highest of two roles: the one in organization with name [organizationName] and global one.
     */
    fun getGlobalRoleOrOrganizationRole(authentication: Authentication, organizationName: String): Role {
        val selfName = authentication.username()
        val selfGlobalRole = getGlobalRole(authentication)
        val selfOrganizationRole = findRoleByUserNameAndOrganizationName(selfName, organizationName)
        return getHighestRole(selfOrganizationRole, selfGlobalRole)
    }

    /**
     * @param newUser
     * @param oldName
     * @param oldUserStatus
     * @return UserSaveStatus
     */
    @Transactional
    fun saveUser(newUser: User, oldName: String?, oldUserStatus: UserStatus): UserSaveStatus {
        applicationEventPublisher.publishEvent(UserEvent(newUser))
        val isNameFreeAndNotTaken = userRepository.validateName(newUser.name) != 0L
        // if we are registering new user (updating just name and status to NOT_APPROVED):
        return if (oldUserStatus == UserStatus.CREATED && newUser.status == UserStatus.NOT_APPROVED) {
            // checking if the user with new name already exists (it's definitely not our user, so if found -> CONFLICT)
            if (isNameFreeAndNotTaken) {
                userRepository.save(newUser)
                UserSaveStatus.UPDATE
            } else {
                UserSaveStatus.CONFLICT
            }
        } else {
            // we are trying to change the name of ACTIVE user
            oldName?.let {
                // but such name is already taken and exists in db
                if (isNameFreeAndNotTaken) {
                    userRepository.deleteHighLevelName(oldName)
                    userRepository.saveHighLevelName(newUser.name)
                    userRepository.save(newUser)
                    UserSaveStatus.UPDATE
                } else {
                    UserSaveStatus.CONFLICT
                }
                // if we are changing other fields of ACTIVE users, but not changing the name we can just save user
                // here we highly depend on the `oldName` field (from client code)
                // but we are safe as we have a unique constraint on the database
            } ?: run {
                userRepository.save(newUser)
                UserSaveStatus.UPDATE
            }
        }
    }

    /**
     * @param name name of user
     * @return UserSaveStatus
     */
    @Transactional
    fun approveUser(
        name: String,
    ): UserSaveStatus {
        val user: User = userRepository.findByName(name).orNotFound()

        userRepository.save(user.apply {
            this.status = UserStatus.ACTIVE
        })

        return UserSaveStatus.APPROVED
    }

    /**
     * @param name name of user
     * @return UserSaveStatus
     */
    @Transactional
    fun banUser(
        name: String,
    ): UserSaveStatus {
        val user: User = userRepository.findByName(name).orNotFound().apply {
            this.status = UserStatus.BANNED
        }
        applicationEventPublisher.publishEvent(UserEvent(user))

        userRepository.save(user)

        return UserSaveStatus.BANNED
    }

    /**
     * @param source
     * @param name
     * @return existed [User] or a new one
     */
    @Transactional
    fun saveNewUserIfRequired(source: String, name: String): User =
            originalLoginRepository.findByNameAndSource(name, source)
                ?.user
                ?.also {
                    log.debug("User $name ($source) is already present in the DB")
                }
                ?: run {
                    log.info {
                        "Saving user $name ($source) with authorities $roleForNewUser to the DB"
                    }
                    saveNewUser(name).also { savedUser ->
                        addSource(savedUser, name, source)
                    }
                }

    /**
     * @param user
     * @param nameInSource
     * @param source
     */
    @Transactional
    fun addSource(user: User, nameInSource: String, source: String) {
        originalLoginRepository.save(OriginalLogin(nameInSource, user, source))
    }

    /**
     * @param userNameCandidate
     * @return created [User]
     */
    private fun saveNewUser(userNameCandidate: String): User {
        val existedUser = userRepository.findByName(userNameCandidate)
        val name = existedUser?.let {
            val prefix = "$userNameCandidate$UNIQUE_NAME_SEPARATOR"
            val suffix = userRepository.findByNameStartingWith(prefix)
                .map { it.name.replace(prefix, "") }
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull()
                ?.inc()
                ?: 1
            "$prefix$suffix"
        } ?: run {
            userNameCandidate
        }
        return userRepository.save(
            User(
                name = name,
                password = null,
                role = roleForNewUser,
                status = UserStatus.CREATED,
            )
        )
    }

    /**
     * @param name name of user
     * @param authentication
     * @return UserSaveStatus
     */
    @Transactional
    fun deleteUser(
        name: String,
        authentication: Authentication,
    ): UserSaveStatus {
        val user: User = userRepository.findByName(authentication.username()).orNotFound {
            "User with name ${authentication.username()} not found in database"
        }
        val newName = "Deleted-${user.id}"
        userRepository.deleteHighLevelName(user.name)
        userRepository.saveHighLevelName(newName)
        userRepository.save(user.apply {
            this.name = newName
            this.status = UserStatus.DELETED
            this.avatar = null
            this.company = null
            this.twitter = null
            this.email = null
            this.gitHub = null
            this.linkedin = null
            this.location = null
        })

        val avatarKey = AvatarKey(
            AvatarType.USER,
            name,
        )
        avatarStorage.delete(avatarKey)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()

        originalLoginRepository.deleteByUserId(user.requiredId())
        lnkUserProjectRepository.deleteByUserId(user.requiredId())
        lnkUserOrganizationRepository.deleteByUserId(user.requiredId())

        return UserSaveStatus.DELETED
    }

    /**
     * We change the version just to work-around the caching on the frontend
     *
     * @param name
     * @return the id (version) of new avatar
     * @throws NoSuchElementException
     */
    fun updateAvatarVersion(name: String): String {
        val user = userRepository.findByName(name).orNotFound()
        var version = user.avatar?.find { it == '?' }?.let {
            user.avatar?.substringAfterLast("?")?.toInt() ?: 0
        } ?: 0
        val newAvatar = "${AvatarType.USER.toUrlStr(name)}?${++version}"
        user.apply { avatar = newAvatar }
            .let { userRepository.save(it) }

        return newAvatar
    }

    /**
     * Only for static resources!
     *
     * @param name
     * @param resource
     * @throws IllegalArgumentException
     */
    fun setAvatarFromResource(name: String, resource: String) {
        if (!resource.startsWith(AVATARS_PACKS_DIR)) {
            throw IllegalArgumentException("Only avatars from $AVATARS_PACKS_DIR can be set for user $name")
        }

        userRepository.findByName(name)
            .orNotFound()
            .apply { avatar = resource }
            .let { userRepository.save(it) }
    }

    companion object {
        private val log: Logger = getLogger<UserService>()
        private const val UNIQUE_NAME_SEPARATOR = "_"
        private val roleForNewUser = Role.VIEWER.asSpringSecurityRole()
    }
}
