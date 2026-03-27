package io.telicent.jena.abac.labels.hashing;

import net.openhft.hashing.LongTupleHashFunction;

import static com.google.common.hash.Hashing.*;
import static net.openhft.hashing.LongHashFunction.*;
import static net.openhft.hashing.LongTupleHashFunction.xx128;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for generate hashing functions. Note: there is always a trade-off between speed of execution and the
 * risk of collision.
 * <p>
 * However it is worth highlighting that in reality wherever we use these functions (see
 * {@link io.telicent.jena.abac.labels.StoreFmtByHash}) we are never creating just one hash for each key.  Rather we
 * create a key by concatenating the hashes of the 4 independent elements of a {@link org.apache.jena.sparql.core.Quad}.
 * This means that we get 4 times the numbers of bits of the base hash function which drastically reduces the actual
 * collision probability to the point where it's essentially irrelevant.  For example using our default XX128 hash
 * function 4 times we are getting an effective 512-bit hash.  With that to even reach a 0.1% collision probability
 * you'd need to hash 52 quindecillion quads, which is approaching the number of atoms in the observable universe i.e.
 * essentially impossible.
 * </p>
 * <p>
 * <strong>TL;DR</strong> Any storage layer we are using will fall over well before we encounter any hash collisions.
 * </p>
 */
public class HasherUtil {

    /**
     * This is the maximum possible hash length in bytes, based on hash functions currently supported.
     * <p>
     * The longest hash is SHA-512 which is 512 bits and thus 64 bytes.  If a new hash function is introduced with a
     * larger size then this <strong>MUST</strong> be updated appropriately.
     * </p>
     * <p>
     * This constant is mainly used for stores that need to assign buffers for reading/writing hashes to ensure that
     * they assign a sufficiently sized buffer.
     * </p>
     */
    public static final int MAX_HASH_LENGTH = 512 / 8;

    /**
     * Private constructor as to be used as a static class
     **/
    private HasherUtil() {
    }

    /**
     * Mapping of string config parameters to Hash Functions
     */
    public static final Map<String, Supplier<Hasher>> hasherMap = new HashMap<>();

    /**
     * Constant name for City 64 hash
     */
    public static final String CITY_64 = "city64";

    /**
     * Constant name for Farm 64 hash
     */
    public static final String FARM_64 = "farm64";

    /**
     * Constant name for Farm 64 NA hash
     */
    public static final String FARM_NA_64 = "farmna64";

    /**
     * Constant name for Farm 64 UO hash
     */
    public static final String FARM_UO_64 = "farmuo64";

    /**
     * Constant name for Metro 64 hash
     */
    public static final String METRO_64 = "metro64";

    /**
     * Constant name for Murmur 64 hash
     */
    public static final String MURMUR_64 = "murmur64";

    /**
     * Constant name for Murmur 128 hash
     */
    public static final String MURMUR_128 = "murmur128";

    /**
     * Constant name for SHA 256 cryptographic hash
     */
    public static final String SHA_256 = "sha256";

    /**
     * Constant name for SHA 512 cryptographic hash
     */
    public static final String SHA_512 = "sha512";

    /**
     * Constant name for SIP 24 hash
     */
    public static final String SIP_24 = "sip24";

    /**
     * Constant name for WY3 hash
     */
    public static final String WY_3 = "wy3";

    /**
     * Constant name for XX 32 hash
     */
    public static final String XX_32 = "xx32";

    /**
     * Constant name for XX 64 hash
     */
    public static final String XX_64 = "xx64";

    /**
     * Constant name for XX 128 hash
     */
    public static final String XX_128 = "xx128";

