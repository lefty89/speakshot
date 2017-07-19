package com.hsfl.speakshot.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.*;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;


public class GuidingLayout extends FrameLayout {
    private static final String TAG = GuidingLayout.class.getSimpleName();

    /**
     * IDs for the different borders
     */
    public final static String BORDER_LEFT   = "left";
    public final static String BORDER_RIGHT  = "right";
    public final static String BORDER_TOP    = "top";
    public final static String BORDER_BOTTOM = "bottom";

    /**
     * Border settings
     */
    private final int MARK_LENGTH   = 50;
    private final int ANIM_DURATION = 1000;
    private final int BORDER_COLOR  = Color.RED;

    /**
     * Border marks
     */
    private Map<String, ImageView> mBorderViews = new HashMap<>();

    /**
     * Constructor
     * @param context
     */
    public GuidingLayout(Context context) {
        super(context);
    }

    /**
     * Adds the borders to all sides
     * @param name
     * @param width
     * @param height
     * @param grad
     * @param dim
     */
    private void addBorderMark(String name, int width, int height, int[] grad, int[] dim) {
        // create canvas from bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // draw cradient into canvas
        Shader shader = new LinearGradient(grad[0], grad[1], grad[2], grad[3], new int[]{BORDER_COLOR, Color.TRANSPARENT}, new float[]{0, 1.0f}, Shader.TileMode.MIRROR);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(shader);
        canvas.drawRect(dim[0], dim[1], dim[2], dim[3], paint);

        // gets the view
        ImageView iv = mBorderViews.get(name);
        if (iv == null) {
            iv = new ImageView(getContext());
            mBorderViews.put(name, iv);
            addView(iv);
        }
        iv.setImageBitmap(bitmap);
        iv.setAlpha(0.0f);
    }

    /**
     * Plays a fade-in-out animation for the given border
     * @param name
     */
    public void playAnimationFor(String name) {

        ImageView iv = mBorderViews.get(name);
        if (iv != null) {

            // fade out animation
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(iv, "alpha",  1f, .0f);
            fadeOut.setDuration(ANIM_DURATION);

            // fade in animation
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(iv, "alpha", .0f, 1f);
            fadeIn.setDuration(ANIM_DURATION);

            // chain them to a set
            final AnimatorSet mAnimationSet = new AnimatorSet();
            mAnimationSet.play(fadeIn).before(fadeOut);
            mAnimationSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                }
            });
            mAnimationSet.start();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        final int width  = right - left;
        final int height = bottom - top;

        // add mars to all sides
        addBorderMark(BORDER_LEFT,   width, height, new int[]{0,0,MARK_LENGTH,0},             new int[]{0,0,MARK_LENGTH,height});
        addBorderMark(BORDER_TOP,    width, height, new int[]{0,0,0,MARK_LENGTH},             new int[]{0,0,width,MARK_LENGTH});
        addBorderMark(BORDER_BOTTOM, width, height, new int[]{0,height,0,height-MARK_LENGTH}, new int[]{0,height-MARK_LENGTH,width,height});
        addBorderMark(BORDER_RIGHT,  width, height, new int[]{width,0,width-MARK_LENGTH,0},   new int[]{width-MARK_LENGTH,0,width,height});
    }
}
