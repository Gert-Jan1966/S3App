package nl.ou.s3app

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.StrictMode
import android.util.Log
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import nl.ou.VolatileLocationData
import nl.ou.s3.common.LocationDto
import nl.ou.s3.common.SymmetricKeyDto
import nl.ou.s3app.domain.JsonSelfie
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

import static org.apache.commons.io.FilenameUtils.removeExtension

/**
 * Dialog voor tonen van een selfie.
 */
@CompileStatic
class SelfieDialog extends Dialog {

    private final String TAG = "SelfieDialog"

    /** De HttpClient t.b.v. communicatie met de server. */
    private final OkHttpClient client = new OkHttpClient()

    /** Utility class met encryptie/decryptie methods. */
    private final CipherModule cipherModule = CipherModule.instance

    /**
     * Constructor.
     */
    SelfieDialog(Context context, List<Uri> thumbnails, int position) {
        super(context)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.layout_dialog)

        // Networkcalls in UI-thread!
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build())

        // Bepaal S3-bestandsnaam vanuit het path van de geselecteerde thumbnail.
        def fileName = removeExtension(thumbnails[position].path)
        fileName = fileName.replace(Constants.THUMBNAILS_DIR, Constants.SELFIES_DIR) + Constants.S3_EXT

        // Inlezen & parsen S3 bestand.
        JsonSelfie jsonSelfie = new ObjectMapper().readValue(new File(fileName), JsonSelfie)

        // Haal de symKey op van de server & vervolg deze method alleen indien symKey gevonden wordt.
        SecretKey symKey = retrieveSymKeyFromServer(jsonSelfie.keyId)
        if (!symKey) return

        // Ontsleutel de selfie en leeg daarna de gebruikte buffer van het selfiebestand.
        byte[] decryptedSelfie = cipherModule.decrypt(jsonSelfie.selfie, symKey)
        jsonSelfie = null

        // Maak de view en toon de selfie. Leeg daarna expliciet de selfiebuffer.
        def selfieView = (ImageView) findViewById(R.id.selfieView)
        selfieView.imageBitmap = BitmapFactory.decodeByteArray(decryptedSelfie, 0, decryptedSelfie.length)
        decryptedSelfie = null
    }

    /**
     * Ophalen gewenste symmetric key van server o.b.v. verstrekte <i>symKeyId</i>.
     *
     * @param symKeyId id van gewenste symmetric key.
     * @return Gewenste symmetric key; null indien een fout optreedt,
     *         waarbij de foutmelding via een TextView wordt getoond.
     */
    private SecretKey retrieveSymKeyFromServer(String symKeyId) {

        // Ophalen meest recente locatie.
        LocationDto locationDto = VolatileLocationData.estimatedLocation
        if (!locationDto) {
            throw new IllegalStateException("S3App heeft niet de beschikking over een geldige locatie!")
        }

        // Probeer de symmetrische key op te halen a.d.h.v. z'n id en de gevonden locatie.
        def jsonMessage = new ObjectMapper().writeValueAsString(locationDto)
        Log.d(TAG, jsonMessage)
        def keyRequest = new Request.Builder()
                .url("${Constants.SERVER_KEY_URL}/${symKeyId}")
                .post(RequestBody.create(Constants.JSON, jsonMessage))
                .build()

        // Verwerk de ontvangen response.
        Response getResponse = client.newCall(keyRequest).execute()
        if (!getResponse.successful) {
            def errorTextView = (TextView) findViewById(R.id.errorTextView)
            errorTextView.text = getResponse.peekBody(200L).string()
            return null
        }

        // Herstel de te gebruiken symmetrische key.
        def symKeyDto = new ObjectMapper().readValue(getResponse.body().bytes(), SymmetricKeyDto)
        new SecretKeySpec(symKeyDto.key.decodeBase64(), Constants.CIPHER_ALGORITHM)
    }

}
