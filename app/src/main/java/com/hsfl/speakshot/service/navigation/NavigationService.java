package com.hsfl.speakshot.service.navigation;

import android.app.*;
import android.os.Bundle;
import com.hsfl.speakshot.R;

public class NavigationService {
    private static final String TAG = NavigationService.class.getSimpleName();

    /**
     * The NavigationService singleton
     */
    private static NavigationService instance = null;

    /**
     * mFragmentManager
     */
    private FragmentManager mFragmentManager;

    /**
     * The default container to inject the fragment
     */
    private int defaultContainer = R.id.fragment_container;

    /**
     * Empty constructor
     */
    NavigationService() {}

    /**
     * Gets the NavigationService instance
     * @return
     */
    public static NavigationService getInstance() {
        if (instance == null) {
            instance = new NavigationService();
        }
        return instance;
    }

    /**
     * init
     * @param fragmentManager
     */
    public void init(FragmentManager fragmentManager) {
        mFragmentManager =  fragmentManager;
    }

    /**
     * (Sticky) Switches the fragment and adds it to the backstack
     * @param fragment
     * @param args
     */
    public void toS(Fragment fragment, Bundle args) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        // sets the arguments
        fragment.setArguments(args);
        // gets the fragment
        ft.replace(defaultContainer, fragment);
        ft.addToBackStack(fragment.getTag());
        // Commit the transaction
        ft.commit();
    }

    /**
     * Switches the fragment
     * @param fragment
     */
    public void to(Fragment fragment, Bundle args) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        // sets the arguments
        fragment.setArguments(args);
        // gets the fragment
        ft.replace(defaultContainer, fragment);
        // Commit the transaction
        ft.commit();
    }

    /**
     * Returns to the last view
     */
    public void back() {
        int i = mFragmentManager.getBackStackEntryCount();
        if (i > 0) {
            mFragmentManager.popBackStackImmediate();
        }
    }
}
