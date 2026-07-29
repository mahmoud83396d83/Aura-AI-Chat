package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"
    
    // User's Interstitial Ad Unit ID
    const val AD_UNIT_ID = "ca-app-pub-1270885163968679/9240638723"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var messageCounter = 0
    private const val MESSAGES_BETWEEN_ADS = 3

    fun init(context: Context) {
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob initialized: $initializationStatus")
                loadInterstitial(context.applicationContext)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadInterstitial(context: Context) {
        try {
            if (interstitialAd != null || isLoading) return

            isLoading = true
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                context,
                AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "Interstitial ad loaded successfully.")
                        interstitialAd = ad
                        isLoading = false
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                        interstitialAd = null
                        isLoading = false
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading interstitial ad", e)
            isLoading = false
        }
    }

    fun showInterstitial(activity: Activity, onAdDismissed: (() -> Unit)? = null) {
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                onAdDismissed?.invoke()
                return
            }

            val ad = interstitialAd
            if (ad != null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad dismissed.")
                        interstitialAd = null
                        loadInterstitial(activity.applicationContext)
                        onAdDismissed?.invoke()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                        interstitialAd = null
                        loadInterstitial(activity.applicationContext)
                        onAdDismissed?.invoke()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial ad showed full screen.")
                    }
                }
                ad.show(activity)
            } else {
                Log.d(TAG, "Interstitial ad not ready yet. Loading for next time.")
                loadInterstitial(activity.applicationContext)
                onAdDismissed?.invoke()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error showing interstitial ad safely", e)
            onAdDismissed?.invoke()
        }
    }

    /**
     * Call when user sends a message. Shows ad every 3 messages.
     */
    fun onUserAction(activity: Activity) {
        try {
            messageCounter++
            if (messageCounter >= MESSAGES_BETWEEN_ADS) {
                messageCounter = 0
                showInterstitial(activity)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error handling user action ad", e)
        }
    }

    /**
     * Call when switching chat sessions or creating new session.
     */
    fun onSessionChanged(activity: Activity) {
        try {
            showInterstitial(activity)
        } catch (e: Throwable) {
            Log.e(TAG, "Error handling session changed ad", e)
        }
    }
}

