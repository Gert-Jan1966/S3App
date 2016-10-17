package nl.ou.s3app

import android.accounts.Account
import android.accounts.AccountManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.annotations.InjectView
import com.arasthel.swissknife.annotations.OnBackground
import com.arasthel.swissknife.annotations.OnItemClick
import com.arasthel.swissknife.annotations.OnItemLongClick
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import nl.ou.s3.common.SymmetricKeyDto
import nl.ou.s3app.domain.JsonSelfie
import nl.ou.s3app.gallery.SelfieAdapter
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.util.regex.Pattern

import static android.graphics.Bitmap.createScaledBitmap
import static org.apache.commons.io.FilenameUtils.removeExtension

/**
 * De belangrijkste Activity binnen S3App.
 */
@CompileStatic
class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity"

    // Requestcodes voor startActivity() of startActivityForResult().
    static final int REQUEST_IMAGE_CAPTURE = 1
    static final int REQUEST_EXPIRATION_POLICY_DATA = 2

    /** Utility class met encryptie/decryptie methods. */
    private final CipherModule cipherModule = CipherModule.instance

    /** De HttpClient t.b.v. communicatie met de server. */
    private final OkHttpClient client = new OkHttpClient()

    /** Intent voor activeren LocationService. */
    private Intent locationServiceIntent

    /** Applicatiepath voor de selfies. */
    private File selfiesPath

    /** Applicatiepath voor de thumbnails. */
    private File thumbnailsPath

    /** Hulpvariabele voor locatie van actuele selfie. */
    private File tempSelfieFile

    /** Lijst voor bekende thumbnails. */
    private List<Uri> thumbnails = []

    /** Definitie t.b.v. JSON structuur voor communicatie met S3Server.  */
    private SymmetricKeyDto symmetricKeyDto

    /** View voor de gallery. */
    @InjectView
    private ListView selfieListView

    /** Android registratie-emailadres van de gebruiker. */
    private String userEmailAddress

    /**
     * Initialisatie van MainActivity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Starten van de LocationService.
        locationServiceIntent = new Intent(this, LocationService)
        startService(locationServiceIntent)

        // Laat Android networkcalls in de UI-thread accepteren!
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build())

        // Nodig voor de SwissKnife annotaties.
        SwissKnife.inject(this)
        SwissKnife.restoreState(this, savedInstanceState)

        // Vaststellen te gebruiken paths.
        selfiesPath = createAppDataDir(Constants.SELFIES_DIR)
        thumbnailsPath = createAppDataDir(Constants.THUMBNAILS_DIR)

        obtainAndDisplayUserEmailAddress()

        initSelfieAdadapter()
    }

    /**
     * Uitzetten locationService in Destroy fase van de Activity lifecycle.
     */
    @Override
    void onDestroy() {

        // Afbreken LocationService.
        stopService(locationServiceIntent)
        super.onDestroy()
    }

    /**
     * Toont dialog met daarin de gekozen selfie.
     */
    @OnItemClick(R.id.selfieListView)
    void onItemClick(int position) {
        try {
            new SelfieDialog(this, thumbnails, position, userEmailAddress).show()
        } catch (IllegalStateException ise) {
            Log.e(TAG, ise.message)

            // TODO: na afsluiten lijkt de app toch nog in de Android-applijst te staan.
            new AlertDialog.Builder(this)
                    .setMessage("${ise.message}\n\nS3App wordt afgesloten.")
                    .setPositiveButton(R.string.locationerror_dialog_ok, {
                        DialogInterface dialog, int whichButton ->
                            finish()
                    })
                    .create().show()
        }
    }

    /**
     * Biedt mogelijkheid tot verwijderen selfie.
     */
    @OnItemLongClick(R.id.selfieListView)
    boolean onItemLongClick(int position) {
        def fileName = removeExtension(new File(thumbnails[position].path).name)

        // Opbouwen & tonen delete-dialoog.
        new AlertDialog.Builder(this)
                .setMessage("Wilt u selfie '$fileName' verwijderen?")
                .setNegativeButton(R.string.delete_dialog_cancel, {
                    DialogInterface dialog, int whichButton -> dialog.cancel()
                })
                .setPositiveButton(R.string.delete_dialog_ok, {
                    DialogInterface dialog, int whichButton ->

                        // Verwijder thumbnail.
                        def toBeDeleted = new File(thumbnailsPath, fileName + ".jpg")
                        toBeDeleted.delete()

                        // Verwijder selfie.
                        toBeDeleted = new File(selfiesPath, fileName + Constants.S3_EXT)
                        String keyId = new ObjectMapper().readValue(toBeDeleted, JsonSelfie).keyId
                        toBeDeleted.delete()

                        // Verwijder symmetrische key bij verwijderde selfie.
                        deleteKeyFromServer(keyId)

                        // Melding naar gebruiker.
                        Toast.makeText(this, "Selfie '$fileName' is verwijderd!", Toast.LENGTH_SHORT).show()

                        // Gallery opnieuw genereren.
                        initSelfieAdadapter()
                })
            .create().show()

        true    // Event afgehandeld, niet doorgaan naar onItemClick()!
    }

    @Override
    boolean onCreateOptionsMenu(Menu menu) {
        menuInflater.inflate(R.menu.menu_main, menu)
        true
    }

    /**
     * Het menu heeft maar 1 optie: het camera-icon.
     */
    @Override
    boolean onOptionsItemSelected(MenuItem item) {
        invokeCamera()
        true
    }

    /**
     * Afhandeling resultaten van door MainActivity gestartte activities.
     */
    @Override
    void onActivityResult(final int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:    // Verwerken resultaat van nemen selfie.
                if (resultCode == RESULT_OK) {
                    createThumbnail()
                    createNewSelfie()

                    //Laat de activities voor de policies elkaar 'genest' aanroepen
                    def expiryIntent = new Intent(this, ExpiryActivity)
                    expiryIntent.putExtra("symmetricKeyDto", symmetricKeyDto)
                    startActivityForResult(expiryIntent, REQUEST_EXPIRATION_POLICY_DATA)
                } else {    // Opruimen ongewenst selfiebestand.
                    if (tempSelfieFile) {
                        tempSelfieFile.delete()
                    }
                }
                break

            case REQUEST_EXPIRATION_POLICY_DATA:    // Verwerken data in S3ExpirationPolicy.
                if (resultCode == RESULT_OK) {
                    symmetricKeyDto = (SymmetricKeyDto) data.getSerializableExtra("symmetricKeyDto")
                    updateKeyOnServer()
                }

                symmetricKeyDto = null
                initSelfieAdadapter()
                break

        }
    }

    /**
     * Initialiseer de SelfieAdapter waarbij de thumbnails van het bestandssysteem worden gelezen.
     * De thumbnails worden op 'leeftijd' gesorteerd.
     */
    private void initSelfieAdadapter() {
        thumbnails = []

        try {
            thumbnailsPath.listFiles().sort().reverse(true).each {
                File f -> thumbnails.add(Uri.fromFile(f))
            }
            selfieListView.adapter = new SelfieAdapter(this, thumbnails)
        } catch (Exception e) {
            e.printStackTrace()
        }

        selfieListView.invalidate()    // Geeft redraw van deze view.
    }

    /**
     * Neem de selfie m.b.v. een reeds geinstalleerde camera-app.
     */
    private void invokeCamera() {
        tempSelfieFile = null

        // Alloceren selfiebestand.
        try {
            tempSelfieFile = new File(selfiesPath, Constants.SIMPLE_DATE_FORMAT.format(new Date()) + ".jpg")
        } catch (IOException e) {
            tempSelfieFile = null
            e.printStackTrace()
        }

        // Stel vast of er een camera-app is & gebruik deze voor de te nemen selfie.
        // Geaccepteerde selfie wordt in gealloceerd bestand opgeslagen.
        def cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempSelfieFile))

        if (cameraIntent.resolveActivity(getPackageManager())) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    /**
     * Aanmaken nieuwe subdirectory binnen de afgeschermde opslagstructuur voor deze app.
     */
    private File createAppDataDir(String subDir) {
        File dataDir = new File(getExternalFilesDir(null), subDir)

        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        dataDir
    }

    /**
     * Aanmaken van een thumbnail. Als dit mislukt, wordt ook de selfie verwijderd.
     */
    private void createThumbnail() {
        File thumbnailFile = null

        try {
            Bitmap selfie = BitmapFactory.decodeFile(tempSelfieFile.absolutePath)

            // Afmetingen van de thumbnail hangen af van landscape of portrait mode.
            int thumbnailWidth = 64
            int thumbnailHeight = 48
            if (selfie.width < selfie.height) {
                thumbnailWidth = 48
                thumbnailHeight = 64
            }
            selfie = createScaledBitmap(selfie, thumbnailWidth, thumbnailHeight, false)

            // Herbruik de bestandsnaam van de selfie voor bijbehorende thumbnail.
            thumbnailFile = new File(thumbnailsPath, tempSelfieFile.name)

            // Sla de thumbnail op.
            def fOut = new FileOutputStream(thumbnailFile)
            selfie.compress(Bitmap.CompressFormat.JPEG, 80, fOut)
            fOut.close()
        } catch (IOException e) {

            // Opruimen loze thumbnail-/selfiebestanden.
            if (thumbnailFile) {
                thumbnailFile.delete()
            }
            if (tempSelfieFile) {
                tempSelfieFile.delete()
                tempSelfieFile = null
            }

            e.printStackTrace()
        }
    }

    /**
     * Ombouwen net genomen selfie naar een encoded en encrypted selfie in JSON formaat.
     */
    private void createNewSelfie() {
        createAndCommunicateSymmetricKey()
        createEncryptedEncodedSelfie()
    }

    /**
     * Genereer nieuwe symmetrische key & voer deze op in S3Server.
     *
     * @return Nieuwe symmetrische key + bijbehorend id in S3Server/S3ServerDb.
     */
    private void createAndCommunicateSymmetricKey() {

        // Generate new symmetric key.
        SecretKey symKey = KeyGenerator.getInstance(Constants.CIPHER_ALGORITHM).generateKey()
        String key = symKey.encoded.encodeBase64()

        symmetricKeyDto = new SymmetricKeyDto(key: key, owner: userEmailAddress)
        def jsonMessage = new ObjectMapper().writeValueAsString(symmetricKeyDto)

        def postRequest = new Request.Builder()
                .url("${Constants.SERVER_KEY_URL}?user=${userEmailAddress}")
                .post(RequestBody.create(Constants.JSON, jsonMessage))
                .build()

        Response postResponse = client.newCall(postRequest).execute()

        //TODO: Betere foutafhandeling toevoegen!
        if (!postResponse.successful) throw new IOException("Unexpected code " + postResponse)

        symmetricKeyDto = new ObjectMapper().readValue(postResponse.body().string(), SymmetricKeyDto)
    }

    /**
     * Encryptie van de tijdelijke selfie. De encrypted selfie wordt Base64 encoded opgeslagen in een nieuw
     * S3 bestand in JSON formaat. <br>
     * Het tijdelijke unencrypted selfiebestand en de gebruikte symmetrische key worden beide verwijderd.
     *
     * @param symmetricKeyDto met gevulde <i>id</i> en <i>key</i> velden.
     */
    private void createEncryptedEncodedSelfie() {
        if (!tempSelfieFile) return

        // Aanmaken nieuw S3 bestand in JSON formaat.
        def jsonWriter = new FileWriter(removeExtension(tempSelfieFile.absolutePath) + Constants.S3_EXT)

        // Lees de selfie in & encrypt deze met de symmetric key.
        SecretKey symmetricKey = new SecretKeySpec(symmetricKeyDto.key.decodeBase64(), Constants.CIPHER_ALGORITHM)

        // Zet de data t.b.v. het S3 JSON bestand klaar & schrijf deze data naar het S3 bestand.
        def jsonSelfie = new JsonSelfie(keyId: symmetricKeyDto.id,
                selfie: cipherModule.encrypt(tempSelfieFile.readBytes(), symmetricKey))
        new ObjectMapper().writeValue(jsonWriter, jsonSelfie)

        // Verwijder de ongewenste JPG selfie.
        tempSelfieFile.delete()
        tempSelfieFile = null
    }

    /**
     * Deze method is in eerste instantie bedoeld om S3 policydata aan reeds opgeslagen keys toe te voegen.
     */
    private void updateKeyOnServer() {
        def jsonMessage = new ObjectMapper().writeValueAsString(symmetricKeyDto)

        def putRequest = new Request.Builder()
                .url("${Constants.SERVER_KEY_URL}/${symmetricKeyDto.id}?user=${userEmailAddress}")
                .put(RequestBody.create(Constants.JSON, jsonMessage))
                .build()

        Response putResponse = client.newCall(putRequest).execute()

        //TODO: Betere foutafhandeling toevoegen!
        if (!putResponse.successful) throw new IOException("Unexpected code " + putResponse)
    }

    /**
     * Verwijder de symmetric key met opgegeven <i>keyId</i> van de server.
     */
    @OnBackground
    private void deleteKeyFromServer(String keyId) {
        def deleteRequest = new Request.Builder()
                .url("${Constants.SERVER_KEY_URL}/${keyId}?user=${userEmailAddress}")
                .delete()
                .build()
        client.newCall(deleteRequest).execute()
    }

    /**
     * Bepaal het emailaccount van de gebruiker (zoals google.com en gmail.com) en toon deze in een Toast.
     */
    private void obtainAndDisplayUserEmailAddress() {
        Pattern emailPattern = Patterns.EMAIL_ADDRESS
        Account[] accounts = AccountManager.get(this).getAccounts()
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                userEmailAddress = account.name
                Toast.makeText(this, "Geregistreerde gebruiker: ${userEmailAddress}", Toast.LENGTH_LONG).show()
                return
            }
        }

        Toast.makeText(this, "Geen registratie-emailadres aangetroffen!", Toast.LENGTH_LONG).show()
    }

}
