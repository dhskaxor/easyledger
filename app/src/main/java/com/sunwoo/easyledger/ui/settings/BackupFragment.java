package com.sunwoo.easyledger.ui.settings;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sunwoo.easyledger.R;
import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.databinding.FragmentBackupBinding;
import com.sunwoo.easyledger.domain.backup.BackupFiles;
import com.sunwoo.easyledger.domain.backup.BackupPreview;
import com.sunwoo.easyledger.domain.model.UserVisibleException;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupFragment extends Fragment {
    private FragmentBackupBinding binding;
    private byte[] pendingZip;
    private final ActivityResultLauncher<String> createDocument =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), this::writeBackup);
    private final ActivityResultLauncher<String[]> openDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::readBackup);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBackupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.backupButton.setOnClickListener(v -> startBackup());
        binding.restoreButton.setOnClickListener(v -> openDocument.launch(new String[] {"application/zip", "application/octet-stream", "*/*"}));
    }

    private void startBackup() {
        setBusy(true);
        EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                byte[] zip = app.backup().exportZip();
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    pendingZip = zip;
                    String name = "쉬운가계부_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip";
                    setBusy(false);
                    createDocument.launch(name);
                });
            } catch (RuntimeException exception) {
                AppLog.error("Backup", "export", exception);
                show(UserMessages.from(exception));
            }
        });
    }

    private void writeBackup(Uri uri) {
        if (uri == null || pendingZip == null) {
            return;
        }
        setBusy(true);
        android.content.ContentResolver resolver = requireContext().getContentResolver();
        EasyLedgerDatabase.executor().execute(() -> {
            try (OutputStream output = resolver.openOutputStream(uri)) {
                if (output == null) {
                    throw new IllegalStateException("output");
                }
                output.write(pendingZip);
                show(getString(R.string.backup_done));
            } catch (Exception exception) {
                AppLog.error("Backup", "write", exception);
                show(UserMessages.STORAGE);
            }
        });
    }

    private void readBackup(Uri uri) {
        if (uri == null) {
            return;
        }
        setBusy(true);
        EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
        android.content.ContentResolver resolver = requireContext().getContentResolver();
        EasyLedgerDatabase.executor().execute(() -> {
            try (InputStream input = resolver.openInputStream(uri)) {
                if (input == null) {
                    throw new IllegalStateException("input");
                }
                byte[] bytes = readLimited(input);
                BackupPreview preview = app.backup().preview(bytes);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> confirmRestore(bytes, preview));
                }
            } catch (Exception exception) {
                AppLog.error("Backup", "read", exception);
                show(exception instanceof RuntimeException ? UserMessages.from((RuntimeException) exception) : "백업 파일을 열 수 없습니다.");
            }
        });
    }

    private void confirmRestore(byte[] bytes, BackupPreview preview) {
        setBusy(false);
        String message = "거래 " + preview.transactionCount + "건\n카테고리 " + preview.categoryCount
                + "개\n반복 거래 " + preview.fixedCount + "개\n발생 이력 " + preview.occurrenceCount + "건\n\n"
                + getString(R.string.restore_warning);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("복원 미리보기")
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore_action, (dialog, which) -> runRestore(bytes))
                .show();
    }

    private void runRestore(byte[] bytes) {
        setBusy(true);
        EasyLedgerApp app = (EasyLedgerApp) requireActivity().getApplication();
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                app.backup().restore(bytes);
                show(getString(R.string.restore_done));
            } catch (RuntimeException exception) {
                AppLog.error("Backup", "restore", exception);
                show(UserMessages.from(exception));
            }
        });
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(chunk)) >= 0) {
            total += read;
            if (total > BackupFiles.MAX_ZIP_BYTES) {
                throw new UserVisibleException("백업 파일이 너무 큽니다.");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private void setBusy(boolean busy) {
        if (binding == null) {
            return;
        }
        binding.backupButton.setEnabled(!busy);
        binding.restoreButton.setEnabled(!busy);
        binding.statusText.setText(busy ? "처리 중입니다." : "");
    }

    private void show(String message) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            setBusy(false);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            if (binding != null) {
                binding.statusText.setText(message);
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
