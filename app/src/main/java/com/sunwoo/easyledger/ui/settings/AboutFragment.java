package com.sunwoo.easyledger.ui.settings;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sunwoo.easyledger.BuildConfig;
import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {
    private static final String SUPPORT_EMAIL = "onamt@naver.com";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentAboutBinding binding = FragmentAboutBinding.inflate(inflater, container, false);
        binding.versionText.setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));
        applyTitleBody(binding.helpInput, R.string.about_help_input);
        applyTitleBody(binding.helpCalendar, R.string.about_help_calendar);
        applyTitleBody(binding.helpHistory, R.string.about_help_history);
        applyTitleBody(binding.helpStats, R.string.about_help_stats);
        applyTitleBody(binding.helpCategory, R.string.about_help_category);
        applyTitleBody(binding.helpFixed, R.string.about_help_fixed);
        applyTitleBody(binding.helpBackup, R.string.about_help_backup);
        applyTitleBody(binding.extraWidget, R.string.about_extra_widget);
        applyTitleBody(binding.extraNotify, R.string.about_extra_notify);
        applyTitleBody(binding.extraFilter, R.string.about_extra_filter);
        binding.emailButton.setOnClickListener(v -> openMail());
        return binding.getRoot();
    }

    private void applyTitleBody(TextView textView, int stringId) {
        String value = getString(stringId);
        int breakAt = value.indexOf('\n');
        if (breakAt <= 0) {
            textView.setText(value);
            return;
        }
        SpannableString spanned = new SpannableString(value);
        spanned.setSpan(new StyleSpan(Typeface.BOLD), 0, breakAt, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spanned);
    }

    private void openMail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_email_subject));
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.about_email)));
        } catch (RuntimeException exception) {
            Toast.makeText(requireContext(), R.string.about_email_failed, Toast.LENGTH_LONG).show();
        }
    }
}
