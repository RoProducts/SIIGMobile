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
package it.geosolutions.android.map.fragment.featureinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.geosolutions.android.map.BuildConfig;
import it.geosolutions.android.map.R;
import it.geosolutions.android.map.activities.GetFeatureInfoLayerListActivity;
import it.geosolutions.android.map.adapters.FeatureInfoAttributesAdapter;
import it.geosolutions.android.map.common.Constants;
import it.geosolutions.android.map.loaders.FeatureInfoLoader;
import it.geosolutions.android.map.model.Feature;
import it.geosolutions.android.map.model.Layer;
import it.geosolutions.android.map.model.query.BBoxQuery;
import it.geosolutions.android.map.model.query.BaseFeatureInfoQuery;
import it.geosolutions.android.map.model.query.CircleQuery;
import it.geosolutions.android.map.model.query.FeatureInfoQueryResult;
import it.geosolutions.android.map.model.query.FeatureInfoTaskQuery;
import it.geosolutions.android.map.model.query.PolygonQuery;
import it.geosolutions.android.map.model.query.WMSGetFeatureInfoQuery;
import it.geosolutions.android.map.utils.FeatureInfoUtils;

/**
 * This fragment shows a view o the attributes of a single feature from a
 * selected layer Supports pagination and returns to the activity in case of
 * selection.
 *
 * @author Lorenzo Natali (www.geo-solutions.it)
 */
public class FeatureInfoAttributeListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<FeatureInfoQueryResult>> {

    // Tag for Logging
    private static String TAG = "FeatureInfoAttributeListFragment";

    public final static String PARAM_CUSTOM_LAYOUT = "custom_layout";
    public final static String PARAM_DONT_LOAD_TWICE= "dont_load_twice";

    private FeatureInfoAttributesAdapter adapter;

    private FeatureInfoTaskQuery[] queryQueue;

    // The callbacks through which we will interact with the LoaderManager.
    private LoaderManager.LoaderCallbacks<List<FeatureInfoQueryResult>> mCallbacks;

    protected Integer start;

    protected Integer limit;

    protected BaseFeatureInfoQuery query;

    protected ArrayList<Layer> layers;
    protected ArrayList<Feature> currentFeatures;
    private ProgressDialog pd;
    private boolean waitForInflation = false;

    private FeatureInfoLoadedListener mListener;
    private int mCustomLayout;
    private static FeatureInfoAttributeListFragment mInstance;

    public static FeatureInfoAttributeListFragment getInstance(){
        if(mInstance == null){
            mInstance = new FeatureInfoAttributeListFragment();
        }
        return mInstance;
    }

    /**
     * Called only once
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // view operations

        setRetainInstance(true);
        // start progress bars
        getActivity().setProgressBarIndeterminateVisibility(true);
        getActivity().setProgressBarVisibility(true);

        // legacy : get data from the intent
        if(getActivity() != null &&
                getActivity().getIntent() != null &&
                getActivity().getIntent().getExtras() != null &&
                getActivity().getIntent().getExtras().containsKey("query")) {

            Bundle extras = getActivity().getIntent().getExtras();

            layers = (ArrayList<Layer>) extras.getSerializable(Constants.ParamKeys.LAYERS);
            start = extras.getInt("start");
            limit = extras.getInt("limit");
            query = extras.getParcelable("query");

        }else if(getArguments() != null){
            // preferred : get data from arguments
            if (getArguments().containsKey(Constants.ParamKeys.LAYERS)) {
                layers = (ArrayList<Layer>) getArguments().getSerializable(Constants.ParamKeys.LAYERS);
            }
            if (getArguments().containsKey("start")){
                start = getArguments().getInt("start");
            }
            if(getArguments().containsKey("limit")) {
                limit = getArguments().getInt("limit");
            }
            if(getArguments().containsKey("query")) {
                query = getArguments().getParcelable("query");
            }
            if(getArguments().containsKey(PARAM_CUSTOM_LAYOUT)){
                mCustomLayout = getArguments().getInt(PARAM_CUSTOM_LAYOUT);
            }
        }


        // setup the listView
        adapter = new FeatureInfoAttributesAdapter(getActivity(), R.layout.feature_info_attribute_row);
        setListAdapter(adapter);

        // TODO get already loaded data;

        startDataLoading(query, layers, start, 2);// use 2 to check availability of the next page
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //why is the loading started in both onCreate and onCreateView ?
        //start this second loading only if not prevented by argument
        if(!(getArguments() != null && getArguments().containsKey(PARAM_DONT_LOAD_TWICE))) {
            startDataLoading(query, layers, start, 2);
        }
        //if we have a custom layout use it, otherwise use the default one
        int resourceID = mCustomLayout == 0 ? R.layout.feature_info_attribute_list : mCustomLayout;

        return inflater.inflate(resourceID, container, false);
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onViewCreated(android.view.View,
     * android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        setButtonBarVisibility(currentFeatures);
        startLoadingGUI();
        ImageButton prev = (ImageButton) view.findViewById(R.id.previousButton);
        ImageButton next = (ImageButton) view.findViewById(R.id.nextButton);
        ImageButton marker = (ImageButton) view.findViewById(R.id.use_for_marker);

        // load the previous page on click
        prev.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startLoadingGUI();
                adapter.clear();
                start--;
                startDataLoading(query, layers, start, 2);
            }
        });

        // load the next page on press
        next.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                startLoadingGUI();
                adapter.clear();
                start++;
                startDataLoading(query, layers, start, 2);

            }
        });
        // show a dialog and return if ok
        final Context context = this.getActivity();
        if(marker != null) {
            marker.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    AlertDialog.Builder confirm = new AlertDialog.Builder(context);
                    confirm.setTitle(R.string.use_this_feature);
                    confirm.setMessage(R.string.use_this_feature_description);
                    confirm.setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    returnSelectedItem();
                                }
                            });
                    confirm.setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // TODO close
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = confirm.create();
                    alert.show();

                }
            });
        }

        if(waitForInflation){

            prev.setVisibility(start > 0 ? View.VISIBLE : View.INVISIBLE);
            next.setVisibility(start + 1 < currentFeatures.size() ? View.VISIBLE : View.INVISIBLE);

            waitForInflation = false;
        }
    }

    /**
     * Create an array of <BaseFeatureInfoQuery> to pass to the loader and
     * initialize the loader
     *
     * @param query the <BaseFeatureInfoQuery>
     * @param layers array of <String> to generate the queryQueue
     * @param start
     * @param limit
     */
    private void startDataLoading(BaseFeatureInfoQuery query, ArrayList<Layer> layers, Integer start, Integer limit) {

        if(createQueryQueue(query, layers, start, limit)) {
            // initialize Load Manager
            mCallbacks = this;
            LoaderManager lm = getActivity().getSupportLoaderManager();
            // NOTE: use the start variable as index in the loadermanager
            // if you use more than one
            adapter.clear();
            lm.initLoader(start, null, this); // uses start to get the
        }
    }

