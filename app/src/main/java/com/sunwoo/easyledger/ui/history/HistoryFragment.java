package com.sunwoo.easyledger.ui.history;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.data.entity.IncomeExpense;
import com.sunwoo.easyledger.data.entity.TxListItem;
import com.sunwoo.easyledger.databinding.FragmentHistoryBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.sunwoo.easyledger.ui.transaction.TransactionDetailDialog;
import com.sunwoo.easyledger.ui.transaction.TransactionListAdapter;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private TransactionListAdapter adapter;
    private boolean updatingToggle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        adapter = new TransactionListAdapter(item -> TransactionDetailDialog.show(this, item.id));
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.prevMonth.setOnClickListener(v -> viewModel.shiftMonth(-1));
        binding.nextMonth.setOnClickListener(v -> viewModel.shiftMonth(1));
        binding.filterButton.setOnClickListener(v -> SearchDialog.create(viewModel).show(getChildFragmentManager(), "search"));
        binding.typeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || updatingToggle) {
                return;
            }
            if (checkedId == R.id.chip_income) {
                viewModel.setType(TransactionType.INCOME.name());
            } else if (checkedId == R.id.chip_expense) {
                viewModel.setType(TransactionType.EXPENSE.name());
            } else {
                viewModel.setType(null);
            }
            styleTypeToggle(checkedId);
        });
        viewModel.title.observe(getViewLifecycleOwner(), binding.monthLabel::setText);
        viewModel.items.observe(getViewLifecycleOwner(), this::show);
        viewModel.monthTotals.observe(getViewLifecycleOwner(), this::showMonthTotals);
        syncTypeToggle();
        binding.adBannerContainer.post(() -> {
            if (!isAdded() || binding == null) {
                return;
            }
            EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
            app.ads().bindBanner(requireActivity(), binding.adBannerContainer);
        });
        getChildFragmentManager().setFragmentResultListener(SearchDialog.REQUEST, getViewLifecycleOwner(), (key, bundle) -> {
            if (bundle.getBoolean("clear")) {
                viewModel.clearFilters();
                return;
            }
            viewModel.apply(
                    new HashSet<>(bundle.getStringArrayList("categories")),
                    bundle.getString("memo"),
                    bundle.containsKey("min") ? bundle.getLong("min") : null,
                    bundle.containsKey("max") ? bundle.getLong("max") : null,
                    LocalDate.parse(bundle.getString("start")),
                    LocalDate.parse(bundle.getString("end")),
                    bundle.getString("payment")
            );
        });
    }

    private void show(List<TxListItem> items) {
        adapter.submit(items);
        boolean empty = items == null || items.isEmpty();
        binding.emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        long income = 0;
        long expense = 0;
        int count = items == null ? 0 : items.size();
        if (items != null) {
            for (TxListItem item : items) {
                if (TransactionType.INCOME.name().equals(item.type)) {
                    income += item.amount;
                } else {
                    expense += item.amount;
                }
            }
        }
        binding.resultSummary.setText(getString(R.string.history_filter_summary,
                count, Money.won(income), Money.won(expense)));
        syncTypeToggle();
    }

    private void showMonthTotals(IncomeExpense totals) {
        long income = totals == null ? 0L : totals.income;
        long expense = totals == null ? 0L : totals.expense;
        long balance = income - expense;
        binding.monthIncome.setText(Money.won(income));
        binding.monthExpense.setText(Money.won(expense));
        binding.monthBalance.setText(Money.signedWon(balance));
        binding.monthBalance.setTextColor(requireContext().getColor(
                balance > 0 ? R.color.income : balance < 0 ? R.color.expense : R.color.neutral));
    }

    private void syncTypeToggle() {
        int checked = viewModel.type() == null ? R.id.chip_all
                : TransactionType.INCOME.name().equals(viewModel.type()) ? R.id.chip_income : R.id.chip_expense;
        if (binding.typeToggle.getCheckedButtonId() != checked) {
            updatingToggle = true;
            binding.typeToggle.check(checked);
            updatingToggle = false;
        }
        styleTypeToggle(checked);
    }

    private void styleTypeToggle(int checkedId) {
        styleChip(binding.chipAll, checkedId == R.id.chip_all, R.color.brand, R.color.brand_soft, R.color.brand_deep);
        styleChip(binding.chipIncome, checkedId == R.id.chip_income, R.color.income, R.color.income_soft, R.color.income_deep);
        styleChip(binding.chipExpense, checkedId == R.id.chip_expense, R.color.expense, R.color.expense_soft, R.color.expense_deep);
    }

    private void styleChip(MaterialButton button, boolean selected, int fill, int soft, int deep) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                requireContext().getColor(selected ? fill : soft)));
        button.setTextColor(requireContext().getColor(selected ? R.color.on_brand : deep));
        button.setStrokeWidth(0);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
            app.ads().unbindBanner(binding.adBannerContainer);
        }
        binding = null;
        super.onDestroyView();
    }
}
