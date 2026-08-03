package com.ces.service.module.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of creating a user (or resetting their password).
 *
 * <p>Carries the generated temporary password — this is the only moment it exists in readable
 * form, since only its hash is stored. The admin has to pass it to the person, who is then forced
 * to replace it on first login.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatedUserResponse(
        UserResponse user,
        /** Present only when the system generated the password; null when the caller supplied one. */
        @JsonProperty("temporary_password") String temporaryPassword) {

    public static CreatedUserResponse of(UserResponse user, String temporaryPassword) {
        return new CreatedUserResponse(user, temporaryPassword);
    }
}
