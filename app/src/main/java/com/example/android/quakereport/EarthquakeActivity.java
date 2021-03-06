/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.quakereport;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Earthquake>> {

    public static final String LOG_TAG = EarthquakeActivity.class.getName();

    /* The USGS Api URL */
    private static final String USGS_REQUEST_URL =
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&orderby=time&minmag=5&limit=10";

    /* Adapter for the list of earthquakes */
    private EarthquakeAdapter mAdapter;

    /*
    *   TextView to display the status of the app
    *   (if it doesn't have any data to display
    *   or if it's not connected to the internet)
    * */
    private TextView mStateTextView;

    /* ProgressBar variable */
    private ProgressBar progressBar;

    /* ConnectivityManager variable to check the state of connectivity */
    ConnectivityManager cm;

    /* Variable to check the default data network */
    NetworkInfo activeNetwork;

    /* The final status of connectivity of the phone */
    boolean isConnected;

    /**
     * Constant value for the earthquake Loader ID. We can chose any integer.
     * This really only comes into play if you're using multiple loaders
     * */
    private static final int EARTHQUAKE_LOADER_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "TEST: Earthquake ACTIVITY onCreate() called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.earthquake_activity);

        // Get a reference to the ConnectivityManager to check state of network connectivity
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        activeNetwork = cm.getActiveNetworkInfo();

        // Assign a boolean value whether or not the phone connected
        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // Get a reference to the ListView, and attach the adapter to the listView.
        ListView listView = (ListView) findViewById(R.id.list);

        mStateTextView = (TextView) findViewById(R.id.state_view);
        listView.setEmptyView(mStateTextView);

        /*
            final ArrayList<Earthquake> earthquakes = QueryUtils.extractFeatureFromJson();

            // Create an {@link AndroidFlavorAdapter}, whose data source is a list of
            // {@link AndroidFlavor}s. The adapter knows how to create list item views for each item
            // in the list.
            final EarthquakeAdapter earthquakeAdapter = new EarthquakeAdapter(this, earthquakes);

            listView.setAdapter(earthquakeAdapter);

            //OnClickListener to listen for clicks in each ListView item
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
                {
                    //Get the specific item from the ListView
                    Earthquake earthquake = (Earthquake) earthquakeAdapter.getItem(i);

                    //Create an intent that will get the url from the JSON and open it
                    //in a browser
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(earthquake.getUrl()));
                    startActivity(intent);
                }
            });
        */

        //Create a new adapter that takes an empty list as input
        mAdapter = new EarthquakeAdapter(this, new ArrayList<Earthquake>());

        //Set the adapter on the {@link ListView}
        //so the list can be populated in the user interface
        listView.setAdapter(mAdapter);

        //Set an item click listener on the ListView, which sends an intent to the web browser
        //to open a website with more information about the selected earthcquake
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
                    {
                        // Find the current earthquake that was clicked on
                        Earthquake currentEarthquake = (Earthquake) mAdapter.getItem(i);

                        //Convert the String URL into a URI object (to pass to the Intent constructor)
                        Uri earthquakeUri = Uri.parse(currentEarthquake.getUrl());

                        //Create a new intent to view the earthquake URI
                        Intent websiteIntent = new Intent(Intent.ACTION_VIEW, earthquakeUri);

                        //Send the intent to launch the new activity
                        startActivity(websiteIntent);
                    }
            }
        );

        /*
            //Start the AsyncTask to fetch the earthquake data
            EarthquakeAsyncTask task = new EarthquakeAsyncTask();
            task.execute(USGS_REQUEST_URL);
        */

        if(isConnected)
            {
                //Get a reference to the LoaderManager, in order to interact with the loaders
                LoaderManager loaderManager = getLoaderManager();

                //Initialize the loader. Pass in the int ID constant defined above and pass in null for
                //the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
                //because this activity implements the LoaderCallbacks interface)
                loaderManager.initLoader(EARTHQUAKE_LOADER_ID, null, this);
                Log.i(LOG_TAG, "TEST: calling initLoader() ... ");
            }
        else
            {
                // Set the progress bar to the ProgressBar View in the xml
                // And set it into an indeterminate state
                progressBar = (ProgressBar) findViewById(R.id.indeterminateBar);
                progressBar.setProgress(0);

                // Once the onLoadFinished() method is called, the loading progressbar will disappear
                progressBar.setVisibility(View.GONE);

                // Set the TextView to show a text that says "Not connected to any Network"
                mStateTextView.setText(R.string.no_internet);
            }
    }

    /**
     * {@link AsyncTask} to perform the network request on a background thread, and then
     * update the UI with the list of earthquakes in the response.
     *
     * AsyncTask has three generic parameters: the input type, a type used for progress updates, and
     * an output type. Our task will take a String URL, and return an Earthquake. We won't do
     * progress updates, so the second generic is just Void.
     *
     * We'll only override two of the methods of AsyncTask: doInBackground() and onPostExecute().
     * The doInBackground() method runs on a background thread, so it can run long-running code
     * (like network activity), without interfering with the responsiveness of the app.
     * Then onPostExecute() is passed the result of doInBackground() method, but runs on the
     * UI thread, so it can use the produced data to update the UI.
     */
    /*
    private class EarthquakeAsyncTask extends AsyncTask<String, Void, List<Earthquake>>
        {
            /**
             * This method runs on a background thread and performs the network request.
             * We should not update the UI from a background thread, so we return a list of
             * {@link Earthquake}s as the result.

            @Override
            protected List<Earthquake> doInBackground(String... urls)
                {
                    if(urls.length <1 || urls[0] == null)
                        {
                            return null;
                        }

                    List<Earthquake> result = QueryUtils.fetchEarthquakeData(urls[0]);
                    return result;
                }

            /**
             * This method runs on the main UI thread after the background work has been
             * completed. This method receives as input, the return value from the doInBackground()
             * method. First we clear out the adapter, to get rid of earthquake data from a previous
             * query to USGS. Then we update the adapter with the new list of earthquakes,
             * which will trigger the ListView to re-populate its list items.

            @Override
            protected void onPostExecute(List<Earthquake> earthquakes)
                {
                    //Clear the adapter of previous earthquake data
                    mAdapter.clear();

                    //If there is a valid list of {@link Earthquake} then add them to the adapter's
                    //data set. This will trigger the ListView to update
                    if(earthquakes != null && !earthquakes.isEmpty())
                        {
                            mAdapter.addAll(earthquakes);
                        }
                }
        }
       */

    @Override
    public Loader<List<Earthquake>> onCreateLoader(int i, Bundle bundle)
        {
            Log.i(LOG_TAG, "TEST: onCreateLoader() called ... ");

            //Create a new Loader for the given url
            return new EarthquakeLoader(this, USGS_REQUEST_URL);
        }

    @Override
    public void onLoadFinished(Loader<List<Earthquake>> loader, List<Earthquake> earthquakes)
        {
            Log.i(LOG_TAG, "TEST: onLoadFinished() called ... ");

            // Set the progress bar to the ProgressBar View in the xml
            // And set it into an indeterminate state
            progressBar = (ProgressBar) findViewById(R.id.indeterminateBar);
            progressBar.setProgress(0);

            // Once the onLoadFinished() method is called, the loading progressbar will disappear
            progressBar.setVisibility(View.GONE);

            // Set the state TextView to show a text that says "Not Earthquakes found"
            mStateTextView.setText(R.string.no_earthquakes);

            // Clear the adapter of previous earthquake data
            mAdapter.clear();

            // If there is a valid list of {@link Earthquake} then add them to the adapter's
            // data set. This will trigger the ListView to update
            if(earthquakes != null && !earthquakes.isEmpty())
                {
                    // Add all the Earthquake infos to the adapter
                    mAdapter.addAll(earthquakes);
                }
        }

    @Override
    public void onLoaderReset(Loader<List<Earthquake>> loader)
        {
            Log.i(LOG_TAG, "TEST: onLoaderReset() called ... ");

            // Loader reset, so we can clear out our existing data
            mAdapter.clear();
        }
}
