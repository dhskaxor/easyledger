package com.sunwoo.easyledger.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.sunwoo.easyledger.BuildConfig;
import com.sunwoo.easyledger.common.AppLog;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 광고 노출 정책:
 * - 배너: 내역/통계
 * - 전면: 내역·통계 진입 시, 최소 3시간 간격(하루 횟수 제한 없음)
 * - 보상형: 백업 전(실패·스킵해도 백업은 진행)
 */
public final class AdManager {
    private static final String PREFS = "easyledger_ads";
    private static final String KEY_INTERSTITIAL_LAST_AT = "interstitial_last_at";
    private static final long INTERSTITIAL_COOLDOWN_MS = 3L * 60L * 60L * 1000L; // 3시간

    private final Context appContext;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Nullable
    private InterstitialAd interstitialAd;
    @Nullable
    private RewardedAd rewardedAd;
    private boolean loadingInterstitial;
    private boolean loadingRewarded;
    private long lastInterstitialAt;

    public AdManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.lastInterstitialAt = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_INTERSTITIAL_LAST_AT, 0L);
    }

    public void initialize(@Nullable Runnable onReady) {
        if (initialized.get()) {
            if (onReady != null) {
                main.post(onReady);
            }
            return;
        }
        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder()
                        .setTestDeviceIds(Arrays.asList(AdRequest.DEVICE_ID_EMULATOR))
                        .build()
        );
        MobileAds.initialize(appContext, status -> {
            initialized.set(true);
            preloadInterstitial();
            preloadRewarded();
            if (onReady != null) {
                main.post(onReady);
            }
        });
    }

    public void bindBanner(@NonNull Activity activity, @NonNull FrameLayout container) {
        container.removeAllViews();
        AdView adView = new AdView(activity);
        adView.setAdUnitId(BuildConfig.ADMOB_BANNER_ID);
        AdSize size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                activity, containerWidthDp(activity, container));
        adView.setAdSize(size);
        container.addView(adView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        adView.loadAd(new AdRequest.Builder().build());
        container.setTag(adView);
    }

    public void unbindBanner(@Nullable FrameLayout container) {
        if (container == null) {
            return;
        }
        Object tag = container.getTag();
        if (tag instanceof AdView) {
            ((AdView) tag).destroy();
            container.setTag(null);
        }
        container.removeAllViews();
    }

    public void maybeShowInterstitial(@NonNull Activity activity) {
        if (!canShowInterstitial()) {
            preloadInterstitial();
            return;
        }
        InterstitialAd ad = interstitialAd;
        if (ad == null) {
            preloadInterstitial();
            return;
        }
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                markInterstitialShown();
                preloadInterstitial();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                AppLog.error("Ads", "interstitial show", new RuntimeException(adError.getMessage()));
                preloadInterstitial();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                lastInterstitialAt = System.currentTimeMillis();
            }
        });
        ad.show(activity);
    }

    /**
     * 보상형 광고를 보여 준 뒤 항상 {@code onContinue}를 호출합니다.
     * 로드 실패·닫기·스킵과 관계없이 백업 흐름이 막히지 않게 fail-open 합니다.
     */
    public void runWithRewardedOptional(
            @NonNull Activity activity,
            @NonNull Runnable onContinue
    ) {
        RewardedAd ad = rewardedAd;
        if (ad == null) {
            preloadRewarded();
            onContinue.run();
            return;
        }
        rewardedAd = null;
        final boolean[] continued = {false};
        Runnable once = () -> {
            if (continued[0]) {
                return;
            }
            continued[0] = true;
            onContinue.run();
        };
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preloadRewarded();
                once.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                AppLog.error("Ads", "rewarded show", new RuntimeException(adError.getMessage()));
                preloadRewarded();
                once.run();
            }
        });
        ad.show(activity, rewardItem -> {
            // 보상 획득 — 백업은 dismiss/실패와 동일하게 진행
        });
    }

    private boolean canShowInterstitial() {
        long now = System.currentTimeMillis();
        return now - lastInterstitialAt >= INTERSTITIAL_COOLDOWN_MS;
    }

    private void markInterstitialShown() {
        lastInterstitialAt = System.currentTimeMillis();
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_INTERSTITIAL_LAST_AT, lastInterstitialAt)
                .remove("interstitial_day")
                .remove("interstitial_count")
                .apply();
    }

    private void preloadInterstitial() {
        if (loadingInterstitial || interstitialAd != null) {
            return;
        }
        loadingInterstitial = true;
        InterstitialAd.load(
                appContext,
                BuildConfig.ADMOB_INTERSTITIAL_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loadingInterstitial = false;
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        loadingInterstitial = false;
                        interstitialAd = null;
                        AppLog.error("Ads", "interstitial load", new RuntimeException(loadAdError.getMessage()));
                    }
                }
        );
    }

    private void preloadRewarded() {
        if (loadingRewarded || rewardedAd != null) {
            return;
        }
        loadingRewarded = true;
        RewardedAd.load(
                appContext,
                BuildConfig.ADMOB_REWARDED_ID,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        loadingRewarded = false;
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        loadingRewarded = false;
                        rewardedAd = null;
                        AppLog.error("Ads", "rewarded load", new RuntimeException(loadAdError.getMessage()));
                    }
                }
        );
    }

    private static int containerWidthDp(Activity activity, FrameLayout container) {
        float density = activity.getResources().getDisplayMetrics().density;
        int widthPx = container.getWidth();
        if (widthPx <= 0) {
            widthPx = activity.getResources().getDisplayMetrics().widthPixels;
        }
        return Math.max(320, Math.round(widthPx / density));
    }
}
