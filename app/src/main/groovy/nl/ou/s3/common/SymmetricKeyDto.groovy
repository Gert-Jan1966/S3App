package nl.ou.s3.common

import groovy.transform.CompileStatic

/**
 * Klasse tbv opvoeren nieuwe symmetric key op S3Server
 */
@CompileStatic
class SymmetricKeyDto implements Serializable {

    String id

    String owner

    S3ExpirationPolicy expirationPolicy

    S3LocationPolicy locationPolicy

    String key

    @Override
    String toString() {
        "id = ${id}; owner = ${owner}; key = ${key}"
    }

}
