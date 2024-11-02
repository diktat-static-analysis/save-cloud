package com.saveourtool.save.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.UserService
import com.saveourtool.common.storage.AvatarKey
import com.saveourtool.common.storage.AvatarStorage
import com.saveourtool.common.utils.*
import com.saveourtool.common.utils.ByteBufferFluxResponse
import com.saveourtool.common.v1
import com.saveourtool.save.authservice.utils.username

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.Logger
import org.springframework.http.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import javax.transaction.NotSupportedException

import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

/**
 * Controller for working with avatars.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "avatars"),
)
@RestController
@RequestMapping(path = ["/api/$v1/avatar"])
internal class AvatarController(
    private val avatarStorage: AvatarStorage,
    private val organizationService: OrganizationService,
    private val userDetailsService: UserService,
) {
    @Operation(
        method = "POST",
        summary = "Set avatar from existing avatar packages",
        description = "This endpoint can only be used in case of setting avatar for user from existing resources",
    )
    @Parameters(
        Parameter(
            name = "resource",
            `in` = ParameterIn.QUERY,
            description = "static resource for avatar",
            required = true
        ),
        Parameter(name = "type", `in` = ParameterIn.QUERY, description = "type of avatar", required = true)

    )
    @GetMapping(path = ["/avatar-update"])
    fun updateAvatarFromResources(
        @RequestParam resource: String,
        @RequestParam type: AvatarType,
        authentication: Authentication
    ): Mono<StringResponse> = Mono.just(resource)
        .map {
            when (type) {
                AvatarType.ORGANIZATION -> throw NotSupportedException("Organization upload is not yet supported")
                AvatarType.USER -> userDetailsService.setAvatarFromResource(authentication.username(), it)
            }
        }.map {
            ResponseEntity.status(HttpStatus.OK).body("Avatar successfully updated with $it")
        }

    /**
     * @param partMono image to be uploaded
     * @param owner owner name
     * @param type type of avatar
     * @param contentLength
     * @param authentication
     * @return [Mono] with response
     */
    @Operation(
        method = "POST",
        summary = "Upload an avatar for user or organization.",
        description = "Upload an avatar for user or organization.",
    )
    @Parameters(
        Parameter(
            name = "owner",
            `in` = ParameterIn.QUERY,
            description = "user name or organization name",
            required = true
        ),
        Parameter(name = "type", `in` = ParameterIn.QUERY, description = "type of avatar", required = true),
        Parameter(name = FILE_PART_NAME, `in` = ParameterIn.DEFAULT, description = "body of avatar", required = true),
        Parameter(
            name = CONTENT_LENGTH_CUSTOM,
            `in` = ParameterIn.HEADER,
            description = "size in bytes of avatar",
            required = true
        ),
    )
    @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully.")
    @ApiResponse(responseCode = "404", description = "User or organization not found.")
    @PostMapping(path = ["/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun uploadImage(
        @RequestPart(FILE_PART_NAME) partMono: Mono<FilePart>,
        @RequestHeader(CONTENT_LENGTH_CUSTOM) contentLength: Long,
        @RequestParam owner: String,
        @RequestParam type: AvatarType,
        authentication: Authentication,
    ): Mono<StringResponse> = partMono
        .flatMap { part ->
            val avatarKey = AvatarKey(
                type,
                owner,
            )
            val content = part.content().map { it.asByteBuffer() }
            avatarStorage.overwrite(avatarKey, contentLength, content).map {
                log.info("Saved $contentLength bytes of $avatarKey")
            }
        }
        .flatMap {
            blockingToMono {
                when (type) {
                    AvatarType.ORGANIZATION -> organizationService.updateAvatarVersion(owner)
                    AvatarType.USER -> userDetailsService.updateAvatarVersion(authentication.username())
                }
            }
        }
        .map {
            ResponseEntity.ok().body(it)
        }

    @Operation(
        method = "GET",
        summary = "Download an avatar for user or organization.",
        description = "Download an avatar for user or organization.",
    )
    @Parameters(
        Parameter(name = "type", `in` = ParameterIn.PATH, description = "type of avatar", required = true),
        Parameter(
            name = "owner",
            `in` = ParameterIn.PATH,
            description = "user name or organization name",
            required = true
        ),
    )
    @ApiResponse(responseCode = "200", description = "Returns content of the file.")
    @ApiResponse(responseCode = "404", description = "User not found.")
    @GetMapping("/{typeStr}/{owner}")
    fun getImage(
        @PathVariable typeStr: String,
        @PathVariable owner: String
    ): Mono<ByteBufferFluxResponse> = getImage(
        AvatarKey(
            type = AvatarType.findByUrlPath(typeStr)
                .orResponseStatusException(HttpStatus.BAD_REQUEST) { "Not supported ${AvatarType::class.simpleName}: $typeStr" },
            objectName = owner,
        )
    )

    private fun getImage(avatarKey: AvatarKey): Mono<ByteBufferFluxResponse> = avatarKey.toMono()
        .filterWhen(avatarStorage::doesExist)
        .flatMap {
            avatarStorage.lastModified(avatarKey)
                .map { lastModified ->
                    ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(longExpirationTime.toJavaDuration()).cachePublic())
                        .lastModified(lastModified)
                        .body(avatarStorage.download(avatarKey))
                }
        }
        .switchIfEmptyToNotFound {
            "Not found avatar for $avatarKey"
        }

    companion object {
        private val log: Logger = getLogger<AvatarController>()
        private val longExpirationTime = 150.days
    }
}
