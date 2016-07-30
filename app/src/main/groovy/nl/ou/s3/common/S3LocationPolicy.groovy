package nl.ou.s3.common

import groovy.transform.CompileStatic

/**
 * Deze klasse implementeert de benodigde gegevens & bewerkingen voor de locatie-policy.
 */
@CompileStatic
class S3LocationPolicy implements Serializable {

    /**
     * De postcode waarbinnen de locatie zich moet bevinden.
     */
    String postalCode

}
