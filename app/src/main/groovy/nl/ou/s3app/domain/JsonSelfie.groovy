package nl.ou.s3app.domain

import groovy.transform.CompileStatic

/**
 * Deze klasse beschrijft het JSON formaat waarmee de selfie wordt opgeslagen.
 */
@CompileStatic
class JsonSelfie {

    /** Id van symmetrische key, bij de S3Server op te vragen. */
    String keyId

    /** Bevat de encrypted selfie. */
    byte[] selfie

}
