package com.sunwoo.easyledger.ui.category;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.IconCatalog;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.databinding.FragmentCategoryManageBinding;
import com.sunwoo.easyledger.databinding.ItemCategoryBinding;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CategoryManageFragment extends Fragment {
    private static final int MODE_EXPENSE = 0;
    private static final int MODE_INCOME = 1;

    private FragmentCategoryManageBinding binding;
    private CategoryAdapter adapter;
    private int mode = MODE_EXPENSE;
    private final List<CategoryEntity> categories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryManageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new CategoryAdapter();
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.list.setItemAnimator(null);
        binding.showExpense.setOnClickListener(v -> switchMode(MODE_EXPENSE));
        binding.showIncome.setOnClickListener(v -> switchMode(MODE_INCOME));
        binding.addButton.setOnClickListener(v -> CategoryEditDialog.create(currentType(), null)
                .show(getChildFragmentManager(), "edit"));
        updateModeButtons();
        EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
        app.categories().observeAll().observe(getViewLifecycleOwner(), value -> {
            categories.clear();
            if (value != null) {
                categories.addAll(value);
            }
            adapter.submit(visibleCategories());
        });
    }

    private void switchMode(int next) {
        mode = next;
        updateModeButtons();
        adapter.submit(visibleCategories());
    }

    private void updateModeButtons() {
        styleModeButton(binding.showExpense, mode == MODE_EXPENSE);
        styleModeButton(binding.showIncome, mode == MODE_INCOME);
    }

    private void styleModeButton(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brand_soft)));
            button.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brand)));
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.card)));
            button.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.line)));
        }
    }

    private String currentType() {
        return mode == MODE_INCOME ? TransactionType.INCOME.name() : TransactionType.EXPENSE.name();
    }

    private List<CategoryEntity> visibleCategories() {
        List<CategoryEntity> rows = new ArrayList<>();
        for (CategoryEntity category : categories) {
            if (currentType().equals(category.type) && category.parentId == null) {
                rows.add(category);
                for (CategoryEntity child : categories) {
                    if (category.id.equals(child.parentId)) {
                        rows.add(child);
                    }
                }
            }
        }
        return rows;
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {
        private final List<CategoryEntity> rows = new ArrayList<>();

        CategoryAdapter() {
            setHasStableIds(true);
        }

        void submit(List<CategoryEntity> next) {
            rows.clear();
            if (next != null) {
                rows.addAll(next);
            }
            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return rows.get(position).id.hashCode();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CategoryEntity item = rows.get(position);
            IconCatalog.apply(holder.binding.icon, item.icon);
            holder.binding.name.setText(item.parentId == null ? item.name : "    " + item.name);

            StringBuilder state = new StringBuilder();
            if (item.favorite) {
                state.append(getString(R.string.favorite));
            }
            if (item.hidden) {
                if (state.length() > 0) {
                    state.append(" · ");
                }
                state.append(getString(R.string.hidden));
            } else if (state.length() == 0) {
                state.append(getString(R.string.active));
            }
            holder.binding.state.setText(state.toString());
            holder.binding.state.setTextColor(ContextCompat.getColor(
                    requireContext(), item.hidden ? R.color.expense : R.color.neutral));

            int favoriteColor = ContextCompat.getColor(requireContext(),
                    item.favorite ? R.color.accent : R.color.line);
            holder.binding.favoriteButton.setImageResource(
                    item.favorite ? R.drawable.ic_star : R.drawable.ic_star_outline);
            holder.binding.favoriteButton.setImageTintList(ColorStateList.valueOf(favoriteColor));
            holder.binding.favoriteButton.setContentDescription(
                    item.favorite ? getString(R.string.favorite_on) : getString(R.string.favorite_off));

            int hideColor = ContextCompat.getColor(requireContext(),
                    item.hidden ? R.color.expense : R.color.brand);
            holder.binding.hideButton.setImageResource(
                    item.hidden ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            holder.binding.hideButton.setImageTintList(ColorStateList.valueOf(hideColor));
            holder.binding.hideButton.setContentDescription(
                    item.hidden ? getString(R.string.unhide) : getString(R.string.hide));

            holder.binding.getRoot().setAlpha(item.hidden ? 0.55f : 1f);
            holder.binding.getRoot().setOnClickListener(v ->
                    CategoryEditDialog.create(item.type, item.id).show(getChildFragmentManager(), "edit"));
            holder.binding.upButton.setOnClickListener(v -> move(item.id, true));
            holder.binding.downButton.setOnClickListener(v -> move(item.id, false));
            holder.binding.favoriteButton.setOnClickListener(v -> favorite(item));
            holder.binding.hideButton.setOnClickListener(v -> hide(item));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemCategoryBinding binding;

            Holder(ItemCategoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private void move(String id, boolean up) {
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                ((EasyLedgerApp) requireActivity().getApplication()).categories().move(id, up);
            } catch (RuntimeException exception) {
                toast(UserMessages.from(exception));
            }
        });
    }

    private void favorite(CategoryEntity item) {
        EasyLedgerDatabase.executor().execute(() ->
                ((EasyLedgerApp) requireActivity().getApplication()).categories().setFavorite(item.id, !item.favorite));
    }

    private void hide(CategoryEntity item) {
        EasyLedgerDatabase.executor().execute(() ->
                ((EasyLedgerApp) requireActivity().getApplication()).categories().setHidden(item.id, !item.hidden));
    }

    private void toast(String message) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        binding = null;
        adapter = null;
        super.onDestroyView();
    }
}
