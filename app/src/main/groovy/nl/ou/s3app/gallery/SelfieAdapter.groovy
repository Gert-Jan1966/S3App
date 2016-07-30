package nl.ou.s3app.gallery

import android.app.Activity
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import groovy.transform.CompileStatic
import nl.ou.s3app.R

import static org.apache.commons.io.FilenameUtils.removeExtension

/**
 * Deze ArrayAdapter bevat een lijst met thumbnails.
 */
@CompileStatic
class SelfieAdapter extends ArrayAdapter<Uri> {
    private final Activity context
    private final LayoutInflater inflater
    private final List<Uri> thumbnails

    SelfieAdapter(Activity context, List<Uri> thumbnails) {
        super(context, R.layout.layout_selfie_listview, thumbnails)
        this.context = context
        this.thumbnails = thumbnails
        this.inflater = LayoutInflater.from(context)
    }

    /**
     * Aanmaken van een view (1 view per Adapter-item!) binnen de SelfieAdapter.
     */
    @Override
    View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder

        // Is er al een view? Zoniet, dan maak er er eentje aan.
        if (view) {
            holder = (ViewHolder) view.tag
        } else {
            view = inflater.inflate(R.layout.layout_selfie_listview, null, true)
            holder = new ViewHolder(view)
            view.tag = holder
        }

        File f = new File(thumbnails[position].encodedPath)

        // Vullen ViewHolder velden.
        holder.title.text = removeExtension(f.name)
        holder.image.imageBitmap = thumbnails[position].toString().asImage()

        view
    }

}
