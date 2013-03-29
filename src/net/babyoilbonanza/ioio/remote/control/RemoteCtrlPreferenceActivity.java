/**
 * 
 */
package net.babyoilbonanza.ioio.remote.control;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * @author rbw
 * 
 */
@TargetApi(10)
public class RemoteCtrlPreferenceActivity extends PreferenceActivity
{

    
    
    @SuppressWarnings("deprecation")
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.ioio_remote_control_preference);
        
    }



}


/*
 * SCRAP - for future API levels (14+)
 

    @Override public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }



    /**
     * This fragment shows the preferences for the first header.
     *
    public static class CtrlModeFragment extends PreferenceFragment
    {
        @Override public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            // Make sure default values are applied. In a real app, you would
            // want this in a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
            PreferenceManager.setDefaultValues(getActivity(), R.xml.fragmented_preferences, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.fragmented_preferences);
        }
    }

*/