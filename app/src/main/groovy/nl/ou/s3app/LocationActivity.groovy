package nl.ou.s3app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.annotations.Extra
import com.arasthel.swissknife.annotations.InjectView
import com.arasthel.swissknife.annotations.OnClick
import groovy.transform.CompileStatic
import nl.ou.s3.common.S3LocationPolicy
import nl.ou.s3.common.SymmetricKeyDto

/**
 * Invoerschermpje voor S3LocationPolicy data.
 */
@CompileStatic
class LocationActivity extends AppCompatActivity {

    private final String TAG = "LocationActivity"

    @Extra
    SymmetricKeyDto symmetricKeyDto

    @InjectView
    CheckBox useLocationCheckBox

    @InjectView
    EditText postcodeEditText

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        // Nodig voor de SwissKnife annotaties.
        SwissKnife.inject(this)
        SwissKnife.restoreState(this, savedInstanceState)
        SwissKnife.loadExtras(this)
    }

    /**
     * Afhandelen Accepteren-button.
     */
    @OnClick(R.id.okButton)
    void onClickOkButton() {
        if (useLocationCheckBox.checked) {
            def postalCode = postcodeEditText.text.toString()
            symmetricKeyDto.locationPolicy = new S3LocationPolicy(postalCode: postalCode)
        }

        Intent data = new Intent()
        data.putExtra("symmetricKeyDto", symmetricKeyDto)
        setResult(RESULT_OK, data)

        finish()
    }

}
