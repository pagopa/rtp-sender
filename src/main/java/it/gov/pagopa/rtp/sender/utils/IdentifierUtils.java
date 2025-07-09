package it.gov.pagopa.rtp.sender.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for operations related to identifiers.
 *
 * <p>This class provides helper methods for handling and formatting identifier values such as
 * UUIDs.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IdentifierUtils {

    private static final String UUID_PATTERN = "^([a-f0-9]{8})-([a-f0-9]{4})-([a-f0-9]{4})-([a-f0-9]{4})-([a-f0-9]{12})$";
    private static final String DASHES_REMOVER_PATTERN = "$1$2$3$4$5";

    private static final String UUID_WITHOUT_DASHES_PATTERN = "^([a-f0-9]{8})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{4})([a-f0-9]{12})$";
    private static final String DASHES_INSERTER_PATTERN = "$1-$2-$3-$4-$5";

    /**
     * Checks if the given string is a valid UUID without dashes.
     *
     * @param uuidString the string to be checked
     * @return true if the string is a valid UUID without dashes, false otherwise
     */
    public static boolean isValidUuidWithoutDashes(final String uuidString) {
        return StringUtils.isNotBlank(uuidString) && uuidString.matches(UUID_WITHOUT_DASHES_PATTERN);
    }

    /**
     * Formats a {@link UUID} by removing all hyphens ("-").
     *
     * <p>This method takes a UUID, converts it to its string representation, and removes all hyphen
     * characters to produce a compact string version.
     *
     * @param uuid the UUID to be formatted (must not be null)
     * @return a hyphen-free string representation of the UUID
     * @throws IllegalArgumentException if the uuid is null
     */
    @NonNull
    public static String formatUuidWithoutHyphens(@NonNull final UUID uuid) {
        return Optional.of(uuid)
                .map(UUID::toString)
                .map(s -> s.replaceFirst(UUID_PATTERN, DASHES_REMOVER_PATTERN))
                .orElseThrow(() -> new IllegalArgumentException("uuid cannot be null"));
    }

    /**
     * Reconstructs a {@link UUID} from a compact string without dashes.
     *
     * <p>This method takes a UUID string in the compact format (i.e., without dashes) and converts
     * it to the standard UUID format by inserting dashes at the appropriate positions.
     *
     * @param uuidString the UUID string without dashes (must not be null and must match a valid UUID format)
     * @return the reconstructed {@link UUID} object
     * @throws IllegalArgumentException if the input is null or does not conform to a valid UUID format
     */
    @NonNull
    public static UUID uuidRebuilder(@NonNull final String uuidString) {
        return Optional.of(uuidString)
                .map(s -> s.replaceFirst(UUID_WITHOUT_DASHES_PATTERN, DASHES_INSERTER_PATTERN))
                .map(UUID::fromString)
                .orElseThrow(() -> new IllegalArgumentException("Invalid UUID format"));
    }

    /**
     * Generates a deterministic UUID using the provided operation slug and RTP ID.
     *
     * <p>This is typically used for generating idempotency keys where the combination of operation type
     * and resource ID needs to produce the same UUID across retries.
     *
     * @param operationSlug a string representing the operation (e.g. "/sepa-request-to-pay-requests")
     * @param rtpId the UUID of the RTP resource
     * @return a UUID generated deterministically from the operation and RTP ID
     */
    @NonNull
    public static UUID generateDeterministicIdempotencyKey(@NonNull final String operationSlug, @NonNull final UUID rtpId) {
        Objects.requireNonNull(operationSlug, "operationSlug cannot be null");
        Objects.requireNonNull(rtpId, "rtpId cannot be null");

        return Optional.of(operationSlug + rtpId)
                .map(String::getBytes)
                .map(UUID::nameUUIDFromBytes)
                .orElseThrow(() -> new IllegalArgumentException("Cannot generate Deterministic Idempotency Key"));
    }

}
