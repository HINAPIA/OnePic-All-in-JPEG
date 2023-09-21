package com.goldenratio.onepic
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.security.SecureRandom

object EncryptionModule { // 싱글톤

    /** 암호화와 복호화에 쓰이는 key, iv 값은 동일해야한다 */

    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): String { // 데이터 암호화
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encryptedBytes = cipher.doFinal(data)
        val cipherText = encryptedBytes + iv
        return Base64.encodeToString(cipherText, Base64.DEFAULT)
    }

    fun decrypt(data: String, key: ByteArray, iv: ByteArray): ByteArray { // 데이터 복호화
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        val cipherText = Base64.decode(data, Base64.DEFAULT)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(cipherText.copyOfRange(0, cipherText.size - 12))
    }

    fun generateRandomIV(): ByteArray { // 암호화와 복호화에 쓰이는 IV(초기 벡터) - 랜덤 생성
        val iv = ByteArray(12)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(iv)
        return iv
    }
}