package com.hsfl.speakshot.service.guide;

import android.app.Activity;
import android.view.ViewGroup;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.ui.views.GuidingLayout;

import java.util.HashMap;
import java.util.Map;

public class GuidingService {
    private static final String TAG = GuidingService.class.getSimpleName();

    /**
     * The GuidingService singleton
     */
    private static GuidingService instance = null;

    /**
     * Orientation texts
     */
    private static Map<String, String> mOrientationTexts = new HashMap<String, String>(){{
        put("left",   "Zu weit Links");
        put("right",  "Zu weit Rechts");
        put("top",    "Zu weit Oben");
        put("bottom", "Zu weit Unten");
    }};

    /**
     * The layout for the border marks
     */
    private GuidingLayout mGuidingLayout = null;

    /**
     * Flag that indicates whether audio is enabled
     */
    private boolean mAudioEnabled = true;

    /**
     * Empty Constructor
     */
    GuidingService() {}

    /**
     * Gets the GuidingService instance
     * @return
     */
    public static GuidingService getInstance() {
        if (instance == null) {
            instance = new GuidingService();
        }
        return instance;
    }

    /**
     * Setting up the GuidingService
     */
    public void init(Activity activity) {
        // initializes the view
        if (mGuidingLayout == null) {
            mGuidingLayout = new GuidingLayout(activity);
            activity.addContentView(mGuidingLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    /**
     * Marks a border
     * @param name
     */
    public void mark(String name) {
        // shows visual mark
        if (mGuidingLayout != null) {
            mGuidingLayout.playAnimationFor(name);
        }
        // shows acoustic mark
        if (mAudioEnabled) {
            String text = mOrientationTexts.get(name);
            AudioService.getInstance().speak(text);
        }
    }
}
