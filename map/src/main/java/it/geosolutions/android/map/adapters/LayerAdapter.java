/*
 * GeoSolutions map - Digital field mapping on Android based devices
 * Copyright (C) 2013  GeoSolutions (www.geo-solutions.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.android.map.adapters;

import it.geosolutions.android.map.R;
import it.geosolutions.android.map.renderer.LegendRenderer;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Lorenzo Natali (www.geo-solutions.it)
 */
public class LayerAdapter extends ArrayAdapter<String> {

/*
 * here we must override the constructor for ArrayAdapter the only variable we
 * care about now is ArrayList<Item> objects, because it is the list of objects
 * we want to display.
 */

/*
 * we are overriding the getView method here - this is what defines how each
 * list item will look.
 */
int resourceId = R.layout.feature_info_layer_list_row;

/**
 * @param context
 * @param resource
 */
public LayerAdapter(Context context, int resource) {
    super(context, resource);
    this.resourceId = resource;
}

/**
 * @param context
 * @param resource feature_info_layer_list_row
 * @param layers
 */
public LayerAdapter(AppCompatActivity context, int resource,
        ArrayList<String> layers) {
    super(context, resource, layers);
    this.resourceId = resource;
}

public View getView(int position, View convertView, ViewGroup parent) {

    // assign the view we are converting to a local variable
    View v = convertView;

    // first check to see if the view is null. if so, we have to inflate it.
    // to inflate it basically means to render, or show, the view.
    if (v == null) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(resourceId, null);
    }

    /*
     * Recall that the variable position is sent in as an argument to this
     * method. The variable simply refers to the position of the current object
     * in the list. (The ArrayAdapter iterates through the list we sent it)
     * Therefore, i refers to the current Item object.
     */
    String layer = getItem(position);

    if (layer != null) {

        // This is how you obtain a reference to the TextViews.
        // These TextViews are created in the XML files we defined.
        TextView name = (TextView) v.findViewById(R.id.feature_layer_name);
        if (name != null) {
            name.setText(layer);
        }
        ImageView legend = (ImageView) v.findViewById(R.id.legend);
        if (legend != null) {
            legend.setImageDrawable(new BitmapDrawable(getContext()
                    .getResources(), LegendRenderer.getLegend(layer)));
        }
    }

    // the view must be returned to our activity
    return v;

}

}
