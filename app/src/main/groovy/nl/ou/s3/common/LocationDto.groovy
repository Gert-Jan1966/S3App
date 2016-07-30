package nl.ou.s3.common

import groovy.transform.CompileStatic

/**
 * Klasse voor transport van locatiegegevens.<br>
 * De attributen van deze klasse worden gemapped vanuit het <i>android.location.Location</i> object.
 */
@CompileStatic
class LocationDto implements Serializable {

    /** Tijdstip vaststelling locatie. */
    Long time

    /** Lengte- en breedtecoordinaten. */
    Double latitude
    Double longitude

//    /** Hoogtecoordinaat. */
//    Boolean hasAltitude
//    Double altitude

    /** Nauwkeurigheid. */
//    Boolean hasAccuracy
//    Float accuracy

    @Override
    String toString() {
        "Time: ${new Date(time)}, Latitude: ${latitude}, Longitude: ${longitude}"
    }

}
