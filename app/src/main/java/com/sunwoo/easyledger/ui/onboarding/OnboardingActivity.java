package com.sunwoo.easyledger.ui.onboarding;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.IconCatalog;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.common.WindowInsetsHelper;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.database.LibraryItem;
import com.sunwoo.easyledger.databinding.ActivityOnboardingBinding;
import com.sunwoo.easyledger.databinding.ItemOnboardingCategoryBinding;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnboardingActivity extends AppCompatActivity {
    private static final int STEP_WELCOME = 0;
    private static final int STEP_EXPENSE = 1;
    private static final int STEP_INCOME = 2;

    private ActivityOnboardingBinding binding;
    private int step = STEP_WELCOME;
    private final Set<String> selected = new HashSet<>();
    private PickAdapter adapter;
    private boolean finishing;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowInsetsHelper.applySystemBars(binding.getRoot());
        adapter = new PickAdapter();
        binding.pickList.setLayoutManager(new GridLayoutManager(this, 3));
        binding.pickList.setAdapter(adapter);
        binding.welcomeNext.setOnClickListener(v -> goTo(STEP_EXPENSE));
        binding.pickBack.setOnClickListener(v -> {
            if (step == STEP_INCOME) {
                goTo(STEP_EXPENSE);
            } else {
                goTo(STEP_WELCOME);
            }
        });
        binding.pickNext.setOnClickListener(v -> {
            if (step == STEP_EXPENSE) {
                goTo(STEP_INCOME);
            } else {
                finishOnboarding();
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (step == STEP_INCOME) {
                    goTo(STEP_EXPENSE);
                } else if (step == STEP_EXPENSE) {
                    goTo(STEP_WELCOME);
                }
            }
        });
        showStep();
    }

    private void goTo(int next) {
        step = next;
        showStep();
    }

    private void showStep() {
        boolean welcome = step == STEP_WELCOME;
        binding.welcomePage.setVisibility(welcome ? View.VISIBLE : View.GONE);
        binding.pickPage.setVisibility(welcome ? View.GONE : View.VISIBLE);
        if (welcome) {
            return;
        }
        boolean expense = step == STEP_EXPENSE;
        binding.stepLabel.setText(expense ? getString(R.string.onboarding_step_1) : getString(R.string.onboarding_step_2));
        binding.pickTitle.setText(expense
                ? R.string.onboarding_expense_title
                : R.string.onboarding_income_title);
        binding.pickBody.setText(expense
                ? R.string.onboarding_expense_body
                : R.string.onboarding_income_body);
        binding.pickNext.setText(expense ? R.string.next : R.string.onboarding_done);
        adapter.setItems(LibraryItem.roots(
                expense ? TransactionType.EXPENSE.name() : TransactionType.INCOME.name()));
    }

    private void finishOnboarding() {
        if (finishing) {
            return;
        }
        finishing = true;
        binding.pickNext.setEnabled(false);
        EasyLedgerApp app = (EasyLedgerApp) getApplication();
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                app.categories().completeOnboarding(new ArrayList<>(selected));
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (RuntimeException exception) {
                AppLog.error("Onboarding", "complete", exception);
                runOnUiThread(() -> {
                    finishing = false;
                    binding.pickNext.setEnabled(true);
                    Toast.makeText(this, UserMessages.from(exception), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private class PickAdapter extends RecyclerView.Adapter<PickAdapter.Holder> {
        private final List<LibraryItem> items = new ArrayList<>();

        void setItems(List<LibraryItem> next) {
            items.clear();
            items.addAll(next);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemOnboardingCategoryBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            LibraryItem item = items.get(position);
            boolean on = selected.contains(item.id);
            IconCatalog.apply(holder.binding.icon, item.icon);
            holder.binding.name.setText(item.name);
            MaterialCardView card = (MaterialCardView) holder.binding.getRoot();
            card.setChecked(on);
            int stroke = ContextCompat.getColor(OnboardingActivity.this,
                    on ? R.color.brand : R.color.line);
            card.setStrokeColor(stroke);
            card.setCardBackgroundColor(ContextCompat.getColor(OnboardingActivity.this,
                    on ? R.color.brand_soft : R.color.card));
            card.setOnClickListener(v -> {
                if (selected.contains(item.id)) {
                    selected.remove(item.id);
                } else {
                    selected.add(item.id);
                }
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemOnboardingCategoryBinding binding;

            Holder(ItemOnboardingCategoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
