package nl.ou.s3.common

import groovy.transform.CompileStatic

/**
 * Deze klasse implementeert de benodigde gegevens & bewerkingen voor de expiry-policy.
 */
@CompileStatic
class S3ExpirationPolicy implements Serializable {

    /**
     * Timestamp die verloopdatum & -tijd voor een selfie bevat.
     */
    Date expiryTimestamp

}
