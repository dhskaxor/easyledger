package com.sunwoo.easyledger.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.app.TodayExpenseWidgetProvider;
import com.sunwoo.easyledger.app.WidgetUpdater;
import com.sunwoo.easyledger.common.WindowInsetsHelper;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.databinding.ActivityMainBinding;
import com.sunwoo.easyledger.ui.onboarding.OnboardingActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private static final long SPLASH_MIN_MS = 1000L;

    private ActivityMainBinding binding;
    private NavController navController;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ready;
    private boolean dataReady;
    private boolean minTimePassed;
    private boolean needOnboarding;
    private boolean entering;
    private boolean openInputPending;
    private boolean openCalendarPending;
    private String pendingCalendarDate;
    private int lastRootDestinationId = View.NO_ID;
    private int rootVisitCount;
    private final ActivityResultLauncher<Intent> onboardingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                enterApp();
                if (result.getResultCode() == RESULT_OK) {
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(R.string.onboarding_category_tip)
                            .setPositiveButton(R.string.confirm, null)
                            .setNeutralButton(R.string.open_category_manage, (dialog, which) -> {
                                if (navController != null) {
                                    navController.navigate(R.id.categoryManageFragment);
                                }
                            })
                            .show();
                }
            });
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applySystemBars(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = host.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        navController.addOnDestinationChangedListener(this::onDestinationChanged);
        captureDeepLink(getIntent());
        handler.postDelayed(() -> {
            minTimePassed = true;
            tryEnter();
        }, SPLASH_MIN_MS);
        EasyLedgerApp app = (EasyLedgerApp) getApplication();
        EasyLedgerDatabase.executor().execute(() -> {
            app.categories().ensureSeeded();
            boolean need = app.categories().needsOnboarding();
            runOnUiThread(() -> {
                needOnboarding = need;
                dataReady = true;
                tryEnter();
            });
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        captureDeepLink(intent);
        if (ready) {
            maybeOpenDeepLink();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ready) {
            return;
        }
        EasyLedgerApp app = (EasyLedgerApp) getApplication();
        EasyLedgerDatabase.executor().execute(() -> {
            boolean need = app.categories().needsOnboarding();
            if (need) {
                runOnUiThread(() -> {
                    ready = false;
                    binding.mainContent.setVisibility(View.INVISIBLE);
                    binding.splash.setVisibility(View.VISIBLE);
                    onboardingLauncher.launch(new Intent(this, OnboardingActivity.class));
                });
            }
        });
    }

    private void tryEnter() {
        if (!dataReady || !minTimePassed || entering || isFinishing()) {
            return;
        }
        entering = true;
        if (needOnboarding) {
            onboardingLauncher.launch(new Intent(this, OnboardingActivity.class));
            return;
        }
        enterApp();
    }

    private void enterApp() {
        ready = true;
        entering = false;
        binding.splash.setVisibility(View.GONE);
        binding.mainContent.setVisibility(View.VISIBLE);
        requestNotificationPermission();
        WidgetUpdater.updateAll(this);
        maybeOpenDeepLink();
    }

    private void captureDeepLink(Intent intent) {
        if (intent == null) {
            return;
        }
        if (intent.getBooleanExtra(TodayExpenseWidgetProvider.EXTRA_OPEN_INPUT, false)) {
            openInputPending = true;
            openCalendarPending = false;
            intent.removeExtra(TodayExpenseWidgetProvider.EXTRA_OPEN_INPUT);
        }
        if (intent.getBooleanExtra(TodayExpenseWidgetProvider.EXTRA_OPEN_CALENDAR, false)) {
            openCalendarPending = true;
            openInputPending = false;
            pendingCalendarDate = intent.getStringExtra(TodayExpenseWidgetProvider.EXTRA_SELECTED_DATE);
            intent.removeExtra(TodayExpenseWidgetProvider.EXTRA_OPEN_CALENDAR);
            intent.removeExtra(TodayExpenseWidgetProvider.EXTRA_SELECTED_DATE);
        }
    }

    private void maybeOpenDeepLink() {
        if (navController == null || binding == null) {
            return;
        }
        if (openCalendarPending) {
            openCalendarPending = false;
            String date = pendingCalendarDate == null || pendingCalendarDate.isEmpty()
                    ? java.time.LocalDate.now().toString()
                    : pendingCalendarDate;
            pendingCalendarDate = date;
            Bundle args = new Bundle();
            args.putString("selectedDate", date);
            navigateTopLevel(R.id.calendarFragment, args);
            return;
        }
        if (openInputPending) {
            openInputPending = false;
            pendingCalendarDate = null;
            navigateTopLevel(R.id.inputFragment, null);
        }
    }

    /** Bottom-nav safe navigation that keeps sibling tabs switchable. */
    private void navigateTopLevel(int destinationId, Bundle args) {
        int startId = NavGraph.findStartDestination(navController.getGraph()).getId();
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(startId, false, true)
                .build();
        try {
            navController.navigate(destinationId, args, options);
        } catch (IllegalArgumentException ignored) {
            // Already on destination or graph temporarily unavailable.
        }
    }

    public void prepareCalendarDate(String date) {
        pendingCalendarDate = date;
    }

    /** Consumed by CalendarFragment when opened from the widget. */
    public String consumePendingCalendarDate() {
        String date = pendingCalendarDate;
        pendingCalendarDate = null;
        return date;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void onDestinationChanged(
            @NonNull NavController controller,
            @NonNull NavDestination destination,
            Bundle arguments
    ) {
        boolean root = destination.getId() == R.id.inputFragment
                || destination.getId() == R.id.calendarFragment
                || destination.getId() == R.id.historyFragment
                || destination.getId() == R.id.statisticsFragment
                || destination.getId() == R.id.settingsFragment;
        binding.bottomNav.setVisibility(root ? View.VISIBLE : View.GONE);
        binding.toolbar.setVisibility(root ? View.GONE : View.VISIBLE);
        if (getSupportActionBar() != null) {
            CharSequence title = destination.getLabel();
            getSupportActionBar().setTitle(root || title == null ? "" : title);
        }
        if (root) {
            binding.toolbar.setNavigationIcon(null);
            maybeOfferInterstitial(destination.getId());
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_back);
            binding.toolbar.setNavigationContentDescription(R.string.back);
            binding.toolbar.setNavigationOnClickListener(view -> navController.popBackStack());
        }
    }

    /** 내역/통계 탭 진입 시 전면 광고 (첫 진입·입력 탭은 제외, AdManager에서 횟수/간격 제한). */
    private void maybeOfferInterstitial(int destinationId) {
        if (!ready) {
            lastRootDestinationId = destinationId;
            return;
        }
        boolean changed = destinationId != lastRootDestinationId;
        lastRootDestinationId = destinationId;
        if (!changed) {
            return;
        }
        rootVisitCount++;
        boolean historyOrStats = destinationId == R.id.historyFragment
                || destinationId == R.id.statisticsFragment;
        // 앱 켠 직후 첫 루트 화면은 건너뛰고, 내역/통계로 옮길 때만
        if (!historyOrStats || rootVisitCount < 2) {
            return;
        }
        EasyLedgerApp app = (EasyLedgerApp) getApplication();
        app.ads().maybeShowInterstitial(this);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
