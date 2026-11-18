package com.sunwoo.easyledger.ui.input;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.WidgetUpdater;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.common.Event;
import com.sunwoo.easyledger.common.IconCatalog;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.databinding.FragmentInputBinding;
import com.sunwoo.easyledger.databinding.ItemInputCategoryBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InputFragment extends Fragment {
    private FragmentInputBinding binding;
    private InputViewModel viewModel;
    private CategoryAdapter categoryAdapter;
    private GridLayoutManager categoryLayoutManager;
    private boolean updatingMemo;
    private final MaterialButton[] quickButtons = new MaterialButton[3];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInputBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(InputViewModel.class);
        String transactionId = getArguments() == null ? null : getArguments().getString("transactionId");
        viewModel.start(transactionId);
        String prefillDate = getArguments() == null ? null : getArguments().getString("prefillDate");
        if (prefillDate != null && !prefillDate.isEmpty() && !viewModel.isEditing()) {
            viewModel.setDate(LocalDate.parse(prefillDate));
        }
        categoryAdapter = new CategoryAdapter();
        categoryLayoutManager = new GridLayoutManager(requireContext(), 4);
        categoryLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return categoryAdapter.isCollapsedSelection() ? 4 : 1;
            }
        });
        binding.categoryList.setLayoutManager(categoryLayoutManager);
        binding.categoryList.setAdapter(categoryAdapter);
        quickButtons[0] = binding.quickAmountButton1;
        quickButtons[1] = binding.quickAmountButton2;
        quickButtons[2] = binding.quickAmountButton3;
        bindKeys();
        binding.typeExpense.setOnClickListener(v -> viewModel.setType(TransactionType.EXPENSE.name()));
        binding.typeIncome.setOnClickListener(v -> viewModel.setType(TransactionType.INCOME.name()));
        binding.payCash.setOnClickListener(v -> viewModel.setPayment(PaymentMethod.CASH.name()));
        binding.payCard.setOnClickListener(v -> viewModel.setPayment(PaymentMethod.CARD.name()));
        binding.dateButton.setOnClickListener(v -> pickDate());
        binding.prevDay.setOnClickListener(v -> shiftDate(-1));
        binding.nextDay.setOnClickListener(v -> shiftDate(1));
        binding.yesterdayButton.setOnClickListener(v -> viewModel.setDate(LocalDate.now().minusDays(1)));
        binding.todayButton.setOnClickListener(v -> viewModel.setDate(LocalDate.now()));
        binding.saveButton.setOnClickListener(v -> {
            hideKeyboard();
            viewModel.save();
        });
        binding.memoInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (!updatingMemo) {
                    viewModel.setMemo(s.toString());
                }
            }
        });
        viewModel.type.observe(getViewLifecycleOwner(), this::showType);
        viewModel.digits.observe(getViewLifecycleOwner(), this::showAmount);
        viewModel.date.observe(getViewLifecycleOwner(), value -> {
            binding.dateButton.setText(DateTimes.formatDay(value));
            LocalDate today = LocalDate.now();
            binding.todayButton.setEnabled(value == null || !today.equals(value));
            binding.yesterdayButton.setEnabled(value == null || !today.minusDays(1).equals(value));
        });
        viewModel.payment.observe(getViewLifecycleOwner(), this::showPayment);
        viewModel.memo.observe(getViewLifecycleOwner(), this::showMemo);
        viewModel.saving.observe(getViewLifecycleOwner(), saving -> binding.saveButton.setEnabled(!Boolean.TRUE.equals(saving)));
        viewModel.message.observe(getViewLifecycleOwner(), this::showMessage);
        viewModel.saved.observe(getViewLifecycleOwner(), this::onSaved);
        viewModel.categories.observe(getViewLifecycleOwner(), rows ->
                categoryAdapter.submit(rows, viewModel.categoryId.getValue(),
                        Boolean.TRUE.equals(viewModel.categoryExpanded.getValue())));
        viewModel.categoryId.observe(getViewLifecycleOwner(), id -> {
            boolean selected = id != null && !id.isEmpty();
            binding.categoryPrompt.setVisibility(selected ? View.GONE : View.VISIBLE);
            categoryAdapter.submit(viewModel.categories.getValue(), id,
                    Boolean.TRUE.equals(viewModel.categoryExpanded.getValue()));
        });
        viewModel.categoryExpanded.observe(getViewLifecycleOwner(), expanded -> {
            boolean collapsed = !Boolean.TRUE.equals(expanded)
                    && viewModel.categoryId.getValue() != null
                    && !viewModel.categoryId.getValue().isEmpty();
            updateCategoryListLayout(collapsed);
            categoryAdapter.submit(viewModel.categories.getValue(), viewModel.categoryId.getValue(),
                    Boolean.TRUE.equals(expanded));
        });
        viewModel.detailsVisible.observe(getViewLifecycleOwner(), visible -> {
            boolean show = Boolean.TRUE.equals(visible);
            binding.detailsPanel.setVisibility(show ? View.VISIBLE : View.GONE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.detailsPanel.getLayoutParams();
            params.height = 0;
            params.weight = show ? 1f : 0f;
            binding.detailsPanel.setLayoutParams(params);
        });
        viewModel.quickAmounts.observe(getViewLifecycleOwner(), this::showQuickAmounts);
    }

    private void updateCategoryListLayout(boolean collapsed) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.categoryList.getLayoutParams();
        if (collapsed) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.weight = 0f;
        } else {
            params.height = 0;
            params.weight = 1f;
        }
        binding.categoryList.setLayoutParams(params);
    }

    private void bindKeys() {
        wire(binding.key0, "0");
        wire(binding.key1, "1");
        wire(binding.key2, "2");
        wire(binding.key3, "3");
        wire(binding.key4, "4");
        wire(binding.key5, "5");
        wire(binding.key6, "6");
        wire(binding.key7, "7");
        wire(binding.key8, "8");
        wire(binding.key9, "9");
        wire(binding.key00, "00");
        wire(binding.key000, "000");
        binding.keyDel.setOnClickListener(v -> viewModel.backspace());
    }

    private void wire(MaterialButton button, String digits) {
        button.setOnClickListener(v -> viewModel.append(digits));
    }

    private void showType(String value) {
        int id = TransactionType.INCOME.name().equals(value) ? R.id.type_income : R.id.type_expense;
        if (binding.typeToggle.getCheckedButtonId() != id) {
            binding.typeToggle.check(id);
        }
        boolean income = TransactionType.INCOME.name().equals(value);
        binding.amountLabel.setText(income ? "수입 금액" : "지출 금액");
        binding.amountLabel.setTextColor(requireContext().getColor(income ? R.color.income_deep : R.color.expense_deep));
        binding.amountText.setTextColor(requireContext().getColor(income ? R.color.income_deep : R.color.expense_deep));
        binding.amountPanel.setBackgroundResource(income ? R.drawable.bg_amount_income : R.drawable.bg_amount_expense);
        styleTypeToggle(binding.typeExpense, !income, false);
        styleTypeToggle(binding.typeIncome, income, true);
    }

    private void showAmount(String value) {
        long amount = Money.toAmount(value);
        binding.amountText.setText(Money.format(amount));
        binding.amountText.setContentDescription("금액 " + Money.won(amount));
    }

    private void showQuickAmounts(List<Long> amounts) {
        List<Long> values = amounts == null ? new ArrayList<>() : amounts;
        boolean hasCategory = viewModel.categoryId.getValue() != null
                && !viewModel.categoryId.getValue().isEmpty();
        binding.quickAmountRow.setVisibility(!values.isEmpty() ? View.VISIBLE : View.GONE);
        binding.quickAmountHint.setVisibility(hasCategory && values.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < quickButtons.length; i++) {
            MaterialButton button = quickButtons[i];
            if (i < values.size()) {
                long amount = values.get(i);
                button.setVisibility(View.VISIBLE);
                button.setText(Money.format(amount));
                button.setOnClickListener(v -> viewModel.setAmount(amount));
            } else {
                button.setVisibility(View.GONE);
                button.setOnClickListener(null);
            }
        }
    }

    private void showPayment(String value) {
        boolean card = PaymentMethod.CARD.name().equals(value);
        int id = card ? R.id.pay_card : R.id.pay_cash;
        if (binding.paymentToggle.getCheckedButtonId() != id) {
            binding.paymentToggle.check(id);
        }
        stylePayment(binding.payCash, !card);
        stylePayment(binding.payCard, card);
    }

    private void styleTypeToggle(MaterialButton button, boolean selected, boolean income) {
        int fill = income ? R.color.income : R.color.expense;
        int soft = income ? R.color.income_soft : R.color.expense_soft;
        button.setBackgroundTintList(ColorStateList.valueOf(
                requireContext().getColor(selected ? fill : soft)));
        button.setTextColor(requireContext().getColor(selected ? R.color.on_brand : (income ? R.color.income_deep : R.color.expense_deep)));
        button.setStrokeWidth(0);
    }

    private void stylePayment(MaterialButton button, boolean selected) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                requireContext().getColor(selected ? R.color.brand : R.color.brand_soft)));
        button.setTextColor(requireContext().getColor(selected ? R.color.on_brand : R.color.brand_deep));
        button.setStrokeWidth(0);
    }

    private void showMemo(String value) {
        String current = binding.memoInput.getText() == null ? "" : binding.memoInput.getText().toString();
        if (!current.equals(value == null ? "" : value)) {
            updatingMemo = true;
            binding.memoInput.setText(value);
            updatingMemo = false;
        }
    }

    private void pickDate() {
        LocalDate current = viewModel.date.getValue() == null ? LocalDate.now() : viewModel.date.getValue();
        new DatePickerDialog(requireContext(), (picker, year, month, day) ->
                viewModel.setDate(LocalDate.of(year, month + 1, day)),
                current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }

    private void shiftDate(int days) {
        LocalDate current = viewModel.date.getValue() == null ? LocalDate.now() : viewModel.date.getValue();
        viewModel.setDate(current.plusDays(days));
    }

    private void showMessage(Event<String> event) {
        if (event == null) {
            return;
        }
        String text = event.getIfNotHandled();
        if (text != null) {
            Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    private void onSaved(Event<Boolean> event) {
        if (event == null) {
            return;
        }
        Boolean editing = event.getIfNotHandled();
        if (editing == null) {
            return;
        }
        hideKeyboard();
        Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_SHORT).show();
        WidgetUpdater.updateAll(requireContext().getApplicationContext());
        boolean returnToCalendar = getArguments() != null
                && getArguments().getBoolean("returnToCalendar", false);
        if (editing) {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).popBackStack();
            return;
        }
        if (returnToCalendar) {
            LocalDate date = viewModel.date.getValue() == null ? LocalDate.now() : viewModel.date.getValue();
            if (requireActivity() instanceof com.sunwoo.easyledger.ui.MainActivity) {
                ((com.sunwoo.easyledger.ui.MainActivity) requireActivity())
                        .prepareCalendarDate(date.toString());
            }
            Bundle args = new Bundle();
            args.putString("selectedDate", date.toString());
            androidx.navigation.NavController nav =
                    androidx.navigation.fragment.NavHostFragment.findNavController(this);
            int startId = androidx.navigation.NavGraph.findStartDestination(nav.getGraph()).getId();
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(startId, false, true)
                    .build();
            nav.navigate(R.id.calendarFragment, args, options);
            return;
        }
        viewModel.onCreated();
    }

    private void hideKeyboard() {
        if (binding == null) {
            return;
        }
        View focus = requireActivity().getCurrentFocus();
        if (focus == null) {
            focus = binding.memoInput;
        }
        focus.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {
        private final List<CategoryEntity> visibleRows = new ArrayList<>();
        private String selectedId;
        private boolean expanded = true;

        void submit(List<CategoryEntity> next, String selected, boolean expand) {
            visibleRows.clear();
            if (next != null) {
                visibleRows.addAll(next);
            }
            selectedId = selected;
            expanded = expand;
            notifyDataSetChanged();
        }

        boolean isCollapsedSelection() {
            return !expanded && visibleRows.size() == 1;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemInputCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CategoryEntity item = visibleRows.get(position);
            boolean selected = item.id.equals(selectedId);
            boolean collapsed = isCollapsedSelection();
            IconCatalog.apply(holder.binding.icon, item.icon);
            holder.binding.name.setText(item.name);
            LinearLayout content = (LinearLayout) holder.binding.getRoot().getChildAt(0);
            if (collapsed) {
                content.setOrientation(LinearLayout.HORIZONTAL);
                content.setGravity(android.view.Gravity.CENTER_VERTICAL);
                content.setPadding(dp(12), dp(8), dp(12), dp(8));
                holder.binding.icon.getLayoutParams().width = dp(22);
                holder.binding.icon.getLayoutParams().height = dp(22);
                holder.binding.name.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                holder.binding.name.setLayoutParams(new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                ((LinearLayout.LayoutParams) holder.binding.name.getLayoutParams()).setMarginStart(dp(10));
                holder.binding.hint.setVisibility(View.VISIBLE);
                holder.binding.hint.setText(R.string.change_category_hint);
                holder.binding.hint.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                holder.binding.hint.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                content.setOrientation(LinearLayout.VERTICAL);
                content.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                content.setPadding(0, dp(10), 0, dp(8));
                holder.binding.icon.getLayoutParams().width = dp(28);
                holder.binding.icon.getLayoutParams().height = dp(28);
                holder.binding.name.setGravity(android.view.Gravity.CENTER);
                holder.binding.name.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ((LinearLayout.LayoutParams) holder.binding.name.getLayoutParams()).topMargin = dp(6);
                holder.binding.hint.setVisibility(View.GONE);
            }
            MaterialCardView card = (MaterialCardView) holder.binding.getRoot();
            card.setChecked(selected);
            int stroke = ContextCompat.getColor(requireContext(), selected ? R.color.brand : R.color.line);
            card.setStrokeColor(stroke);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                    selected ? R.color.brand_soft : R.color.card));
            card.setOnClickListener(v -> viewModel.onCategoryClicked(item));
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }

        @Override
        public int getItemCount() {
            return visibleRows.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemInputCategoryBinding binding;

            Holder(ItemInputCategoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
