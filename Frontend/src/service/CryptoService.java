package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.Map;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Service for E2EE operations.
 * Manages RSA key pair (LỚP 2 - Mã hóa bằng Password).
 * Manages shared AES keys (LỚP 1 - Lưu file cho Chat Nhóm Tạm thời).
 */
public class CryptoService {

    // --- RSA Constants ---
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int RSA_KEY_SIZE = 2048;

    // --- AES Constants (Message Encryption) ---
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_MSG_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_MSG_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    // --- PBKDF2 Constants (Password-based Key) ---
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    
    // --- AES Constants (Private Key Encryption) ---
    private static final String AES_KEY_ENCRYPTION_CIPHER = "AES/GCM/NoPadding";

    // --- Key Storage ---
    private static KeyPair userRsaKeyPair; // Chỉ lưu trong bộ nhớ khi đã đăng nhập
    private static final Path KEY_DIR = Paths.get("keys"); // Dùng cho AES keys chung (Chat nhóm)
    private static int currentUserId = -1;

    /**
     * ✅ LỚP 2: Khởi tạo (load) Private Key từ server data.
     */
    public static boolean initialize(int userId, String password, String encPrivateKeyBase64, String saltBase64, String ivBase64) {
        if (userId <= 0) return false;
        
        System.out.println("CryptoService: Đang giải mã Private Key cho user " + userId + "...");
        try {
            // 1. Giải mã Private Key
            PrivateKey privateKey = decryptPrivateKey(password, encPrivateKeyBase64, saltBase64, ivBase64);
            
            if (privateKey == null) {
                 System.err.println("CryptoService: Giải mã Private Key thất bại! (Sai mật khẩu hoặc dữ liệu lỗi?)");
                 return false;
            }
            
            // 2. Tái tạo Public Key từ Private Key
            KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
            // Sử dụng RSAPrivateCrtKeySpec (quan trọng)
            RSAPrivateCrtKeySpec privKeySpec = kf.getKeySpec(privateKey, RSAPrivateCrtKeySpec.class);
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(privKeySpec.getModulus(), privKeySpec.getPublicExponent());
            PublicKey publicKey = kf.generatePublic(pubKeySpec);

            userRsaKeyPair = new KeyPair(publicKey, privateKey);
            currentUserId = userId;
            System.out.println("CryptoService: Tải và giải mã KeyPair thành công cho user " + userId);
            return true;

        } catch (Exception e) {
             System.err.println("CryptoService: Lỗi nghiêm trọng khi khởi tạo: " + e.getMessage());
             e.printStackTrace();
             return false;
        }
    }
    
    /**
     * ✅ LỚP 2: Dùng khi đăng ký. Tạo cặp khóa RSA VÀ mã hóa Private Key bằng password.
     */
    public static Map<String, String> generateAndEncryptKeys(String password) {
        try {
            KeyPair rsaKeyPair = generateRSAKeyPair();
            if (rsaKeyPair == null) throw new Exception("Không thể tạo RSA key pair");

            String publicKeyBase64 = publicKeyToString(rsaKeyPair.getPublic());
            byte[] privateKeyBytes = rsaKeyPair.getPrivate().getEncoded();

            byte[] salt = generateSalt();
            SecretKey aesKey = getKeyFromPassword(password, salt);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(AES_KEY_ENCRYPTION_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);
            
            byte[] encryptedPrivateKeyBytes = cipher.doFinal(privateKeyBytes);
            
            // 4. Ghép IV + Ciphertext (cho private key)
            // (Backend không cần IV riêng nếu chúng ta ghép nó)
            byte[] ivAndEncPrivateKey = new byte[GCM_IV_LENGTH + encryptedPrivateKeyBytes.length];
            System.arraycopy(iv, 0, ivAndEncPrivateKey, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedPrivateKeyBytes, 0, ivAndEncPrivateKey, GCM_IV_LENGTH, encryptedPrivateKeyBytes.length);

            return Map.of(
                "publicKey", publicKeyBase64,
                "encPrivateKey", Base64.getEncoder().encodeToString(ivAndEncPrivateKey), // IV + Encrypted Key
                "salt", Base64.getEncoder().encodeToString(salt),
                "iv", Base64.getEncoder().encodeToString(iv) // Gửi IV riêng (Backend yêu cầu)
            );

        } catch (Exception e) {
             System.err.println("CryptoService: Lỗi khi tạo và mã hóa keys: " + e.getMessage());
             e.printStackTrace();
             return null;
        }
    }
    
    /**
     * ✅ LỚP 2: Giải mã Private Key (được lưu trên server) bằng mật khẩu.
     */
    private static PrivateKey decryptPrivateKey(String password, String encPrivateKeyBase64, String saltBase64, String ivBase64_UNUSED) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            SecretKey aesKey = getKeyFromPassword(password, salt);

            // Gói dữ liệu (IV + Encrypted Private Key)
            byte[] ivAndEncPrivateKey = Base64.getDecoder().decode(encPrivateKeyBase64);
            
