/**
 * Unchecked functional interfaces — bridge between checked-exception APIs
 * and lambda-friendly {@code java.util.function} types.
 * <p>
 * Each interface extends a standard JDK functional interface and provides
 * a {@code *Throws} method for implementations that throw checked exceptions.
 * The default method catches checked exceptions and re-wraps them as
 * {@link RuntimeException}.
 */
package com.emme.functional;
