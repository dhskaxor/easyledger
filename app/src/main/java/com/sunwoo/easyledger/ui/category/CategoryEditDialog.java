package com.sunwoo.easyledger.ui.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.IconCatalog;
import com.sunwoo.easyledger.common.SheetDialogs;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.databinding.DialogCategoryEditBinding;
import com.sunwoo.easyledger.databinding.ItemIconBinding;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CategoryEditDialog extends androidx.fragment.app.DialogFragment {
    private DialogCategoryEditBinding binding;
    private String icon = "generic";
    private String parentId;
    private String type = TransactionType.EXPENSE.name();
    private String editingId;
    private final List<CategoryEntity> roots = new ArrayList<>();

    public static CategoryEditDialog create(String type, String id) {
        CategoryEditDialog dialog = new CategoryEditDialog();
        Bundle args = new Bundle();
        args.putString("type", type);
        args.putString("id", id);
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
        binding = DialogCategoryEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        SheetDialogs.apply(this, SheetDialogs.HEIGHT_TALL);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        type = requireArguments().getString("type", TransactionType.EXPENSE.name());
        editingId = requireArguments().getString("id");
        boolean editing = editingId != null && !editingId.isEmpty();
        binding.title.setText(editing ? R.string.edit_category : R.string.add_category);
        binding.closeButton.setOnClickListener(v -> dismiss());
        binding.iconGrid.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        binding.iconGrid.setAdapter(new IconAdapter());
        binding.parentButton.setOnClickListener(v -> chooseParent());
        binding.saveButton.setOnClickListener(v -> save());
        bindFocusScroll(binding.nameInput);
        bindFocusScroll(binding.quickAmount1);
        bindFocusScroll(binding.quickAmount2);
        bindFocusScroll(binding.quickAmount3);
        EasyLedgerDatabase.executor().execute(() -> {
            EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
            List<CategoryEntity> all = app.categories().all();
            CategoryEntity entity = editingId == null ? null : app.categories().get(editingId);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> fill(all, entity));
        });
    }

    private void bindFocusScroll(TextInputEditText field) {
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus || binding == null) {
                return;
            }
            NestedScrollView scroll = binding.scroll;
            scroll.post(() -> {
                int[] fieldLoc = new int[2];
                int[] scrollLoc = new int[2];
                v.getLocationOnScreen(fieldLoc);
                scroll.getLocationOnScreen(scrollLoc);
                int relativeTop = fieldLoc[1] - scrollLoc[1];
                int target = scroll.getScrollY() + relativeTop - scroll.getHeight() / 3;
                scroll.smoothScrollTo(0, Math.max(0, target));
            });
        });
    }

    private void fill(List<CategoryEntity> all, CategoryEntity editing) {
        roots.clear();
        for (CategoryEntity category : all) {
            if (category.parentId == null && category.type.equals(type) && !category.id.equals(editingId)) {
                roots.add(category);
            }
        }
        if (editing != null) {
            type = editing.type;
            binding.nameInput.setText(editing.name);
            icon = editing.icon;
            parentId = editing.parentId;
            binding.favoriteSwitch.setChecked(editing.favorite);
            binding.hiddenSwitch.setChecked(editing.hidden);
            setQuickField(binding.quickAmount1, editing.quickAmount1);
            setQuickField(binding.quickAmount2, editing.quickAmount2);
            setQuickField(binding.quickAmount3, editing.quickAmount3);
            binding.parentButton.setEnabled(false);
            binding.title.setText(R.string.edit_category);
        }
        binding.parentButton.setText(parentLabel());
        if (binding.iconGrid.getAdapter() != null) {
            binding.iconGrid.getAdapter().notifyDataSetChanged();
        }
    }

    private void setQuickField(TextInputEditText field, long amount) {
        field.setText(amount > 0 ? Long.toString(amount) : "");
    }

    private String parentLabel() {
        for (CategoryEntity root : roots) {
            if (root.id.equals(parentId)) {
                return getString(R.string.parent_category_of, root.name);
            }
        }
        return getString(R.string.no_parent_category);
    }

    private void chooseParent() {
        String[] names = new String[roots.size() + 1];
        names[0] = getString(R.string.none);
        for (int i = 0; i < roots.size(); i++) {
            names[i + 1] = roots.get(i).name;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.parent_category)
                .setItems(names, (dialog, which) -> {
                    parentId = which == 0 ? null : roots.get(which - 1).id;
                    binding.parentButton.setText(parentLabel());
                })
                .show();
    }

    private void save() {
        long[] amounts;
        try {
            amounts = new long[] {
                    parseQuick(binding.quickAmount1),
                    parseQuick(binding.quickAmount2),
                    parseQuick(binding.quickAmount3)
            };
        } catch (RuntimeException exception) {
            Toast.makeText(requireContext(), exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        CategoryEntity draft = new CategoryEntity();
        draft.id = editingId;
        draft.name = binding.nameInput.getText() == null ? "" : binding.nameInput.getText().toString();
        draft.type = type;
        draft.parentId = parentId;
        draft.icon = icon;
        draft.favorite = binding.favoriteSwitch.isChecked();
        draft.hidden = binding.hiddenSwitch.isChecked();
        draft.quickAmount1 = amounts[0];
        draft.quickAmount2 = amounts[1];
        draft.quickAmount3 = amounts[2];
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                ((EasyLedgerApp) requireActivity().getApplication()).categories().save(draft);
                if (isAdded()) {
                    requireActivity().runOnUiThread(this::dismiss);
                }
            } catch (RuntimeException exception) {
                AppLog.error("Category", "save", exception);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), UserMessages.from(exception), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private long parseQuick(TextInputEditText field) {
        String raw = field.getText() == null ? "" : field.getText().toString().trim().replace(",", "");
        if (raw.isEmpty()) {
            return 0L;
        }
        try {
            long amount = Money.parsePlain(raw);
            if (amount < 0) {
                throw new IllegalArgumentException("자주 쓰는 금액은 0보다 커야 합니다.");
            }
            return amount;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("자주 쓰는 금액을 확인해 주세요.");
        }
    }

    private class IconAdapter extends RecyclerView.Adapter<IconAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemIconBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String key = IconCatalog.KEYS[position];
            boolean selected = key.equals(icon);
            IconCatalog.apply(holder.binding.icon, key);
            holder.binding.icon.setSelected(selected);
            holder.binding.icon.setAlpha(selected ? 1f : 0.72f);
            holder.binding.getRoot().setOnClickListener(v -> {
                icon = key;
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return IconCatalog.KEYS.length;
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemIconBinding binding;

            Holder(ItemIconBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
