package org.godotengine.godot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.widget.FrameLayout;
import android.view.ViewGroup.LayoutParams;
import android.provider.Settings;
import android.graphics.Color;
import android.util.Log;
import java.util.Locale;
import android.view.Gravity;
import android.view.View;
import android.os.Bundle;

import com.smaato.sdk.core.Config;
import com.smaato.sdk.core.SmaatoSdk;
import com.smaato.sdk.core.log.LogLevel;
import com.smaato.sdk.banner.widget.BannerView;
import com.smaato.sdk.banner.ad.BannerAdSize;
import com.smaato.sdk.banner.widget.BannerError;
import com.smaato.sdk.interstitial.Interstitial;
import com.smaato.sdk.interstitial.InterstitialAd;
import com.smaato.sdk.interstitial.InterstitialError;
import com.smaato.sdk.interstitial.InterstitialRequestError;
//import com.smaato.sdk.interstitial.EventListener;
import com.smaato.sdk.rewarded.RewardedError;
import com.smaato.sdk.rewarded.RewardedRequestError;
import com.smaato.sdk.rewarded.RewardedInterstitial;
import com.smaato.sdk.rewarded.RewardedInterstitialAd;
//import com.smaato.sdk.rewarded.EventListener;

public class GodotSmaato extends Godot.SingletonBase
{

    private final String TAG = GodotSmaato.class.getName();
    private Activity activity = null; // The main activity of the game

