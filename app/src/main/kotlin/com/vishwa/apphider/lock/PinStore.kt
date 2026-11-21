package com.vishwa.apphider.lock

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class PinStore(context: Context) {

    private val prefs = context.getSharedPreferences("pin", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun hasPin(): Boolean =
        prefs.getString(KEY_SALT, null) != null && prefs.getString(KEY_HASH, null) != null

    fun setPin(pin: String) {
        require(pin.length in 4..6 && pin.all(Char::isDigit))
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, salt.encode())
            .putString(KEY_HASH, mac(salt, pin).encode())
            .apply()
    }

    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.decode() ?: return false
        val expected = prefs.getString(KEY_HASH, null)?.decode() ?: return false
        return MessageDigest.isEqual(expected, mac(salt, pin))
    }

    private fun mac(salt: ByteArray, pin: String): ByteArray {
        val key = (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: createKey()
        return Mac.getInstance("HmacSHA256").apply { init(key) }.doFinal(salt + pin.toByteArray())
    }

    private fun createKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setKeySize(256)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.DEFAULT)

    private fun String.decode(): ByteArray = Base64.decode(this, Base64.DEFAULT)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pin_hmac"
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
    }
}
