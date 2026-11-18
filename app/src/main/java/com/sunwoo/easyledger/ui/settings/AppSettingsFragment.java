package com.sunwoo.easyledger.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.SettingKeys;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.databinding.FragmentAppSettingsBinding;
import com.sunwoo.easyledger.domain.model.PaymentMethod;

public class AppSettingsFragment extends Fragment {
    private FragmentAppSettingsBinding binding;
    private boolean ready;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        EasyLedgerDatabase.executor().execute(() -> {
            EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
            String payment = app.settings().get(SettingKeys.DEFAULT_PAYMENT, PaymentMethod.CASH.name());
            boolean include = !"false".equals(app.settings().get(SettingKeys.INCLUDE_FIXED, "true"));
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                binding.paymentGroup.check(PaymentMethod.CARD.name().equals(payment) ? R.id.pay_card : R.id.pay_cash);
                binding.includeFixed.setChecked(include);
                ready = true;
            });
        });
        binding.paymentGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!ready) {
                return;
            }
            String value = checkedId == R.id.pay_card ? PaymentMethod.CARD.name() : PaymentMethod.CASH.name();
            EasyLedgerDatabase.executor().execute(() ->
                    ((EasyLedgerApp) requireActivity().getApplication()).settings().put(SettingKeys.DEFAULT_PAYMENT, value));
        });
        binding.includeFixed.setOnCheckedChangeListener((button, checked) -> {
            if (!ready) {
                return;
            }
            EasyLedgerDatabase.executor().execute(() ->
                    ((EasyLedgerApp) requireActivity().getApplication()).settings()
                            .put(SettingKeys.INCLUDE_FIXED, Boolean.toString(checked)));
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
