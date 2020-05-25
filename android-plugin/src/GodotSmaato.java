package org.godotengine.godot;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;

import com.smaato.soma.AdDimension;
import com.smaato.soma.AdDownloaderInterface;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.AdSettings;
import com.smaato.soma.BannerStateListener;
import com.smaato.soma.BannerView;
import com.smaato.soma.BaseView;
import com.smaato.soma.ReceivedBannerInterface;
import com.smaato.soma.SOMA;
import com.smaato.soma.bannerutilities.constant.BannerStatus;
import com.smaato.soma.internal.requests.settings.UserSettings;
import com.smaato.soma.interstitial.Interstitial;
import com.smaato.soma.interstitial.InterstitialAdListener;
import com.smaato.soma.video.RewardedVideo;
import com.smaato.soma.video.RewardedVideoListener;

public class GodotSmaato extends Godot.SingletonBase
{

    private final String TAG = GodotSmaato.class.getName();
    private Activity activity = null; // The main activity of the game
    private long publisherId;

    private HashMap<String, View> zombieBanners = new HashMap<>();
    private HashMap<String, FrameLayout.LayoutParams> bannerParams = new HashMap<>();
    private HashMap<String, Interstitial> interstitials = new HashMap<>();
    private HashMap<String, BannerView> banners = new HashMap<>();
    private HashMap<String, RewardedVideo> rewardeds = new HashMap<>();

    private boolean ProductionMode = true; // Store if is real or not
    private boolean isForChildDirectedTreatment = false; // Store if is children directed treatment desired
    private String maxAdContentRating = ""; // Store maxAdContentRating ("G", "PG", "T" or "MA")
    private Bundle extras = null;

    private FrameLayout layout = null; // Store the layout

    /* Init
     * ********************************************************************** */

    /**
     * Prepare for work with Smaato
     * @param boolean ProductionMode Tell if the enviroment is for real or test
     * @param int gdscript instance id
     */
    public void init(final String _publisherId, boolean ProductionMode) {

        layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();
        this.ProductionMode = ProductionMode;
        publisherId = Long.parseLong(_publisherId);
        UserSettings userSettings = new UserSettings();
        SOMA.init(activity.getApplication(), userSettings);
    }