    static {
        hasherMap.put(CITY_64, HasherUtil::createCity64Hasher);
        hasherMap.put(FARM_64, HasherUtil::createFarm64Hasher);
        hasherMap.put(FARM_NA_64, HasherUtil::createFarmNaHasher);
        hasherMap.put(FARM_UO_64, HasherUtil::createFarmUoHasher);
        hasherMap.put(METRO_64, HasherUtil::createMetro64Hasher);
        hasherMap.put(MURMUR_64, HasherUtil::createMurmur64Hasher);
        hasherMap.put(MURMUR_128, HasherUtil::createMurmer128Hasher);
        hasherMap.put(SHA_256, HasherUtil::createSHA256Hasher);
        hasherMap.put(SHA_512, HasherUtil::createSHA512Hasher);
        hasherMap.put(SIP_24, HasherUtil::createSIP24Hasher);
        hasherMap.put(WY_3, HasherUtil::createWY64Hasher);
        hasherMap.put(XX_32, HasherUtil::createXX32Hasher);
        hasherMap.put(XX_64, HasherUtil::createXX64Hasher);
        hasherMap.put(XX_128, HasherUtil::createXX128Hasher);
    }

    /**
     * Obtain the appropriate Hasher based on the provided string key.
     *
     * @param string The string key representing the hashing algorithm.
     * @return The corresponding Hasher. Defaults to 128 Bit XX Hash.
     */
    public static Hasher obtainHasherFromConfig(String string) {
        if (null == string || string.isEmpty()) {
            return createXX128Hasher();
        }
        return hasherMap.getOrDefault(string.toLowerCase(), HasherUtil::createXX128Hasher).get();
    }

    /**
     * Create a 64-bit XX Hash function.
     * <ul>
     *     <li>Very fast, often used in scenarios requiring speed over collision resistance.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Lower collision resistance compared to 128-bit variant. Still rare, with about 2^32 hashes (~4 billion)
     *     before a collision is expected.</li>
     * </ul>
     *
     * @return A Hasher using the 64-bit XX Hash algorithm.
     */
    public static Hasher createXX64Hasher() {
        return new BaseLongHasher(xx3(), XX_64);
    }

    /**
     * Create a 32-bit XX Hash function.
     * <ul>
     *     <li>Extremely fast for small inputs. Faster than 64, 128 variants</li>
     *     <li>Not cryptographically secure. </li>
     *     <li>Limited collision resistance due to the smaller 32-bit output space. Around 2^16 (65,000) hashes before
     *     expecting a collision.</li>
     * </ul>
     *
     * @return A Hasher using the 32-bit XX Hash algorithm.
     */
    public static Hasher createXX32Hasher() {
        return new BaseLongHasher(xx(), XX_32);
    }

    /**
     * Create a 64-bit Farm Hash function.
     * <ul>
     *     <li>Highly optimized for speed on 64-bit architectures.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Slightly slower than equivalent XX Hash in some scenarios.</li>
     *     <li>Collision rate probability grows significantly at 2^32 hashes.</li>
     * </ul>
     *
     * @return A Hasher using the 64-bit Farm Hash algorithm.
     */
    public static Hasher createFarm64Hasher() {
        return new BaseHasher(farmHashFingerprint64(), FARM_64);
    }

    /**
     * Create a Sip Hash function.
     * <ul>
     *     <li>Cryptographically secure, designed for resistance against hash-flooding attacks.</li>
     *     <li>Slower than non-cryptographic hash functions like XX or Murmur.</li>
     *     <li>Collision rate is low, significantly increasing after 2^64 hashes.</li>
     * </ul>
     *
     * @return A Hasher using the Sip 24 algorithm.
     */
    public static Hasher createSIP24Hasher() {
        return new BaseHasher(sipHash24(), SIP_24);
    }

    /**
     * Create a SHA-256 Hash function.
     * <ul>
     *     <li>Cryptographically secure, widely used in security applications.</li>
     *     <li>Extremely low chance of collisions (256-bit hash). Almost impossible, with 2^128 hashes (340 undecillion)
     *     before collisions become probable.</li>
     *     <li>Significantly slower than non-cryptographic hash functions.</li>
     * </ul>
     *
     * @return A Hasher using the SHA-256 algorithm.
     */
    public static Hasher createSHA256Hasher() {
        return new BaseHasher(sha256(), SHA_256);
    }

