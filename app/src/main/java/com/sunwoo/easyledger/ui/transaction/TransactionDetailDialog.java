package com.sunwoo.easyledger.ui.transaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.fragment.NavHostFragment;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.app.WidgetUpdater;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.common.IconCatalog;
import com.sunwoo.easyledger.common.SheetDialogs;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.TransactionEntity;
import com.sunwoo.easyledger.databinding.DialogTransactionDetailBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;

public class TransactionDetailDialog extends DialogFragment {
    public static void show(androidx.fragment.app.Fragment parent, String id) {
        TransactionDetailDialog dialog = new TransactionDetailDialog();
        Bundle args = new Bundle();
        args.putString("id", id);
        dialog.setArguments(args);
        dialog.show(parent.getChildFragmentManager(), "transaction");
    }

    private DialogTransactionDetailBinding binding;

    @Override
    public int getTheme() {
        return SheetDialogs.theme();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogTransactionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        SheetDialogs.apply(this, SheetDialogs.HEIGHT_COMPACT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.closeButton.setOnClickListener(v -> dismiss());
        String id = requireArguments().getString("id");
        EasyLedgerDatabase.executor().execute(() -> {
            EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
            TransactionEntity entity = app.transactions().get(id);
            CategoryEntity category = entity == null ? null : app.categories().get(entity.categoryId);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> bind(entity, category));
        });
    }

    private void bind(TransactionEntity entity, CategoryEntity category) {
        if (binding == null || entity == null) {
            dismiss();
            return;
        }
        boolean income = TransactionType.INCOME.name().equals(entity.type);
        binding.typeText.setText(income ? R.string.income : R.string.expense);
        binding.typeText.setTextColor(requireContext().getColor(income ? R.color.income_deep : R.color.expense_deep));
        binding.typeText.setBackgroundResource(income ? R.drawable.bg_type_chip_income : R.drawable.bg_type_chip);
        binding.summaryPanel.setBackgroundResource(income ? R.drawable.bg_amount_income : R.drawable.bg_amount_expense);
        binding.amountText.setText((income ? "+" : "-") + Money.won(entity.amount));
        binding.amountText.setTextColor(requireContext().getColor(income ? R.color.income : R.color.expense));
        binding.categoryText.setText(category == null ? getString(R.string.category) : category.name);
        if (category != null) {
            IconCatalog.apply(binding.icon, category.icon);
        } else {
            IconCatalog.apply(binding.icon, "generic");
        }
        try {
            binding.dateText.setText(DateTimes.formatDay(LocalDate.parse(entity.transactionDate)));
        } catch (RuntimeException ignored) {
            binding.dateText.setText(entity.transactionDate);
        }
        binding.timeText.setText(DateTimes.formatHourMinute(entity.transactionTime));
        binding.paymentText.setText(PaymentMethod.CARD.name().equals(entity.paymentMethod)
                ? getString(R.string.card) : getString(R.string.cash));
        if (entity.memo == null || entity.memo.trim().isEmpty()) {
            binding.memoText.setText(R.string.memo_empty);
            binding.memoText.setTextColor(requireContext().getColor(R.color.neutral));
        } else {
            binding.memoText.setText(entity.memo);
            binding.memoText.setTextColor(requireContext().getColor(R.color.ink));
        }
        if (entity.fixed) {
            binding.fixedText.setVisibility(View.VISIBLE);
            binding.fixedText.setText(R.string.fixed_origin);
        } else {
            binding.fixedText.setVisibility(View.GONE);
        }
        binding.editButton.setOnClickListener(v -> edit(entity.id));
        binding.copyButton.setOnClickListener(v -> copy(entity.id));
        binding.deleteButton.setOnClickListener(v -> confirmDelete(entity.id));
    }

    private void edit(String id) {
        Bundle bundle = new Bundle();
        bundle.putString("transactionId", id);
        dismiss();
        if (getParentFragment() != null) {
            NavHostFragment.findNavController(getParentFragment()).navigate(R.id.transactionEditFragment, bundle);
        }
    }

    private void copy(String id) {
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                ((EasyLedgerApp) requireActivity().getApplication()).transactions().copy(id);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (RuntimeException exception) {
                AppLog.error("Detail", "copy", exception);
                showError(UserMessages.from(exception));
            }
        });
    }

    private void confirmDelete(String id) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> delete(id))
                .show();
    }

    private void delete(String id) {
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                ((EasyLedgerApp) requireActivity().getApplication()).transactions().delete(id);
                WidgetUpdater.updateAll(requireActivity().getApplicationContext());
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.deleted, Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (RuntimeException exception) {
                AppLog.error("Detail", "delete", exception);
                showError(UserMessages.GENERIC);
            }
        });
    }

    private void showError(String message) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
