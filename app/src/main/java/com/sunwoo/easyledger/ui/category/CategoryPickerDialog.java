package com.sunwoo.easyledger.ui.category;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.IconCatalog;
import com.sunwoo.easyledger.common.SheetDialogs;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.databinding.DialogCategoryPickerBinding;
import com.sunwoo.easyledger.databinding.ItemLibraryBinding;

import java.util.ArrayList;
import java.util.List;

public class CategoryPickerDialog extends DialogFragment {
    public static final String REQUEST = "category-picked";

    public static CategoryPickerDialog newInstance(String type) {
        return newInstance(type, REQUEST);
    }

    public static CategoryPickerDialog newInstance(String type, String request) {
        CategoryPickerDialog dialog = new CategoryPickerDialog();
        Bundle args = new Bundle();
        args.putString("type", type);
        args.putString("request", request);
        dialog.setArguments(args);
        return dialog;
    }

    private DialogCategoryPickerBinding binding;
    private final List<CategoryEntity> items = new ArrayList<>();

    @Override
    public int getTheme() {
        return SheetDialogs.theme();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogCategoryPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        SheetDialogs.apply(this, SheetDialogs.HEIGHT_DEFAULT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.header.sheetTitle.setText(R.string.pick_category_title);
        binding.header.sheetClose.setOnClickListener(v -> dismiss());
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(new Adapter());
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { load(s.toString()); }
        });
        load("");
    }

    private void load(String keyword) {
        String type = getArguments() == null ? "EXPENSE" : getArguments().getString("type");
        EasyLedgerDatabase.executor().execute(() -> {
            List<CategoryEntity> found = ((EasyLedgerApp) requireActivity().getApplication()).categories().picker(type, keyword);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                items.clear();
                items.addAll(found);
                if (binding != null && binding.list.getAdapter() != null) {
                    binding.list.getAdapter().notifyDataSetChanged();
                }
            });
        });
    }

    private class Adapter extends androidx.recyclerview.widget.RecyclerView.Adapter<Adapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemLibraryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CategoryEntity item = items.get(position);
            IconCatalog.apply(holder.binding.icon, item.icon);
            String prefix = item.parentId == null ? "" : "    ";
            holder.binding.name.setText(prefix + item.name + (item.favorite ? "  · 자주 사용" : ""));
            holder.binding.action.setText(R.string.pick_category);
            holder.binding.getRoot().setOnClickListener(v -> choose(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final ItemLibraryBinding binding;
            Holder(ItemLibraryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private void choose(CategoryEntity item) {
        Bundle result = new Bundle();
        result.putString("id", item.id);
        result.putString("name", item.name);
        result.putString("type", item.type);
        String request = getArguments() == null ? REQUEST : getArguments().getString("request", REQUEST);
        getParentFragmentManager().setFragmentResult(request, result);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