    /**
     * Create a SHA-512 Hash Function
     * <ul>
     *     <li>Cryptographically secure with an even larger 512-bit hash size.</li>
     *     <li>Almost zero chance of collisions in practical applications. With 2^512 hashes (115 quattuorvigintillion)
     *     before the possibility of collision becomes significant.</li>
     *     <li>Even slower than SHA-256, especially on 32-bit architectures.</li>
     * </ul>
     *
     * @return A Hasher using the SHA-512 algorithm.
     */
    public static Hasher createSHA512Hasher() {
        return new BaseHasher(sha512(), SHA_512);
    }

    /**
     * Create a 64-bit City Hash function.
     * <ul>
     *     <li>Optimized for small, short strings.</li>
     *     <li>Performs well in low-latency environments.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Slightly slower than XX equivalent.</li>
     *     <li>Collision probability similar to other 64 bit functions.</li>
     * </ul>
     *
     * @return A Hasher using the City Hash 64-bit algorithm.
     */
    public static Hasher createCity64Hasher() {
        return new BaseLongHasher(city_1_1(), CITY_64);
    }

    /**
     * Create a 64-bt Farm Hash NA function.
     * <ul>
     *     <li>Fast on modern architectures and optimized for various input sizes.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Collision probability similar to other 64 bit functions.</li>
     * </ul>
     *
     * @return A Hasher using the Farm Hash NA 64-bit algorithm.
     */
    public static Hasher createFarmNaHasher() {
        return new BaseLongHasher(farmNa(), FARM_NA_64);
    }

    /**
     * Create a 64-bit Farm Hash UO function.
     * <ul>
     *     <li>Similar to FarmHash NA, optimized for various architectures.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Collision probability similar to other 64 bit functions.</li>
     * </ul>
     *
     * @return A Hasher using the FarmHash UO algorithm.
     */
    public static Hasher createFarmUoHasher() {
        return new BaseLongHasher(farmUo(), FARM_UO_64);
    }

    /**
     * Create a 64-bit Metro Hash function.
     * <ul>
     *     <li>High-speed hash function for use in hash tables and checksums.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Collision probability similar to other 64 bit functions.</li>
     * </ul>
     *
     * @return A Hasher using the Metro Hash 64-bit algorithm.
     */
    public static Hasher createMetro64Hasher() {
        return new BaseLongHasher(metro(), METRO_64);
    }

    /**
     * Create a 64-bit Murmur Hash Function.
     * <ul>
     *     <li>Very fast on both 32-bit and 64-bit architectures.</li>
     *     <li>Good distribution for general-purpose use cases.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Collision probability similar to other 64 bit functions.</li>
     * </ul>
     *
     * @return A Hasher using the Murmur Hash 64-bit algorithm.
     */
    public static Hasher createMurmur64Hasher() {
        return new BaseLongHasher(murmur_3(), MURMUR_64);
    }


    /**
     * Create a 64-bit WY Hash function.
     * <ul>
     *     <li>Extremely fast, suitable for performance-sensitive environments.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>Collision probability similar to other 64 bit functions.</li>
     * </ul>
     *
     * @return A Hasher using the WY3 algorithm.
     */
    public static Hasher createWY64Hasher() {
        return new BaseLongHasher(wy_3(), WY_3);
    }


    /**
     * Create a 128-bit XX Hash function.
     * <ul>
     *     <li>Extremely fast, often the fastest non-cryptographic hash function.</li>
     *     <li>Not cryptographically secure.</li>
     *     <li>128-bit size offers good collision resistance. The number of hashes required before expecting a collision
     *     is in the region of ~2^64 (a quintillion) so very rare.</li>
     *     <li>Slower than 64 and 32 bit variants.</li>
     * </ul>
     *
     * @return A Hasher using the 128-bit XX Hash algorithm.
     */
    public static Hasher createXX128Hasher() {
        return new BaseLongTupleHasher(xx128(), XX_128);
    }

    /**
     * Create a 128-bit Murmur Hash Function.
     * <ul>
     *     <li>High-performance with better collision resistance than MurmurHash 64-bit.</li>
     *     <li>Not cryptographically secure.</li>
     * </ul>
     *
     * @return A Hasher using the Murmur Hash 128-bit algorithm.
     */
    public static Hasher createMurmer128Hasher() {
        return new BaseLongTupleHasher(LongTupleHashFunction.murmur_3(), MURMUR_128);
    }
}