package com.sunwoo.easyledger.ui.transaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.common.IconCatalog;
import com.sunwoo.easyledger.data.entity.TxListItem;
import com.sunwoo.easyledger.databinding.ItemTransactionBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionListAdapter.Holder> {
    public interface Listener {
        void onClick(TxListItem item);
    }

    private final Listener listener;
    private final List<TxListItem> items = new ArrayList<>();

    public TransactionListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<TxListItem> next) {
        items.clear();
        if (next != null) {
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        Holder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TxListItem item) {
            boolean income = TransactionType.INCOME.name().equals(item.type);
            IconCatalog.apply(binding.icon, item.categoryIcon);
            binding.icon.setBackgroundResource(income ? R.drawable.bg_well_income : R.drawable.bg_well_expense);
            binding.categoryName.setText(item.categoryName);
            if (item.memo == null || item.memo.isEmpty()) {
                binding.memo.setVisibility(View.GONE);
            } else {
                binding.memo.setVisibility(View.VISIBLE);
                binding.memo.setText(item.memo);
            }
            String payment = PaymentMethod.CARD.name().equals(item.paymentMethod) ? "카드" : "현금";
            String fixed = item.fixed ? " · 고정" : "";
            binding.meta.setText(item.transactionDate.substring(5) + " " + DateTimes.formatHourMinute(item.transactionTime)
                    + " · " + payment + fixed);
            binding.typeLabel.setText(income ? "수입" : "지출");
            binding.amount.setText((income ? "+" : "-") + Money.won(item.amount));
            int color = ContextCompat.getColor(binding.getRoot().getContext(), income ? R.color.income : R.color.expense);
            binding.typeLabel.setTextColor(color);
            binding.amount.setTextColor(color);
            binding.getRoot().setOnClickListener(view -> listener.onClick(item));
            binding.getRoot().setContentDescription((income ? "수입 " : "지출 ") + item.categoryName + " " + Money.won(item.amount));
        }
    }
}
