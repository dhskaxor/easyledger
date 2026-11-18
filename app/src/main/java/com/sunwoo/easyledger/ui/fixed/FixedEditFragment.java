package com.sunwoo.easyledger.ui.fixed;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.FixedTransactionEntity;
import com.sunwoo.easyledger.data.repository.FixedTransactionRepository;
import com.sunwoo.easyledger.databinding.FragmentFixedEditBinding;
import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.RecurrenceType;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.sunwoo.easyledger.domain.recurrence.FixedRule;
import com.sunwoo.easyledger.domain.recurrence.RecurrenceCalendar;
import com.sunwoo.easyledger.ui.category.CategoryPickerDialog;

import java.time.LocalDate;
import java.util.UUID;

public class FixedEditFragment extends Fragment {
    private FragmentFixedEditBinding binding;
    private String editingId;
    private String categoryId;
    private String categoryType;
    private RecurrenceType recurrence = RecurrenceType.MONTHLY;
    private LocalDate start = LocalDate.now();
    private LocalDate end;
    private boolean updating;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFixedEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        editingId = getArguments() == null ? null : getArguments().getString("id");
        binding.typeExpense.setOnClickListener(v -> setType(TransactionType.EXPENSE.name()));
        binding.typeIncome.setOnClickListener(v -> setType(TransactionType.INCOME.name()));
        binding.payCash.setOnClickListener(v -> binding.paymentToggle.check(R.id.pay_cash));
        binding.payCard.setOnClickListener(v -> binding.paymentToggle.check(R.id.pay_card));
        binding.everyDay.setOnClickListener(v -> setRecurrence(RecurrenceType.DAILY));
        binding.everyWeek.setOnClickListener(v -> setRecurrence(RecurrenceType.WEEKLY));
        binding.everyMonth.setOnClickListener(v -> setRecurrence(RecurrenceType.MONTHLY));
        binding.everyYear.setOnClickListener(v -> setRecurrence(RecurrenceType.YEARLY));
        binding.startButton.setOnClickListener(v -> pickDate(true));
        binding.endButton.setOnClickListener(v -> pickDate(false));
        binding.clearEnd.setOnClickListener(v -> {
            end = null;
            refreshTexts();
        });
        binding.categoryButton.setOnClickListener(v -> CategoryPickerDialog.newInstance(currentType(), "fixed-category")
                .show(getParentFragmentManager(), "fixed-category"));
        binding.saveButton.setOnClickListener(v -> save());
        binding.paymentToggle.check(R.id.pay_cash);
        binding.typeToggle.check(R.id.type_expense);
        binding.recurrenceToggle.check(R.id.every_month);
        refreshTexts();
        getParentFragmentManager().setFragmentResultListener("fixed-category", getViewLifecycleOwner(), (key, bundle) -> {
            categoryId = bundle.getString("id");
            categoryType = bundle.getString("type");
            binding.categoryButton.setText(bundle.getString("name"));
        });
        if (editingId != null) {
            EasyLedgerDatabase.executor().execute(() -> {
                EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
                FixedTransactionEntity entity = app.fixedTransactions().get(editingId);
                CategoryEntity category = entity == null ? null : app.categories().get(entity.categoryId);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> fill(entity, category));
                }
            });
        }
    }

    private void fill(FixedTransactionEntity entity, CategoryEntity category) {
        if (binding == null || entity == null) {
            return;
        }
        updating = true;
        binding.nameInput.setText(entity.name);
        binding.amountInput.setText(String.valueOf(entity.amount));
        binding.typeToggle.check(TransactionType.INCOME.name().equals(entity.type) ? R.id.type_income : R.id.type_expense);
        binding.paymentToggle.check(PaymentMethod.CARD.name().equals(entity.paymentMethod) ? R.id.pay_card : R.id.pay_cash);
        recurrence = RecurrenceType.fromStorage(entity.recurrenceType);
        binding.recurrenceToggle.check(recurrenceButton());
        start = LocalDate.parse(entity.startDate);
        end = entity.endDate == null || entity.endDate.isEmpty() ? null : LocalDate.parse(entity.endDate);
        categoryId = entity.categoryId;
        categoryType = category == null ? entity.type : category.type;
        binding.categoryButton.setText(category == null ? getString(R.string.pick_category) : category.name);
        binding.activeSwitch.setChecked(entity.active);
        updating = false;
        refreshTexts();
    }

    private void setType(String type) {
        if (categoryType != null && !categoryType.equals(type)) {
            categoryId = null;
            categoryType = null;
            binding.categoryButton.setText(R.string.pick_category);
        }
    }

    private void setRecurrence(RecurrenceType value) {
        recurrence = value;
        refreshTexts();
    }

    private String currentType() {
        return binding.typeToggle.getCheckedButtonId() == R.id.type_income
                ? TransactionType.INCOME.name() : TransactionType.EXPENSE.name();
    }

    private int recurrenceButton() {
        switch (recurrence) {
            case DAILY:
                return R.id.every_day;
            case WEEKLY:
                return R.id.every_week;
            case YEARLY:
                return R.id.every_year;
            default:
                return R.id.every_month;
        }
    }

    private void refreshTexts() {
        if (binding == null) {
            return;
        }
        binding.recurrenceDescription.setText(RecurrenceCalendar.describe(recurrence, start));
        binding.startButton.setText("시작일  " + DateTimes.formatDay(start));
        binding.endButton.setText(end == null ? "종료일 없음" : "종료일  " + DateTimes.formatDay(end));
    }

    private void pickDate(boolean startDate) {
        LocalDate current = startDate || end == null ? start : end;
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            LocalDate chosen = LocalDate.of(year, month + 1, day);
            if (startDate) {
                start = chosen;
            } else {
                end = chosen;
            }
            refreshTexts();
        }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }

    private void save() {
        if (updating) {
            return;
        }
        String digits = binding.amountInput.getText() == null ? "" : binding.amountInput.getText().toString().replaceAll("[^0-9]", "");
        long amount = digits.isEmpty() ? 0 : Long.parseLong(digits);
        String name = binding.nameInput.getText() == null ? "" : binding.nameInput.getText().toString();
        String type = currentType();
        String payment = binding.paymentToggle.getCheckedButtonId() == R.id.pay_card
                ? PaymentMethod.CARD.name() : PaymentMethod.CASH.name();
        FixedRule rule = new FixedRule(
                editingId == null ? UUID.randomUUID().toString() : editingId,
                type,
                name,
                amount,
                categoryId,
                payment,
                recurrence,
                RecurrenceCalendar.valueFor(recurrence, start),
                start,
                end,
                binding.activeSwitch.isChecked(),
                start,
                1
        );
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
                if (editingId == null) {
                    app.fixedTransactions().create(rule);
                } else {
                    app.fixedTransactions().update(rule);
                }
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(this).popBackStack();
                    });
                }
            } catch (RuntimeException exception) {
                AppLog.error("Fixed", "save", exception);
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
