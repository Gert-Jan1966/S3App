package nl.ou.s3app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TimePicker
import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.annotations.Extra
import com.arasthel.swissknife.annotations.InjectView
import com.arasthel.swissknife.annotations.OnClick
import groovy.transform.CompileStatic
import nl.ou.s3.common.S3ExpirationPolicy
import nl.ou.s3.common.SymmetricKeyDto

/**
 * Invoerschermpje voor S3ExpirationPolicy data.
 */
@CompileStatic
class ExpiryActivity extends AppCompatActivity {

    private final String TAG = "ExpiryActivity"

    static final int REQUEST_LOCATION_POLICY_DATA = 3

    @Extra
    SymmetricKeyDto symmetricKeyDto

    @InjectView
    CheckBox useExpiryCheckBox

    @InjectView
    EditText dateText

    @InjectView
    EditText timeText

    Date startDate

    /** Hulpvariabelen voor de datum- en de tijdpickers. */
    private int mYear, mMonth, mDay, mHour, mMinute

    /**
     * Initialisatie ExpiryActivity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expiry)

        // Nodig voor de SwissKnife annotaties.
        SwissKnife.inject(this)
        SwissKnife.restoreState(this, savedInstanceState)
        SwissKnife.loadExtras(this)

        // Bewaar huidige datum in Date() formaat.
        startDate = new Date()

        // Bepaal huidige datum & timestamp.
        final Calendar c = Calendar.getInstance()
        mYear = c.get(Calendar.YEAR)
        mMonth = c.get(Calendar.MONTH)
        mDay = c.get(Calendar.DAY_OF_MONTH)
        mHour = c.get(Calendar.HOUR_OF_DAY)
        mMinute = c.get(Calendar.MINUTE)

        dateText.text = "${mDay}-${(mMonth + 1)}-${mYear}"
        timeText.text = "${mHour}:${mMinute}"
    }

    /**
     * Opbouwen & tonen datumpicker.
     */
    @OnClick(R.id.dateButton)
    void onClickDateButton() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, {
                DatePicker view, int year, int month, int dayOfMonth ->
                    dateText.text = "${dayOfMonth}-${(month + 1)}-${year}"

                    mYear = year
                    mMonth = month
                    mDay = dayOfMonth
                },
                mYear, mMonth, mDay)

        datePickerDialog.show()
    }

    /**
     * Opbouwen & tonen tijdpicker.
     */
    @OnClick(R.id.timeButton)
    void onClickTimeButton() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, {
                TimePicker view, int hourOfDay, int minute ->
                    timeText.text = "${hourOfDay}:${minute}"

                    mHour = hourOfDay
                    mMinute = minute
                },
                mHour, mMinute, true)

        timePickerDialog.show()
    }

    /**
     * Afhandelen Accepteren-button.<br>
     * De S3ExpirationPolicy wordt alleen doorgegeven als de checkbox is aangevinkt.
     */
    @OnClick(R.id.okButton)
    void onClickOkButton() {
        if (useExpiryCheckBox.checked) {
            def expirationDate = new Date(mYear - 1900, mMonth, mDay, mHour, mMinute, 0)

            // "Te kleine" datum opgegeven? Gebruik dan huidige datum/timestamp + 1 dag.
            if (expirationDate.before(startDate)) expirationDate = startDate.next()

            def s3ExpirationPolicy = new S3ExpirationPolicy(expiryTimestamp: expirationDate)
            symmetricKeyDto.expirationPolicy = s3ExpirationPolicy
        }

        // Hier de LocationActivity opstarten.
        def locationIntent = new Intent(this, LocationActivity)
        locationIntent.putExtra("symmetricKeyDto", symmetricKeyDto)
        startActivityForResult(locationIntent, REQUEST_LOCATION_POLICY_DATA)
    }

    /**
     * Afhandeling resultaten van door ExpiryActivity gestartte activities.
     */
    @Override
    void onActivityResult(final int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LOCATION_POLICY_DATA:
                if (resultCode == RESULT_OK) {
                    symmetricKeyDto = (SymmetricKeyDto) data.getSerializableExtra("symmetricKeyDto")
                    Intent returnData = new Intent()
                    returnData.putExtra("symmetricKeyDto", symmetricKeyDto)
                    setResult(RESULT_OK, returnData)
                }

                if (resultCode == RESULT_CANCELED) {
                    Intent returnData = new Intent()
                    setResult(RESULT_CANCELED, returnData)
                }
        }

        finish()
    }

}
