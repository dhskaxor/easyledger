package com.sunwoo.easyledger.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.data.entity.DayTotal;
import com.sunwoo.easyledger.databinding.FragmentCalendarBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.ui.transaction.TransactionDetailDialog;
import com.sunwoo.easyledger.ui.transaction.TransactionListAdapter;

import java.time.LocalDate;
import java.util.List;

public class CalendarFragment extends Fragment {
    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private CalendarDayAdapter calendarAdapter;
    private TransactionListAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
        calendarAdapter = new CalendarDayAdapter(viewModel::select);
        listAdapter = new TransactionListAdapter(item -> TransactionDetailDialog.show(this, item.id));
        binding.calendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.calendarGrid.setAdapter(calendarAdapter);
        binding.dayList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.dayList.setAdapter(listAdapter);
        binding.prevMonth.setOnClickListener(v -> viewModel.shift(-1));
        binding.nextMonth.setOnClickListener(v -> viewModel.shift(1));
        binding.todayButton.setOnClickListener(v -> viewModel.goToday());
        binding.addFab.setOnClickListener(v -> openInput());
        applyPendingDate();
        viewModel.month.observe(getViewLifecycleOwner(), month -> binding.monthLabel.setText(DateTimes.formatMonth(month)));
        viewModel.selected.observe(getViewLifecycleOwner(), this::renderSelection);
        viewModel.totals.observe(getViewLifecycleOwner(), totals -> renderSelection(viewModel.selected.getValue()));
        viewModel.dayItems.observe(getViewLifecycleOwner(), items -> {
            listAdapter.submit(items);
            boolean empty = items == null || items.isEmpty();
            binding.dayEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void renderSelection(LocalDate selected) {
        if (selected == null || viewModel.month.getValue() == null) {
            return;
        }
        List<DayTotal> totals = viewModel.totals.getValue();
        calendarAdapter.setMonth(viewModel.month.getValue(), selected, totals);
        long income = 0;
        long expense = 0;
        if (totals != null) {
            for (DayTotal total : totals) {
                income += total.income;
                expense += total.expense;
            }
        }
        binding.incomeTotal.setText(Money.won(income));
        binding.expenseTotal.setText(Money.won(expense));
        long balance = income - expense;
        binding.balanceTotal.setText(Money.signedWon(balance));
        binding.balanceTotal.setTextColor(requireContext().getColor(
                balance > 0 ? com.sunwoo.easyledger.R.color.income : balance < 0 ? com.sunwoo.easyledger.R.color.expense : com.sunwoo.easyledger.R.color.neutral));
        binding.selectedLabel.setText(DateTimes.formatDay(selected));
    }

    @Override
    public void onResume() {
        super.onResume();
        applyPendingDate();
    }

    private void applyPendingDate() {
        if (viewModel == null) {
            return;
        }
        String selectedDate = getArguments() == null ? null : getArguments().getString("selectedDate");
        if (selectedDate != null && !selectedDate.isEmpty()) {
            viewModel.select(LocalDate.parse(selectedDate));
            getArguments().remove("selectedDate");
            return;
        }
        if (requireActivity() instanceof com.sunwoo.easyledger.ui.MainActivity) {
            String pending = ((com.sunwoo.easyledger.ui.MainActivity) requireActivity()).consumePendingCalendarDate();
            if (pending != null && !pending.isEmpty()) {
                viewModel.select(LocalDate.parse(pending));
            }
        }
    }

    private void openInput() {
        Bundle args = new Bundle();
        LocalDate selected = viewModel.selected.getValue();
        if (selected != null) {
            args.putString("prefillDate", selected.toString());
        }
        args.putBoolean("returnToCalendar", true);
        int startId = androidx.navigation.NavGraph.findStartDestination(
                NavHostFragment.findNavController(this).getGraph()).getId();
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(startId, false, true)
                .build();
        NavHostFragment.findNavController(this).navigate(R.id.inputFragment, args, options);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
