package service;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

/**
 * Service to cache shared AES keys for conversations and public RSA keys for
 * exchange.
 */
public class KeyService {

    // Cache 1: Shared AES Keys <ConversationID_String, SecretKey>
    private static final Map<String, SecretKey> sharedAESKeyCache = new ConcurrentHashMap<>();

    // Cache 2: Public RSA Keys <UserID, PublicKey>
    private static final Map<Integer, PublicKey> publicRSAKeyCache = new ConcurrentHashMap<>();

    /**
     * Generates a consistent conversation ID string from two user IDs.
     */
    public static String getConversationID(int user1, int user2) {
        if (user1 <= 0 || user2 <= 0) {
            return null;
        }
        return Math.min(user1, user2) + "_" + Math.max(user1, user2);
    }

    // --- Shared AES Key Cache Methods ---
    /**
     * Stores a shared AES key in the cache and saves it to file.
     */
    public static void cacheAndSaveSharedAESKey(int user1, int user2, SecretKey key) {
        String convId = getConversationID(user1, user2);
        if (convId != null && key != null) {
            sharedAESKeyCache.put(convId, key);
            System.out.println("KeyService: Cached shared AES key for conversation " + convId);
            CryptoService.saveSharedAESKeyToFile(user1, user2, key);
        }
    }

    /**
     * Gets a shared AES key from cache or loads from file if not cached.
     */
    public static SecretKey getSharedAESKey(int user1, int user2) {
        String convId = getConversationID(user1, user2);
        if (convId == null) {
            return null;
        }
        SecretKey cachedKey = sharedAESKeyCache.get(convId);
        if (cachedKey != null) {
            return cachedKey;
        }
        SecretKey loadedKey = CryptoService.loadSharedAESKeyFromFile(user1, user2);
        if (loadedKey != null) {
            sharedAESKeyCache.put(convId, loadedKey); // Add to cache
            return loadedKey;
        }
        System.out.println("KeyService: Shared AES key for " + convId + " not found.");
        return null;
    }

    // --- Public RSA Key Cache Methods (Keep for Key Exchange) ---
    public static void cachePublicRSAKey(Integer userId, PublicKey key) {
        if (userId != null && key != null && userId > 0) {
            publicRSAKeyCache.put(userId, key);
            System.out.println("KeyService: Cached public RSA key for user " + userId);
        }
    }

    public static PublicKey getPublicRSAKey(Integer userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        return publicRSAKeyCache.get(userId);
    }

    public static boolean isPublicRSAKeyCached(Integer userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        return publicRSAKeyCache.containsKey(userId);
    }

    // --- Cache Clearing ---
    public static void clearCache() {
        sharedAESKeyCache.clear();
        publicRSAKeyCache.clear();
        System.out.println("KeyService: Cleared all key caches.");
    }
}