    /* Rewarded Video
     * ********************************************************************** */
    private RewardedVideo initRewardedVideo(final String id, final int callback_id)
    {
        Log.w(TAG, "Smaato: Prepare rewarded video: "+id+" callback: "+Integer.toString(callback_id));
        RewardedVideo rewardedVideo = new RewardedVideo(activity);
        rewardedVideo.getAdSettings().setPublisherId(publisherId);
        rewardedVideo.getAdSettings().setAdspaceId(Long.parseLong(id));
        rewardedVideo.setRewardedVideoListener(new RewardedVideoListener(){
                // Called when the banner has been loaded.
                @Override
                public void onReadyToShow() {
                    Log.w(TAG, "Smaato: onReadyToShow");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_loaded", new Object[] { id });
                }
                // Called when the ad will be displayed.
                @Override
                public void onWillShow() {
                }
                // Called when the RewardedVideo or the EndCard is clicked after video completion
                @Override
                public void onWillOpenLandingPage () {
                    Log.w(TAG, "Smaato: onWillOpenLandingPage");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_left_application", new Object[] { id });
                }
                // Called when the ad is closed.
                @Override
                public void onWillClose () {
                    Log.w(TAG, "Smaato: onWillClose");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_closed", new Object[] { id });
                }
                // Called when there is No ad or Ad failed to Load
                @Override
                public void onFailedToLoadAd () {
                    Log.w(TAG, "Smaato: onFailedToLoadAd");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, "General failure" });
                }
                @Override
                public void onRewardedVideoStarted () {
                    Log.w(TAG, "Smaato: onRewardedVideoStarted");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_started", new Object[] { id });
                }
                @Override
                public void onFirstQuartileCompleted () {
                }
                @Override
                public void onSecondQuartileCompleted () {
                }
                @Override
                public void onThirdQuartileCompleted () {
                }
                // Called when the Video ad display completed.
                @Override
                public void onRewardedVideoCompleted () {
                    Log.w(TAG, "Smaato: onRewardedVideoCompleted");
                    GodotLib.calldeferred(callback_id, "_on_rewarded", new Object[] { id, "reward", 1 });
                }
            });
        return rewardedVideo;
    }

    /**
     * Load a Rewarded Video
     * @param String id AdMod Rewarded video ID
     */
    public void loadRewardedVideo(final String id, final int callback_id) {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    RewardedVideo r = initRewardedVideo(id, callback_id);
                    rewardeds.put(id, r);
                    r.asyncLoadNewBanner();
                }
            });
    }

    /**
     * Show a Rewarded Video
     */
    public void showRewardedVideo(final String id) {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(rewardeds.containsKey(id)) {
                        RewardedVideo r = rewardeds.get(id);
                        r.show();   // call this when to show the RewardedVideo ad.
                    } else {
                        Log.w(TAG, "Smaato: showRewardedVideo - rewarded not found: " + id);
                    }
                }
            });
    }

    /* Banner
     * ********************************************************************** */

    private BannerView initBanner(final String id, final boolean isOnTop, final int callback_id)
    {
        Log.w(TAG, "Smaato: Prepare banner: "+id+" callback: "+Integer.toString(callback_id));

        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                                                                         FrameLayout.LayoutParams.MATCH_PARENT,
                                                                         FrameLayout.LayoutParams.WRAP_CONTENT
                                                                         );
        if(isOnTop) adParams.gravity = Gravity.TOP;
        else adParams.gravity = Gravity.BOTTOM;
        bannerParams.put(id, adParams);
                
        BannerView banner = new BannerView(activity);
        banner.setBackgroundColor(/* Color.WHITE */Color.TRANSPARENT);
        banner.getAdSettings().setPublisherId(publisherId);
        banner.getAdSettings().setAdspaceId(Long.parseLong(id));
        banner.getAdSettings().setHttpsOnly(false);
        banner.setLocationUpdateEnabled(true);
        banner.setAutoReloadEnabled(false);
        //banner.getAdSettings().setAdDimension(AdDimension.MEDIUMRECTANGLE);
        banner.getAdSettings().setAdDimension(AdDimension.DEFAULT);

        banner.setBannerStateListener(new BannerStateListener() {
                @Override
                public void onWillOpenLandingPage(BaseView bannerView) {
                    // TODO Auto-generated method stub
                }
                @Override
                public void onWillCloseLandingPage(BaseView bannerView) {
                    // TODO Auto-generated method stub
                }
            });

        banner.addAdListener(new AdListenerInterface() {
                @Override
                public void onReceiveAd(AdDownloaderInterface adDownloader, ReceivedBannerInterface banner) {
                    if(banner.getStatus() == BannerStatus.ERROR){
                        Log.w(TAG, "onReceiveAd error:"+banner.getErrorCode()+ " "+banner.getErrorMessage());
                        GodotLib.calldeferred(callback_id, "_on_banner_failed_to_load", new Object[]{ id, "General failure" });
                    } else {
                        // Banner download succeeded
                        Log.w(TAG, "onReceiveAd success");
                        GodotLib.calldeferred(callback_id, "_on_banner_loaded", new Object[]{ id });
                    }
                }
            });
        return banner;
    }

    private void placeBannerOnScreen(final String id, final BannerView banner)
    {
        FrameLayout.LayoutParams adParams = bannerParams.get(id);
        layout.addView(banner, adParams);
    }

    /**
     * Load a banner
     * @param String id AdMod Banner ID
     * @param boolean isOnTop To made the banner top or bottom
     */
    public void loadBanner(final String id, final boolean isOnTop, final int callback_id)
    {
        activity.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    if(!banners.containsKey(id)) {
                        BannerView b = initBanner(id, isOnTop, callback_id);
                        banners.put(id, b);
                        // Request
                        b.asyncLoadNewBanner();
                    } else {
                        BannerView b = banners.get(id);
                        b.asyncLoadNewBanner();
                        //Log.w(TAG, "Smaato: Banner already created: "+id);
                    }
                }
            });
    }

    /**
     * Show the banner
     */
    public void showBanner(final String id)
    {
        activity.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    if(banners.containsKey(id)) {
                        BannerView b = banners.get(id);
                        if(b.getParent() == null) {
                            placeBannerOnScreen(id, b);
                        }
                        b.setVisibility(View.VISIBLE);
                        for (String key : banners.keySet()) {
                            if(!key.equals(id)) {
                                BannerView b2 = banners.get(key);
                                b2.setVisibility(View.GONE);
                            }
                        }
                        Log.d(TAG, "Smaato: Show Banner");
                    } else {
                        Log.w(TAG, "Smmato: Banner not found: "+id);
                    }
                }
            });
    }

    public void removeBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        BannerView b = banners.get(id);
                        banners.remove(id);
                        layout.removeView(b); // Remove the banner
                        b.destroy();
                        Log.d(TAG, "Smaato: Remove Banner");
                    } else {
                        Log.w(TAG, "Smaato: Banner not found: "+id);
                    }
                }
            });
    }


    /**
     * Hide the banner
     */
    public void hideBanner(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(banners.containsKey(id)) {
                        BannerView b = banners.get(id);
                        b.setVisibility(View.GONE);
                        Log.d(TAG, "Smaato: Hide Banner");
                    } else {
                        Log.w(TAG, "Smaato: Banner not found: "+id);
                    }
                }
            });
    }

    /**
     * Get the banner width
     * @return int Banner width
     */
    public int getBannerWidth(final String id)
    {
        if(banners.containsKey(id)) {
            BannerView b = banners.get(id);
            if(b != null) {
                int w = b.getWidth();
                if(w == 0) w = b.getAdSettings().getBannerWidth();
                if(w == 0) {
                    Resources r = activity.getResources();
                    w = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, r.getDisplayMetrics());
                }
                return w;
            } else {
                Log.w(TAG, "getBannerWidth: banner not found " + id);
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Get the banner height
     * @return int Banner height
     */
    public int getBannerHeight(final String id)
    {
        if(banners.containsKey(id)) {
            BannerView b = banners.get(id);
            if(b != null) {
                int h = b.getHeight();
                if(h == 0) h = b.getAdSettings().getBannerHeight();
                if(h == 0) {
                    Resources r = activity.getResources();
                    h = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());
                }
                return h;
            } else {
                Log.w(TAG, "getBannerHeight: banner not found " + id);
                return 0;
            }
        } else {
            return 0;
        }
    }

    public String makeZombieBanner(final String id)
    {
        if (banners.containsKey(id)) {
            BannerView b = banners.get(id);
            String zid = java.util.UUID.randomUUID().toString();
            banners.remove(id);
            zombieBanners.put(zid, b);
            Log.i(TAG, "makeZombieBanner: OK");
            return zid;
        } else {
            Log.w(TAG, "makeZombieBanner: Banner not found: "+id);
            return "";
        }
    }

    public void killZombieBanner(final String zid)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (zombieBanners.containsKey(zid)) {
                        View z = zombieBanners.get(zid);
                        zombieBanners.remove(zid);
                        layout.removeView(z); // Remove the zombie banner
                        Log.w(TAG, "killZombieBanner: OK");
                    } else {
                        Log.w(TAG, "killZombieBanner: Banner not found: "+zid);
                    }
                }
            });
    }

    /* Interstitial
     * ********************************************************************** */

    private Interstitial initInterstitial(final String id, final int callback_id)
    {
        Log.w(TAG, "Smaato: Prepare interstitial: "+id+" callback: "+Integer.toString(callback_id));
        Interstitial interstitial = new Interstitial(activity);
        interstitial.getAdSettings().setPublisherId(publisherId);
        interstitial.getAdSettings().setAdspaceId(Long.parseLong(id));
        interstitial.setInterstitialAdListener(new InterstitialAdListener() {
                @Override
                public void onReadyToShow() {
                    // interstitial is loaded an may be shown
                    Log.w(TAG, "Interstitial: onReadyToShow");
                    GodotLib.calldeferred(callback_id, "_on_interstitial_loaded", new Object[] { id });
                }
                @Override
                public void onWillShow() {
                    // called immediately before the interstitial is shown
                    Log.w(TAG, "Interstitial: onWillShow");
                }
                @Override
                public void onWillOpenLandingPage() {
                    // called immediately before the landing page is opened, after the user clicked
                    Log.w(TAG, "Interstitial: onWillOpenLandingPage");
                }
                @Override
                public void onWillClose() {
                    // called immediately before the interstitial is dismissed
                    Log.w(TAG, "Interstitial: onWillClose");
                    GodotLib.calldeferred(callback_id, "_on_interstitial_close", new Object[] { id });
                }
                @Override
                public void onFailedToLoadAd() {
                    // loading the interstitial has failed
                    Log.w(TAG, "Interstitial: onFailedToLoadAd");
                    GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, "General failure" });
                }
            });
        return interstitial;
    }

    /**
     * Load a interstitial
     * @param String id AdMod Interstitial ID
     */
    public void loadInterstitial(final String id, final int callback_id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    Interstitial interstitialAd = initInterstitial(id, callback_id);
                    interstitials.put(id, interstitialAd);
                    interstitialAd.asyncLoadNewBanner();
                }
            });
    }

    /**
     * Show the interstitial
     */
    public void showInterstitial(final String id)
    {
        activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(interstitials.containsKey(id)) {
                        Interstitial interstitial = interstitials.get(id);
                        interstitial.show();
                    } else {
                        Log.w(TAG, "showInterstitial: Interstitial not found " + id);
                    }
                }
            });
    }

    /* Definitions
     * ********************************************************************** */

    /**
     * Initilization Singleton
     * @param Activity The main activity
     */
    static public Godot.SingletonBase initialize(Activity activity)
    {
        return new GodotSmaato(activity);
    }

    /**
     * Constructor
     * @param Activity Main activity
     */
    public GodotSmaato(Activity p_activity) {
        registerClass("Smaato", new String[] {
                "init",
                "initWithContentRating",
                // banner
                "loadBanner", "showBanner", "hideBanner", "removeBanner", "getBannerWidth", "getBannerHeight", 
                "makeZombieBanner", "killZombieBanner",
                // Interstitial
                "loadInterstitial", "showInterstitial",
                // Rewarded video
                "loadRewardedVideo", "showRewardedVideo"
            });
        activity = p_activity;
    }
}
