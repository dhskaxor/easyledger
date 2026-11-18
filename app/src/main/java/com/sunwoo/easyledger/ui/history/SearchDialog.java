package com.sunwoo.easyledger.ui.history;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.common.SheetDialogs;
import com.sunwoo.easyledger.data.SavedSearchFilters;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.databinding.DialogSearchBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchDialog extends DialogFragment {
    public static final String REQUEST = "history-search";
    private DialogSearchBinding binding;
    private final Set<String> selected = new HashSet<>();
    private final List<CategoryEntity> categories = new ArrayList<>();
    private List<SavedSearchFilters.Filter> savedFilters = new ArrayList<>();
    private LocalDate start;
    private LocalDate end;
    private String payment;

    public static SearchDialog create(HistoryViewModel model) {
        SearchDialog dialog = new SearchDialog();
        Bundle args = new Bundle();
        args.putString("start", model.start().toString());
        args.putString("end", model.end().toString());
        args.putString("memo", model.memo());
        args.putString("payment", model.payment());
        if (model.minAmount() != null) {
            args.putLong("min", model.minAmount());
        }
        if (model.maxAmount() != null) {
            args.putLong("max", model.maxAmount());
        }
        args.putStringArrayList("categories", new ArrayList<>(model.categoryIds()));
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getTheme() {
        return SheetDialogs.theme();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        SheetDialogs.apply(this, SheetDialogs.HEIGHT_DEFAULT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.header.sheetTitle.setText(R.string.search);
        binding.header.sheetClose.setOnClickListener(v -> dismiss());
        Bundle args = requireArguments();
        start = LocalDate.parse(args.getString("start"));
        end = LocalDate.parse(args.getString("end"));
        payment = args.getString("payment");
        binding.memoInput.setText(args.getString("memo"));
        if (args.containsKey("min")) {
            binding.minInput.setText(String.valueOf(args.getLong("min")));
        }
        if (args.containsKey("max")) {
            binding.maxInput.setText(String.valueOf(args.getLong("max")));
        }
        ArrayList<String> ids = args.getStringArrayList("categories");
        if (ids != null) {
            selected.addAll(ids);
        }
        refreshLabels();
        binding.startButton.setOnClickListener(v -> pick(true));
        binding.endButton.setOnClickListener(v -> pick(false));
        binding.paymentButton.setOnClickListener(v -> cyclePayment());
        binding.categoryButton.setOnClickListener(v -> pickCategories());
        binding.saveFilterButton.setOnClickListener(v -> promptSaveFilter());
        binding.savedFiltersButton.setOnClickListener(v -> showSavedFilters());
        binding.applyButton.setOnClickListener(v -> apply());
        binding.clearButton.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putBoolean("clear", true);
            getParentFragmentManager().setFragmentResult(REQUEST, result);
            dismiss();
        });
        EasyLedgerDatabase.executor().execute(() -> {
            EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
            List<CategoryEntity> all = app.categories().all();
            List<SavedSearchFilters.Filter> filters = SavedSearchFilters.load(app.settings());
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    categories.clear();
                    categories.addAll(all);
                    savedFilters = filters;
                    refreshSavedLabel();
                });
            }
        });
    }

    private void refreshLabels() {
        binding.startButton.setText(DateTimes.formatDay(start));
        binding.endButton.setText(DateTimes.formatDay(end));
        if (payment == null) {
            binding.paymentButton.setText(R.string.payment_all);
        } else if (PaymentMethod.CARD.name().equals(payment)) {
            binding.paymentButton.setText(getString(R.string.payment) + " · " + getString(R.string.card));
        } else {
            binding.paymentButton.setText(getString(R.string.payment) + " · " + getString(R.string.cash));
        }
        binding.categoryButton.setText(selected.isEmpty()
                ? getString(R.string.category_all)
                : getString(R.string.category) + " " + selected.size() + "개");
        refreshSavedLabel();
    }

    private void refreshSavedLabel() {
        if (binding == null) {
            return;
        }
        binding.savedFiltersButton.setText(savedFilters.isEmpty()
                ? getString(R.string.saved_filters_empty)
                : getString(R.string.saved_filters_count, savedFilters.size()));
    }

    private void pick(boolean startDate) {
        LocalDate current = startDate ? start : end;
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            LocalDate chosen = LocalDate.of(year, month + 1, day);
            if (startDate) {
                start = chosen;
                if (end.isBefore(start)) {
                    end = start;
                }
            } else {
                end = chosen;
                if (start.isAfter(end)) {
                    start = end;
                }
            }
            refreshLabels();
        }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }

    private void cyclePayment() {
        if (payment == null) {
            payment = PaymentMethod.CASH.name();
        } else if (PaymentMethod.CASH.name().equals(payment)) {
            payment = PaymentMethod.CARD.name();
        } else {
            payment = null;
        }
        refreshLabels();
    }

    private void pickCategories() {
        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), R.string.category_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[categories.size()];
        boolean[] checked = new boolean[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            names[i] = categories.get(i).name;
            checked[i] = selected.contains(categories.get(i).id);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.category)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    selected.clear();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            selected.add(categories.get(i).id);
                        }
                    }
                    refreshLabels();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void promptSaveFilter() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.filter_name_hint);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.save_filter)
                .setView(input)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.filter_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Long min = parseAmount(textOf(binding.minInput));
                    Long max = parseAmount(textOf(binding.maxInput));
                    SavedSearchFilters.Filter filter = new SavedSearchFilters.Filter(
                            name,
                            start.toString(),
                            end.toString(),
                            textOf(binding.memoInput),
                            payment,
                            min,
                            max,
                            new ArrayList<>(selected)
                    );
                    EasyLedgerDatabase.executor().execute(() -> {
                        EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
                        SavedSearchFilters.add(app.settings(), filter);
                        List<SavedSearchFilters.Filter> next = SavedSearchFilters.load(app.settings());
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                savedFilters = next;
                                refreshSavedLabel();
                                Toast.makeText(requireContext(), R.string.filter_saved, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showSavedFilters() {
        if (savedFilters.isEmpty()) {
            Toast.makeText(requireContext(), R.string.saved_filters_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[savedFilters.size()];
        for (int i = 0; i < savedFilters.size(); i++) {
            names[i] = savedFilters.get(i).name;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.saved_filters)
                .setItems(names, (dialog, which) -> applySaved(savedFilters.get(which)))
                .setNeutralButton(R.string.delete_saved_filter, (dialog, which) -> confirmDeleteSaved())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeleteSaved() {
        if (savedFilters.isEmpty()) {
            return;
        }
        String[] names = new String[savedFilters.size()];
        for (int i = 0; i < savedFilters.size(); i++) {
            names[i] = savedFilters.get(i).name;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_saved_filter)
                .setItems(names, (dialog, which) -> {
                    String name = savedFilters.get(which).name;
                    EasyLedgerDatabase.executor().execute(() -> {
                        EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
                        SavedSearchFilters.remove(app.settings(), name);
                        List<SavedSearchFilters.Filter> next = SavedSearchFilters.load(app.settings());
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                savedFilters = next;
                                refreshSavedLabel();
                            });
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applySaved(SavedSearchFilters.Filter filter) {
        start = LocalDate.parse(filter.start);
        end = LocalDate.parse(filter.end);
        payment = filter.payment;
        binding.memoInput.setText(filter.memo);
        binding.minInput.setText(filter.minAmount == null ? "" : String.valueOf(filter.minAmount));
        binding.maxInput.setText(filter.maxAmount == null ? "" : String.valueOf(filter.maxAmount));
        selected.clear();
        selected.addAll(filter.categoryIds);
        refreshLabels();
        apply();
    }

    private void apply() {
        Long min = parseAmount(textOf(binding.minInput));
        Long max = parseAmount(textOf(binding.maxInput));
        if (min != null && max != null && min > max) {
            Toast.makeText(requireContext(), R.string.amount_range_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle result = new Bundle();
        result.putBoolean("clear", false);
        result.putString("start", start.toString());
        result.putString("end", end.toString());
        result.putString("memo", textOf(binding.memoInput));
        result.putString("payment", payment);
        if (min != null) {
            result.putLong("min", min);
        }
        if (max != null) {
            result.putLong("max", max);
        }
        result.putStringArrayList("categories", new ArrayList<>(selected));
        getParentFragmentManager().setFragmentResult(REQUEST, result);
        dismiss();
    }

    private static String textOf(android.widget.TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }

    private static Long parseAmount(String raw) {
        String digits = raw.replace(",", "").trim();
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Money.parsePlain(digits);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
