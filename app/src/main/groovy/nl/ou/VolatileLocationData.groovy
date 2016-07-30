package nl.ou

import groovy.transform.CompileStatic
import nl.ou.s3.common.LocationDto

/**
 * Bevat de laatst waargenomen en geaccepteerde locatie-data.
 */
@CompileStatic
class VolatileLocationData {

    static volatile LocationDto estimatedLocation

}
