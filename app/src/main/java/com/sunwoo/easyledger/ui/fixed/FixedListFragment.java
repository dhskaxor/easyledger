package com.sunwoo.easyledger.ui.fixed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.FixedTransactionEntity;
import com.sunwoo.easyledger.data.repository.FixedTransactionRepository;
import com.sunwoo.easyledger.databinding.FragmentFixedListBinding;
import com.sunwoo.easyledger.databinding.ItemFixedBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.RecurrenceType;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.sunwoo.easyledger.domain.recurrence.FixedRule;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class FixedListFragment extends Fragment {
    private FragmentFixedListBinding binding;
    private final List<FixedTransactionEntity> items = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFixedListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(new Adapter());
        binding.addButton.setOnClickListener(v -> open(null));
        ((EasyLedgerApp) requireActivity().getApplication()).fixedTransactions().observeAll()
                .observe(getViewLifecycleOwner(), value -> {
                    items.clear();
                    if (value != null) {
                        items.addAll(value);
                    }
                    binding.emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.list.getAdapter().notifyDataSetChanged();
                });
    }

    private void open(String id) {
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        NavHostFragment.findNavController(this).navigate(R.id.fixedEditFragment, bundle);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemFixedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            FixedTransactionEntity item = items.get(position);
            holder.binding.name.setText(item.name);
            String type = TransactionType.INCOME.name().equals(item.type) ? "수입" : "지출";
            holder.binding.cycle.setText(type + " · " + RecurrenceType.fromStorage(item.recurrenceType).label());
            holder.binding.state.setText(item.active ? "사용 중" : "사용 안 함");
            holder.binding.amount.setText((TransactionType.INCOME.name().equals(item.type) ? "+" : "-") + Money.won(item.amount));
            holder.binding.getRoot().setOnClickListener(v -> open(item.id));
            holder.binding.getRoot().setOnLongClickListener(v -> {
                confirmToggle(item);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemFixedBinding binding;
            Holder(ItemFixedBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private void confirmToggle(FixedTransactionEntity item) {
        if (!item.active) {
            toggle(item);
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.stop_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.stop_rule, (dialog, which) -> toggle(item))
                .show();
    }

    private void toggle(FixedTransactionEntity item) {
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                FixedRule rule = FixedTransactionRepository.toRule(item);
                FixedRule changed = new FixedRule(
                        rule.id, rule.type, rule.name, rule.amount, rule.categoryId, rule.paymentMethod,
                        rule.recurrenceType, rule.recurrenceValue, rule.startDate, rule.endDate,
                        !rule.active, rule.effectiveFrom, rule.ruleVersion
                );
                ((EasyLedgerApp) requireActivity().getApplication()).fixedTransactions().update(changed);
            } catch (RuntimeException exception) {
                AppLog.error("Fixed", "toggle", exception);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), UserMessages.from(exception), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
