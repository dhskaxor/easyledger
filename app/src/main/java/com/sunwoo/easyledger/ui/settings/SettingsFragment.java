package com.sunwoo.easyledger.ui.settings;

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
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.databinding.FragmentSettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.rowFixed.setOnClickListener(v -> navigate(R.id.fixedListFragment));
        binding.rowCategory.setOnClickListener(v -> navigate(R.id.categoryManageFragment));
        binding.rowBackup.setOnClickListener(v -> navigate(R.id.backupFragment));
        binding.rowApp.setOnClickListener(v -> navigate(R.id.appSettingsFragment));
        binding.rowAbout.setOnClickListener(v -> navigate(R.id.aboutFragment));
        binding.rowReset.setOnClickListener(v -> confirmReset());
    }

    private void navigate(int destination) {
        NavHostFragment.findNavController(this).navigate(destination);
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.reset_warning)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("계속", (dialog, which) ->
                        new MaterialAlertDialogBuilder(requireContext())
                                .setMessage(R.string.reset_final)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.reset_data, (d, w) -> reset())
                                .show())
                .show();
    }

    private void reset() {
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                ((EasyLedgerApp) requireActivity().getApplication()).categories().resetAllData();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.reset_done, Toast.LENGTH_SHORT).show();
                        requireActivity().recreate();
                    });
                }
            } catch (RuntimeException exception) {
                AppLog.error("Settings", "reset", exception);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "초기화하지 못했습니다.", Toast.LENGTH_SHORT).show());
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