    /**
     * creates a query queue if necessary
     * if not false is returned
     * @param query the query
     * @param layers the layers (may be null for WMSGetFeatureInfo)
     * @param start the index of the requested feature
     * @param limit
     * @return true if a new queryqueue was created, false if the query can be answered with the current data
     */
    private boolean createQueryQueue(BaseFeatureInfoQuery query,ArrayList<Layer> layers, Integer start, Integer limit){

        // create task query
        if(query instanceof WMSGetFeatureInfoQuery){
            if(currentFeatures != null){
                //start is the desired index
                if (currentFeatures.size() > start) {
                    adapter.clear();
                    adapter.addAll(currentFeatures.get(start));

                    //setup the buttons manually
                    if(getView() != null) {
                        //if start > 0 previous is visible
                        if (getView().findViewById(R.id.previousButton) != null) {
                            getView().findViewById(R.id.previousButton).setVisibility(start > 0 ? View.VISIBLE : View.INVISIBLE);
                        }
                        //if start + 1 < feature.count next is visible
                        if (getView().findViewById(R.id.nextButton) != null) {
                            getView().findViewById(R.id.nextButton).setVisibility(start + 1 < currentFeatures.size() ? View.VISIBLE : View.INVISIBLE);
                        }
                    }else{
                        waitForInflation = true;
                    }
                    return false; //no need to requery
                }
            }
            queryQueue = FeatureInfoUtils.createWMSPointQueryQueue((WMSGetFeatureInfoQuery) query, start, limit);

            showProgress(getActivity().getString(R.string.please_wait));

        }else if(query instanceof BBoxQuery) {
            queryQueue = FeatureInfoUtils.createTaskQueryQueue(layers, (BBoxQuery) query, start,
                    limit);
        }else if(query instanceof CircleQuery) {
            queryQueue = FeatureInfoUtils.createTaskQueryQueue(layers, (CircleQuery) query, start,
                    limit);
        }else {
            queryQueue = FeatureInfoUtils.createTaskQueryQueue(layers, (PolygonQuery) query, start,
                    limit);
        }
        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // mCallbacks = (TaskCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // mCallbacks = null;
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int,
     * android.os.Bundle)
     */
    @Override
    public Loader<List<FeatureInfoQueryResult>> onCreateLoader(int id, Bundle args) {

        return new FeatureInfoLoader(getActivity(),queryQueue);
    }

    // populate the list and set buttonbar visibility options
    @Override
    public void onLoadFinished(Loader<List<FeatureInfoQueryResult>> loader,
                               List<FeatureInfoQueryResult> data) {
        setListAdapter(adapter);
        if (data.size() > 0) {
            // only one layer display
            FeatureInfoQueryResult result = data.get(0);
            currentFeatures = result.getFeatures();
            setButtonBarVisibility(currentFeatures);
            if (currentFeatures.size() > 0) {
                // only the first feature display.
                // other will be used to check availability
                adapter.addAll(currentFeatures.get(0));
            }

        } else {
            setButtonBarVisibility(null);

        }
        if(query instanceof  WMSGetFeatureInfoQuery){
            hideProgress();
        }

        Log.v("FEATURE_INFO", "added " + adapter.getCount() + " items to the view");
        stopLoadingGUI();

        if(mListener != null){
            mListener.didFinishLoading();
        }

    }

