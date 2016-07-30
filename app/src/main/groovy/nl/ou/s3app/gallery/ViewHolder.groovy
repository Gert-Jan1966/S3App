package nl.ou.s3app.gallery

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.annotations.InjectView
import groovy.transform.CompileStatic

/**
 * View voor een thumbnail, met de <i>image</i> en de <i>title</i> van bijbehorende selfie.
 */
@CompileStatic
class ViewHolder {
    @InjectView ImageView image
    @InjectView TextView title

    ViewHolder(View view) {
        SwissKnife.inject(this, view)
    }

}
