package com.sunwoo.easyledger.ui.calendar;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.data.entity.DayTotal;
import com.sunwoo.easyledger.databinding.ItemCalendarDayBinding;
import com.sunwoo.easyledger.domain.calculator.Money;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.Holder> {
    public interface Listener {
        void onDay(LocalDate date);
    }

    private final Listener listener;
    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private final Map<LocalDate, DayTotal> totals = new HashMap<>();

    public CalendarDayAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setMonth(YearMonth month, LocalDate selected, List<DayTotal> rows) {
        this.month = month;
        this.selected = selected;
        totals.clear();
        if (rows != null) {
            for (DayTotal row : rows) {
                totals.put(LocalDate.parse(row.date), row);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LocalDate first = month.atDay(1);
        int lead = first.getDayOfWeek().getValue() % 7;
        LocalDate date = first.minusDays(lead).plusDays(position);
        holder.bind(date);
    }

    @Override
    public int getItemCount() {
        return 42;
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding binding;

        Holder(ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(LocalDate date) {
            boolean inMonth = YearMonth.from(date).equals(month);
            binding.dayNumber.setText(String.valueOf(date.getDayOfMonth()));
            binding.dayNumber.setAlpha(inMonth ? 1f : 0.35f);
            int dow = date.getDayOfWeek().getValue() % 7;
            int color = dow == 0 ? R.color.expense : dow == 6 ? R.color.brand : R.color.neutral;
            binding.dayNumber.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), color));
            binding.dayNumber.setTypeface(null, date.equals(LocalDate.now()) ? Typeface.BOLD : Typeface.NORMAL);
            DayTotal total = totals.get(date);
            if (inMonth && total != null && total.income > 0) {
                binding.dayIncome.setText("+" + compact(total.income));
            } else {
                binding.dayIncome.setText("");
            }
            if (inMonth && total != null && total.expense > 0) {
                binding.dayExpense.setText("-" + compact(total.expense));
            } else {
                binding.dayExpense.setText("");
            }
            if (date.equals(selected) && inMonth) {
                binding.getRoot().setBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.calendar_selected));
            } else if (date.equals(LocalDate.now()) && inMonth) {
                binding.getRoot().setBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.calendar_today));
            } else {
                binding.getRoot().setBackgroundColor(0);
            }
            binding.getRoot().setOnClickListener(inMonth ? v -> listener.onDay(date) : null);
            binding.getRoot().setContentDescription(date.getMonthValue() + "월 " + date.getDayOfMonth() + "일");
        }
    }

    private static String compact(long amount) {
        if (amount >= 10000) {
            return (amount / 10000) + "만";
        }
        return Money.format(amount);
    }
}