    /**
     * sets no data view in default listview empty text
     */
    private void setNoData() {
        ((TextView) getView().findViewById(R.id.empty_text))
                .setText(R.string.feature_info_extracting_no_result);
    }

    /**
     * Set the loading bar and loading text
     */
    private void startLoadingGUI() {
        if (getActivity() != null) {
            // start progress bars
            getActivity().setProgressBarVisibility(true);
        }
        // set suggestion text
        ((TextView) getView().findViewById(R.id.empty_text))
                .setText(R.string.feature_info_extracting_information);
    }

    /**
     * hide loading bar and set loading task
     */
    private void stopLoadingGUI() {
        if (getActivity() != null) {
            getActivity().setProgressBarIndeterminateVisibility(
                    false);
            getActivity().setProgressBarVisibility(false);
            Log.v("FEATURE_INFO_TASK", "task terminated");

        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Set the visibility using the size of the features
     *
     * @param features
     */
    private void setButtonBarVisibility(ArrayList<Feature> features) {

        if(getView() == null){
            return;
        }

        if (features == null) {
            getView().findViewById(R.id.attributeButtonBar).setVisibility(
                    View.INVISIBLE);
            setNoData();
            return;
        }
        if (features.size() > 0) {
            getView().findViewById(R.id.attributeButtonBar).setVisibility(
                    View.VISIBLE);
            //previous button
            if (start > 0) {
                getView().findViewById(R.id.previousButton).setVisibility(
                        View.VISIBLE);
            } else {
                getView().findViewById(R.id.previousButton).setVisibility(
                        View.INVISIBLE);
            }
            //next button
            if (features.size() > 1) {
                getView().findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
            } else {
                getView().findViewById(R.id.nextButton).setVisibility(
                        View.INVISIBLE);
            }
            //marker button
            if(getView().findViewById(R.id.use_for_marker) != null) {
                if (Intent.ACTION_VIEW.equals(getActivity().getIntent().getAction())) {
                    getView().findViewById(R.id.use_for_marker).setVisibility(View.INVISIBLE);
                } else {
                    getView().findViewById(R.id.use_for_marker).setVisibility(View.VISIBLE);
                }
            }else{
                //this is a custom layout without "use for marker" it can hide the bottom bar
                //if there is only one feature
                if(features.size() == 1){
                    getView().findViewById(R.id.attributeButtonBar).setVisibility(View.GONE);
                }
            }
        } else {
            getView().findViewById(R.id.attributeButtonBar).setVisibility(
                    View.INVISIBLE);
            setNoData();
        }

    }

    @Override
    public void onLoaderReset(Loader<List<FeatureInfoQueryResult>> arg0) {
        adapter.clear();

    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        // TODO try to kill the load process
        super.onDestroy();
    }

    private void returnSelectedItem() {
        Intent returnIntent = new Intent();
        Activity activity = getActivity();
        // get current markers
        // currentFeatures is present
        if (currentFeatures != null && currentFeatures.size() > 0) {
            returnIntent.putExtra(
                    GetFeatureInfoLayerListActivity.RESULT_FEATURE_EXTRA,
                    currentFeatures.get(0));
            returnIntent.putExtra(
                    GetFeatureInfoLayerListActivity.LAYER_FEATURE_EXTRA,
                    layers.get(0));
            activity.setResult(Activity.RESULT_OK, returnIntent);
        }
        activity.finish();
    }


    public void showProgress(final String message) {

        if (pd == null ||  pd.getOwnerActivity() == null || pd.getOwnerActivity().isFinishing() || pd.getOwnerActivity().isFinishing()){
            pd = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
            pd.setCancelable(false);
            pd.setIcon(R.drawable.ic_launcher);
        }
        pd.setMessage(message);
        pd.show();

    }
    public void hideProgress(){

        if(pd != null && pd.isShowing()){
            try {
                pd.dismiss();
            }catch (IllegalArgumentException e){
                // window was dismissed, ignore this
                if(BuildConfig.DEBUG) {
                    Log.e(TAG, "Dismissing an orphaned Dialog", e);
                }
            }
        }
    }

    /**
     * requery the loader and update the view of this fragment
     * @param query the new query
     * @param layers list of layers (may be null for WMSGetFeatureInfoQuery)
     */
    public void requery(BaseFeatureInfoQuery query, ArrayList<Layer> layers) {

        currentFeatures = null;

        start = 0;
        limit = 1;

        createQueryQueue(query, layers, start, limit);

        adapter.clear();

        getActivity().getSupportLoaderManager().restartLoader(start, null, this);

    }

    /**
     * Listener for this fragment which informs registered objects that the loader finished loading
     */
    public interface FeatureInfoLoadedListener
    {
        void didFinishLoading();
    }

    public void setFeatureInfoLoadedListener(FeatureInfoLoadedListener mListener) {
        this.mListener = mListener;
    }
}
