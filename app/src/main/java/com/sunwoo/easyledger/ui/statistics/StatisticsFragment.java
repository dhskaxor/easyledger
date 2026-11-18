package com.sunwoo.easyledger.ui.statistics;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.databinding.FragmentStatisticsBinding;
import com.sunwoo.easyledger.databinding.ItemCategoryStatBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.calculator.NamedAmount;
import com.sunwoo.easyledger.domain.calculator.StatisticsReport;

import java.util.ArrayList;
import java.util.List;

public class StatisticsFragment extends Fragment {
    private FragmentStatisticsBinding binding;
    private StatisticsViewModel viewModel;
    private final DetailAdapter adapter = new DetailAdapter();
    private boolean updatingSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
        binding.categoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.categoryList.setAdapter(adapter);
        binding.prevMonth.setOnClickListener(v -> viewModel.shift(-1));
        binding.nextMonth.setOnClickListener(v -> viewModel.shift(1));
        binding.includeFixed.setOnCheckedChangeListener((button, checked) -> {
            if (!updatingSwitch) {
                viewModel.setIncludeFixed(checked);
            }
        });
        viewModel.month.observe(getViewLifecycleOwner(), month -> binding.monthLabel.setText(DateTimes.formatMonth(month)));
        viewModel.includeFixed.observe(getViewLifecycleOwner(), include -> {
            if (include == null) {
                return;
            }
            updatingSwitch = true;
            binding.includeFixed.setChecked(include);
            updatingSwitch = false;
        });
        viewModel.report.observe(getViewLifecycleOwner(), this::show);
        viewModel.attach();
    }

    private void show(StatisticsReport report) {
        if (report == null) {
            return;
        }
        binding.incomeTotal.setText(Money.won(report.totalIncome));
        binding.expenseTotal.setText(Money.won(report.totalExpense));
        binding.balanceTotal.setText(Money.signedWon(report.balance));
        binding.balanceTotal.setTextColor(requireContext().getColor(
                report.balance > 0 ? R.color.income : report.balance < 0 ? R.color.expense : R.color.neutral));
        binding.incomeChange.setText("수입 전월 대비 " + report.incomeChangeText);
        binding.expenseChange.setText("지출 전월 대비 " + report.expenseChangeText);
        binding.comparisonRange.setText("비교 구간 " + report.comparisonRangeText);
        binding.averageText.setText("하루 평균 지출 " + report.dailyAverageText);
        binding.peakDay.setText("지출이 가장 많았던 날 " + report.peakDayText);
        binding.peakCategory.setText("지출이 가장 많은 카테고리 " + report.peakCategoryText);
        binding.expensePattern.setText("전월 대비 지출 변화 " + report.expenseChangeText);
        boolean hasBar = report.totalIncome > 0 || report.totalExpense > 0;
        binding.barChart.setVisibility(hasBar ? View.VISIBLE : View.GONE);
        binding.barEmpty.setVisibility(hasBar ? View.GONE : View.VISIBLE);
        if (hasBar) {
            ChartBinder.bindBars(binding.barChart, report.incomeByDay, report.expenseByDay);
        }
        boolean hasPie = !report.chartSlices.isEmpty();
        binding.pieChart.setVisibility(hasPie ? View.VISIBLE : View.GONE);
        binding.pieEmpty.setVisibility(hasPie ? View.GONE : View.VISIBLE);
        if (hasPie) {
            ChartBinder.bindDonut(binding.pieChart, report.chartSlices);
        }
        adapter.submit(report.detailRows);
        binding.getRoot().setContentDescription("총수입 " + Money.won(report.totalIncome)
                + ", 총지출 " + Money.won(report.totalExpense)
                + ", 차액 " + Money.signedWon(report.balance));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private final class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.Holder> {
        private final List<NamedAmount> rows = new ArrayList<>();

        void submit(List<NamedAmount> next) {
            rows.clear();
            if (next != null) {
                rows.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemCategoryStatBinding.inflate(getLayoutInflater(), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            NamedAmount row = rows.get(position);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(ContextCompat.getColor(requireContext(), ChartBinderColors.colorAt(position)));
            holder.binding.dot.setBackground(dot);
            holder.binding.name.setText(row.name);
            holder.binding.amount.setText(Money.won(row.amount));
            holder.binding.percent.setText(row.percentText);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemCategoryStatBinding binding;
            Holder(ItemCategoryStatBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
