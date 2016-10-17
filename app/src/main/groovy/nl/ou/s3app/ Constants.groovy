package nl.ou.s3app

import groovy.transform.CompileStatic
import okhttp3.MediaType

import java.text.SimpleDateFormat

/**
 * Definities van constanten.
 */
@CompileStatic
class Constants {

    // Definities voor datums.
    static final String DATE_FORMAT = "yyyyMMdd_HHmmss_SSS"
    static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT)

    // Definities voor bestanden en paden.
    static final String S3_EXT = ".S3"
    static final String SELFIES_DIR = "selfies"
    static final String THUMBNAILS_DIR = "thumbnails"

    // Definities voor toegang tot de server
    static final String SERVER_IP = "https://145.20.144.40"
    static final String SERVER_PORT = "8080"    // 8443 voor HTTPS onder Tomcat, indien geconfigureerd.
    static final String SERVER_URL = "${SERVER_IP}:${SERVER_PORT}/S3Server"
    static final String SERVER_KEY_URL = "${SERVER_URL}/symmetrickey"

    // MIME type voor JSON.
    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8")

    // Te gebruiken algorime voor symmetrische encryptie.
    static final String CIPHER_ALGORITHM = "AES"

}