    private HashMap<String, View> zombieBanners = new HashMap<>();
    private HashMap<String, FrameLayout.LayoutParams> bannerParams = new HashMap<>();
    private HashMap<String, InterstitialAd> interstitials = new HashMap<>();
    private HashMap<String, BannerView> banners = new HashMap<>();
    private HashMap<String, RewardedInterstitialAd> rewardeds = new HashMap<>();

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
    public void init(final String publisherId, boolean ProductionMode) {

        layout = (FrameLayout)activity.getWindow().getDecorView().getRootView();
        this.ProductionMode = ProductionMode;
        activity.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    try {
                        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
                        //SharedPreferences.Editor editor = sharedPref.edit();
                        //editor.putString("IABConsent_CMPPresent", "YES");
                        //editor.putString("IABConsent_SubjectToGDPR", "0");
                        //editor.commit();

                        Config.ConfigBuilder builder = Config.builder();
                        if(!ProductionMode)
                            builder.setLogLevel(LogLevel.DEBUG);
                        else
                            builder.setLogLevel(LogLevel.WARNING);
                        builder.setHttpsOnly(false);
                        Config config = builder.build();
                        SmaatoSdk.init(activity.getApplication(), config, publisherId);

                        // additional information

                        SmaatoSdk.setRegion(Locale.getDefault().getCountry());
                        SmaatoSdk.setLanguage(Locale.getDefault().getLanguage());
                        SmaatoSdk.setGPSEnabled(true);

                        /*
                          SmaatoSdk.setKeywords(...);
                          SmaatoSdk.setSearchQuery(...);
                          SmaatoSdk.setGender(...);
                          SmaatoSdk.setAge(...);
                          SmaatoSdk.setLatLng(...);
                          SmaatoSdk.setZip(...);
                          SmaatoSdk.setCoppa(...);
                          SmaatoSdk.setWatermarkEnabled(...);
                        */

                        Log.d(TAG, "Smaato: inited with publisher id " + SmaatoSdk.getPublisherId() + " ver " + SmmatoSdk.getVersion());
                        //initRewardedVideo("130908160", 0);
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                        e.printStackTrace();
                    }
                }
            });
    }

    /* Rewarded Video
     * ********************************************************************** */
    private void initRewardedVideo(final String id, final int callback_id)
    {
        Log.w(TAG, "Smaato: Prepare rewarded video: "+id+" callback: "+Integer.toString(callback_id));
        
        RewardedInterstitial.loadAd(id, new com.smaato.sdk.rewarded.EventListener() {
                public void onAdLoaded(RewardedInterstitialAd rewardedInterstitialAd) {
                    rewardeds.put(id, rewardedInterstitialAd);
                    Log.w(TAG, "Smaato: onAdLoaded");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_loaded", new Object[] { id });
                }
                public void onAdFailedToLoad(RewardedRequestError rewardedRequestError) {
                    Log.w(TAG, "Smaato: onAdFailedToLoad. error: " + rewardedRequestError.toString());
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, rewardedRequestError.toString() });
                }
                public void onAdError(RewardedInterstitialAd rewardedInterstitialAd, RewardedError rewardedError) {
                    Log.w(TAG, "Smaato: onAdError. error: " + rewardedError.toString());
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, rewardedError.toString() });
                }
                public void onAdClosed(RewardedInterstitialAd rewardedInterstitialAd) {
                    Log.w(TAG, "Smaato: onAdClosed");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_closed", new Object[] { id });
                }
                public void onAdClicked(RewardedInterstitialAd rewardedInterstitialAd) {
                    Log.w(TAG, "Smaato: onAdClicked");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_left_application", new Object[] { id });
                }
                public void onAdStarted(RewardedInterstitialAd rewardedInterstitialAd) {
                    Log.w(TAG, "Smaato: onAdStarted");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_started", new Object[] { id });
                }
                public void onAdReward(RewardedInterstitialAd rewardedInterstitialAd) {
                    Log.w(TAG, "Smaato: " + String.format(" onRewarded!"));
                    GodotLib.calldeferred(callback_id, "_on_rewarded", new Object[] { id, "reward", 1 });
                }
                @Override
                public void onAdTTLExpired(RewardedInterstitialAd rewardedInterstitialAd) {
                    Log.w(TAG, "Smaato: onAdTTLExpired.");
                    GodotLib.calldeferred(callback_id, "_on_rewarded_video_ad_failed_to_load", new Object[] { id, "Ad TTL expired!" });
                }
            });
    }

    /**
     * Load a Rewarded Video
     * @param String id AdMod Rewarded video ID
     */
    public void loadRewardedVideo(final String id, final int callback_id) {
        activity.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    try {
                        initRewardedVideo(id, callback_id);
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                        e.printStackTrace();
                    }
                }
            });
    }

    /**
     * Show a Rewarded Video
     */
    public void showRewardedVideo(final String id) {
        activity.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    if(rewardeds.containsKey(id)) {
                        RewardedInterstitialAd r = rewardeds.get(id);
                        if (r.isAvailableForPresentation()) {
                            r.showAd();
                        } else {
                            Log.w(TAG, "Smaato: showRewardedVideo - rewarded not loaded");
                        }
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

        banner.setEventListener(new BannerView.EventListener() {
                @Override
                public void onAdLoaded(BannerView bannerView) {
                    Log.w(TAG, "Smaato: onAdLoaded");
                    GodotLib.calldeferred(callback_id, "_on_banner_loaded", new Object[]{ id });
                }
                @Override
                public void onAdFailedToLoad(BannerView bannerView, BannerError bannerError) {
                    String  str;
                    Log.w(TAG, "Smaato: onAdFailedToLoad -> " + bannerError.toString());
                    GodotLib.calldeferred(callback_id, "_on_banner_failed_to_load", new Object[]{ id, bannerError.toString() });
                }
                @Override
                public void onAdImpression(BannerView bannerView) {
                    Log.w(TAG, "Smaato: onAdImpression");
                }
                @Override
                public void onAdClicked(BannerView bannerView) {
                    Log.w(TAG, "Smaato: onAdClicked");
                }
                @Override
                public void onAdTTLExpired(BannerView bannerView) {
                    Log.w(TAG, "Smaato: onAdTTLExpired");
                    GodotLib.calldeferred(callback_id, "_on_banner_failed_to_load", new Object[]{ id, "Ad TTL expired!" });
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
                        BannerAdSize adSize = BannerAdSize.XX_LARGE_320x50;
                        b.loadAd(id, adSize);
                    } else {
                        BannerView b = banners.get(id);
                        b.loadAd(id, BannerAdSize.XX_LARGE_320x50);
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
            if(b != null)
                return 320; // b.getAdSize().getWidthInPixels(activity);
            else
                return 0;
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
            if(b != null)
                return 50; // b.getAdSize().getHeightInPixels(activity);
            else
                return 0;
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

    private void initInterstitial(final String id, final int callback_id)
    {
        Log.w(TAG, "Smaato: Prepare interstitial: "+id+" callback: "+Integer.toString(callback_id));

        Interstitial.loadAd(id, new com.smaato.sdk.interstitial.EventListener() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    Log.w(TAG, "Smaato: onAdLoaded");
                    interstitials.put(id, interstitialAd);
                    GodotLib.calldeferred(callback_id, "_on_interstitial_loaded", new Object[] { id });
                }
                @Override
                public void onAdFailedToLoad(InterstitialRequestError interstitialRequestError) {
                    Log.w(TAG, "Smaato: onAdFailedToLoad - error: " + interstitialRequestError.toString());
                    GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, interstitialRequestError.toString() });
                }
                @Override
                public void onAdError(InterstitialAd interstitialAd, InterstitialError interstitialError) {
                    Log.w(TAG, "Smaato: onAdError - error: " + interstitialError.toString());
                    GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, interstitialError.toString() });
                }
                @Override
                public void onAdOpened(InterstitialAd interstitialAd) {
                    Log.w(TAG, "Smaato: onAdOpened()");
                }
                @Override
                public void onAdClosed(InterstitialAd interstitialAd) {
                    Log.w(TAG, "Smaato: onAdClosed");
                    GodotLib.calldeferred(callback_id, "_on_interstitial_close", new Object[] { id });
                }
                @Override
                public void onAdClicked(InterstitialAd interstitialAd) {
                    Log.w(TAG, "Smaato: onAdClicked");
                }
                @Override
                public void onAdImpression(InterstitialAd interstitialAd) {
                    Log.w(TAG, "Smaato: onAdImpression");
                }
                @Override
                public void onAdTTLExpired(InterstitialAd interstitialAd) {
                    Log.w(TAG, "Smaato: onAdTTLExpired");
                    GodotLib.calldeferred(callback_id, "_on_interstitial_failed_to_load", new Object[] { id, "Ad TTL expired!" });
                }
            });
    }

    /**
     * Load a interstitial
     * @param String id AdMod Interstitial ID
     */
    public void loadInterstitial(final String id, final int callback_id)
    {
        activity.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    initInterstitial(id, callback_id);
                }
            });
    }

    /**
     * Show the interstitial
     */
    public void showInterstitial(final String id)
    {
        activity.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    if(interstitials.containsKey(id)) {
                        InterstitialAd interstitial = interstitials.get(id);
                        if (interstitial.isAvailableForPresentation()) {
                            interstitial.showAd(activity);
                        } else {
                            Log.w(TAG, "Smaato: showInterstitial - interstitial not loaded");
                        }
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
