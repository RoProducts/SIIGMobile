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
import it.geosolutions.android.map.model.Layer;
import it.geosolutions.android.map.model.query.FeatureInfoQueryResult;
import it.geosolutions.android.map.renderer.LegendRenderer;
import it.geosolutions.android.map.spatialite.SpatialiteLayer;

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
 * Adapter for a row of layers from a <ArrayList> of <FeatureInfoQueryResult>
 * The implementation show legend and name.
 * 
 * @author Lorenzo Natali (www.geo-solutions.it)
 */
public class FeatureInfoLayerAdapter extends
        ArrayAdapter<FeatureInfoQueryResult> {

int resourceId = R.layout.feature_info_layer_list_row;

/**
 * The constructor gets the resource (id of the layout for the element)
 * 
 * @param context
 * @param resource
 */
public FeatureInfoLayerAdapter(Context context, int resource) {
    super(context, resource);
    this.resourceId = resource;
}

/**
 * Create the adapter and populate it with a list of <FeatureInfoQueryResult>
 * 
 * @param context
 * @param resource feature_info_layer_list_row
 * @param layers feature_layer_name
 */
public FeatureInfoLayerAdapter(AppCompatActivity context, int resource,
        ArrayList<FeatureInfoQueryResult> layers) {
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
    FeatureInfoQueryResult result = getItem(position);

    if (result != null) {

        // This is how you obtain a reference to the TextViews.
        // These TextViews are created in the XML files we defined.
        // TODO use ViewHolder
        // display name
    	Layer<?> layer= result.getLayer();
        TextView name = (TextView) v.findViewById(R.id.feature_layer_name);
        if (name != null) {
            name.setText(result.getLayer().getTitle());
        }
        // display legend
        ImageView legend = (ImageView) v.findViewById(R.id.legend);
        if (legend != null) {
        	if(layer instanceof SpatialiteLayer){
        		SpatialiteLayer l = (SpatialiteLayer)layer;
        		 legend.setImageDrawable(new BitmapDrawable(getContext()
                         .getResources(), LegendRenderer.getLegend(l.getTableName() )));
        	}
           
        }
    }

    // the view must be returned to our activity
    return v;

}

}
