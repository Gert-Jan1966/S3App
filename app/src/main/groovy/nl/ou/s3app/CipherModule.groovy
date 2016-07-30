package nl.ou.s3app

import groovy.transform.CompileStatic

import javax.crypto.Cipher
import javax.crypto.SecretKey

/**
 * Module voor uitvoering van symmetrische encryptie en decryptie.
 */
@CompileStatic
@Singleton
class CipherModule {

    private final String TAG = "CipherModule"

    /**
     * Decrypten van gegeven bytearray m.b.v. gegeven <i>secretKey</i>.
     *
     * @param input Te decrypten bytearray.
     * @param secretKey Key gegenereerd volgens <i>Constants.CIPHER_ALGORITHM</i>.
     * @return Decrypted bytearray.
     */
    byte[] decrypt(byte[] input, SecretKey secretKey) {
        if (!input || !secretKey) {
            throw new IllegalArgumentException("Alle argumenten voor CipherModule.decrypt() zijn verplicht!")
        }

        doWork(input, secretKey, Cipher.DECRYPT_MODE)
    }

    /**
     * Encrypten van gegeven bytearray m.b.v. gegeven <i>secretKey</i>.
     *
     * @param input Te encrypten bytearray.
     * @param secretKey Key gegenereerd volgens <i>Constants.CIPHER_ALGORITHM</i>.
     * @return Encrypted bytearray.
     */
    byte[] encrypt(byte[] input, SecretKey secretKey) {
        if (!input || !secretKey) {
            throw new IllegalArgumentException("Alle argumenten voor CipherModule.encrypt() zijn verplicht!")
        }

        doWork(input, secretKey, Cipher.ENCRYPT_MODE)
    }

    /**
     * Daadwerkelijke uitvoering van de en- of decryptie.
     *
     * @param input De te verwerken bytearray.
     * @param secretKey Key gegenereerd volgens <i>Constants.CIPHER_ALGORITHM</i>.
     * @param cipherMode Mogelijke waarden: <i>Cipher.DECRYPT_MODE</i> of <i>Cipher.DECRYPT_MODE</i>.
     * @return Encrypted of decrypted bytearray, afhankelijk van <i>cipherMode</i>.
     */
    private byte[] doWork(byte[] input, SecretKey secretKey, int cipherMode) {
        if ( ![Cipher.DECRYPT_MODE, Cipher.ENCRYPT_MODE].contains(cipherMode) ) {
            throw new IllegalArgumentException(
                    "Argument cipherMode moet Cipher.DECRYPT_MODE of Cipher.ENCRYPT_MODE zijn!")
        }

        Cipher cipher = Cipher.getInstance(Constants.CIPHER_ALGORITHM)
        cipher.init(cipherMode, secretKey)
        cipher.doFinal(input)
    }

}