            if (ivAndEncPrivateKey.length < GCM_IV_LENGTH) return null;
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(ivAndEncPrivateKey, 0, iv, 0, GCM_IV_LENGTH);
            int encKeyLength = ivAndEncPrivateKey.length - GCM_IV_LENGTH;
            byte[] encKeyBytes = new byte[encKeyLength];
            System.arraycopy(ivAndEncPrivateKey, GCM_IV_LENGTH, encKeyBytes, 0, encKeyLength);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv); 
            Cipher cipher = Cipher.getInstance(AES_KEY_ENCRYPTION_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);
            
            byte[] privateKeyBytes = cipher.doFinal(encKeyBytes);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
            return kf.generatePrivate(keySpec);

        } catch (Exception e) {
            return null; // Lỗi (sai mật khẩu, v.v.)
        }
    }

    /** Tạo Salt ngẫu nhiên */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /** Dẫn xuất khóa AES từ mật khẩu và salt (PBKDF2) */
    private static SecretKey getKeyFromPassword(String password, byte[] salt) 
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    }

    // --- RSA Key Getters ---
    public static PrivateKey getPrivateKey() { if (userRsaKeyPair == null || currentUserId <= 0) { System.err.println("CryptoService: PrivateKey requested but keys not initialized!"); return null; } return userRsaKeyPair.getPrivate(); }
    public static PublicKey getPublicKey() { if (userRsaKeyPair == null || currentUserId <= 0) { System.err.println("CryptoService: PublicKey requested but keys not initialized!"); return null; } return userRsaKeyPair.getPublic(); }

    // --- RSA Key Generation (Public) ---
    public static KeyPair generateRSAKeyPair() {
        try { KeyPairGenerator g = KeyPairGenerator.getInstance(RSA_ALGORITHM); g.initialize(RSA_KEY_SIZE, new SecureRandom()); return g.generateKeyPair(); }
        catch (NoSuchAlgorithmException e) { e.printStackTrace(); return null; }
    }

    // --- RSA Encryption/Decryption (Dùng cho trao đổi khóa AES) ---
    public static String encryptRSA(String plainText, PublicKey publicKey) {
        if (plainText == null || publicKey == null) return null;
        try { Cipher c = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION); c.init(Cipher.ENCRYPT_MODE, publicKey); byte[] encBytes = c.doFinal(plainText.getBytes(StandardCharsets.UTF_8)); return Base64.getEncoder().encodeToString(encBytes); }
        catch (Exception e) { System.err.println("RSA Encrypt Error: " + e.getMessage()); return null; }
    }
    public static String decryptRSA(String encryptedBase64, PrivateKey privateKey) {
        if (encryptedBase64 == null || privateKey == null) return null;
        try { byte[] encBytes = Base64.getDecoder().decode(encryptedBase64); Cipher c = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION); c.init(Cipher.DECRYPT_MODE, privateKey); byte[] decBytes = c.doFinal(encBytes); return new String(decBytes, StandardCharsets.UTF_8); }
        catch (IllegalArgumentException | BadPaddingException e) { return null; }
        catch (Exception e) { System.err.println("RSA Decrypt Error: " + e.getMessage()); return null; }
    }

    // --- RSA Key String Conversion ---
    public static String publicKeyToString(PublicKey publicKey) { if (publicKey == null) return null; return Base64.getEncoder().encodeToString(publicKey.getEncoded()); }
    public static PublicKey stringToPublicKey(String keyBase64) { if (keyBase64 == null || keyBase64.isEmpty()) return null; try { byte[] b = Base64.getDecoder().decode(keyBase64); X509EncodedKeySpec s = new X509EncodedKeySpec(b); KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM); return kf.generatePublic(s); } catch (Exception e) { return null; } }
    
    public static void clearKeys() {
        userRsaKeyPair = null;
        currentUserId = -1;
        System.out.println("CryptoService: Cleared loaded RSA key pair.");
        KeyService.clearCache(); // Xóa cả cache AES
    }

    // ===========================================
    // === AES Methods for Messages (Lớp 2) ===
    // ===========================================
    
    /** ✅ LỚP 2: Tạo khóa AES (dùng cho tin nhắn) */
    public static SecretKey generateAESKey_Session() { 
        try { 
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM); 
            keyGen.init(AES_MSG_KEY_SIZE, new SecureRandom()); 
            return keyGen.generateKey(); 
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); return null; } 
    }
    
    /** ✅ LỚP 2: Chuyển AES Key thành String */
    public static String aesKeyToString(SecretKey secretKey) { 
        if (secretKey == null) return null; 
        return Base64.getEncoder().encodeToString(secretKey.getEncoded()); 
    }
    
    /** ✅ LỚP 2: Chuyển String thành AES Key */
    public static SecretKey stringToAESKey(String keyBase64) { 
        if (keyBase64 == null || keyBase64.isEmpty()) return null; 
        try { 
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64); 
            return new SecretKeySpec(keyBytes, 0, keyBytes.length, AES_ALGORITHM); 
        } catch (IllegalArgumentException e) { return null; } 
    }

    /** ✅ LỚP 2: Mã hóa AES, trả về Map {"iv", "cipherText"} */
    public static Map<String, String> aesEncrypt(String plainText, SecretKey secretKey) {
        if (plainText == null || secretKey == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH]; new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(AES_MSG_CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            return Map.of(
                "iv", Base64.getEncoder().encodeToString(iv),
                "cipherText", Base64.getEncoder().encodeToString(cipherTextBytes)
            );
        } catch (Exception e) { System.err.println("AES/GCM Encryption Error: " + e.getMessage()); return null; }
    }
    
    /** ✅ LỚP 2: Giải mã AES, nhận iv và cipherText */
    public static String aesDecrypt(String cipherTextBase64, String ivBase64, SecretKey secretKey) {
        if (cipherTextBase64 == null || ivBase64 == null || secretKey == null) return null;
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] cipherText = Base64.getDecoder().decode(cipherTextBase64);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(AES_MSG_CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | AEADBadTagException e) { return null; }
        catch (Exception e) { return null; }
    }

    // ===========================================
    // === AES Methods for Shared Chat (Lớp 1 - Chat Nhóm) ===
    // ===========================================
    
    // (Vẫn giữ logic lưu/tải file key AES chung, vì flow Lớp 2 không
    // mô tả cách hoạt động của chat nhóm E2EE. Chúng ta sẽ dùng cách này cho chat nhóm)
    
    public static void saveSharedAESKeyToFile(int user1, int user2, SecretKey aesKey) { if (aesKey == null || user1 <= 0 || user2 <= 0) return; String keyString = aesKeyToString(aesKey); if (keyString == null) return; String filename = "shared_" + Math.min(user1, user2) + "_" + Math.max(user1, user2) + ".aeskey"; Path keyPath = KEY_DIR.resolve(filename); try { if (!Files.exists(KEY_DIR)) Files.createDirectories(KEY_DIR); Files.writeString(keyPath, keyString, StandardCharsets.UTF_8); System.out.println("CryptoService: Saved shared AES key to " + filename); } catch (IOException e) { System.err.println("CryptoService: Error saving shared AES key: " + e.getMessage()); } }
    public static SecretKey loadSharedAESKeyFromFile(int user1, int user2) { if (user1 <= 0 || user2 <= 0) return null; String filename = "shared_" + Math.min(user1, user2) + "_" + Math.max(user1, user2) + ".aeskey"; Path keyPath = KEY_DIR.resolve(filename); if (Files.exists(keyPath)) { try { String keyString = Files.readString(keyPath, StandardCharsets.UTF_8); SecretKey key = stringToAESKey(keyString); if (key != null) { System.out.println("CryptoService: Loaded shared AES key from " + filename); return key; } } catch (IOException e) { System.err.println("CryptoService: Error loading shared AES key: " + e.getMessage()); } } else { System.out.println("CryptoService: Shared AES key file not found: " + filename); } return null; }
    public static String aesEncrypt_Shared(String plainText, SecretKey secretKey) { if (plainText == null || secretKey == null) return null; try { byte[] iv = new byte[GCM_IV_LENGTH]; new SecureRandom().nextBytes(iv); GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv); Cipher cipher = Cipher.getInstance(AES_MSG_CIPHER_TRANSFORMATION); cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec); byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)); byte[] ivAndCipherText = new byte[GCM_IV_LENGTH + cipherText.length]; System.arraycopy(iv, 0, ivAndCipherText, 0, GCM_IV_LENGTH); System.arraycopy(cipherText, 0, ivAndCipherText, GCM_IV_LENGTH, cipherText.length); return Base64.getEncoder().encodeToString(ivAndCipherText); } catch (Exception e) { System.err.println("AES/GCM Encryption Error: " + e.getMessage()); return null; } }
    public static String aesDecrypt_Shared(String encryptedIvAndCipherTextBase64, SecretKey secretKey) { if (encryptedIvAndCipherTextBase64 == null || secretKey == null) return null; try { byte[] ivAndCipherText = Base64.getDecoder().decode(encryptedIvAndCipherTextBase64); if (ivAndCipherText.length < GCM_IV_LENGTH) return null; byte[] iv = new byte[GCM_IV_LENGTH]; System.arraycopy(ivAndCipherText, 0, iv, 0, GCM_IV_LENGTH); int cipherTextLength = ivAndCipherText.length - GCM_IV_LENGTH; byte[] cipherText = new byte[cipherTextLength]; System.arraycopy(ivAndCipherText, GCM_IV_LENGTH, cipherText, 0, cipherTextLength); GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv); Cipher cipher = Cipher.getInstance(AES_MSG_CIPHER_TRANSFORMATION); cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec); byte[] decryptedBytes = cipher.doFinal(cipherText); return new String(decryptedBytes, StandardCharsets.UTF_8); } catch (IllegalArgumentException | AEADBadTagException e) { return null; } catch (Exception e) { return null; } }
}