package com.sunwoo.easyledger.ui.input;

import android.app.Application;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.sunwoo.easyledger.app.EasyLedgerApp;
import com.sunwoo.easyledger.common.AppLog;
import com.sunwoo.easyledger.common.DateTimes;
import com.sunwoo.easyledger.common.Event;
import com.sunwoo.easyledger.common.SettingKeys;
import com.sunwoo.easyledger.common.UserMessages;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.TransactionEntity;
import com.sunwoo.easyledger.domain.calculator.Money;
import com.sunwoo.easyledger.domain.model.PaymentMethod;
import com.sunwoo.easyledger.domain.model.TransactionRules;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.sunwoo.easyledger.domain.model.UserVisibleException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InputViewModel extends AndroidViewModel {
    public final MutableLiveData<String> type = new MutableLiveData<>(TransactionType.EXPENSE.name());
    public final MutableLiveData<String> digits = new MutableLiveData<>("0");
    public final MutableLiveData<LocalDate> date = new MutableLiveData<>(LocalDate.now());
    public final MutableLiveData<String> categoryId = new MutableLiveData<>();
    public final MutableLiveData<String> categoryName = new MutableLiveData<>();
    public final MutableLiveData<String> categoryType = new MutableLiveData<>();
    public final MutableLiveData<List<Long>> quickAmounts = new MutableLiveData<>(Collections.emptyList());
    public final MutableLiveData<Boolean> categoryExpanded = new MutableLiveData<>(true);
    public final MutableLiveData<Boolean> detailsVisible = new MutableLiveData<>(false);
    public final MutableLiveData<String> payment = new MutableLiveData<>(PaymentMethod.CASH.name());
    public final MutableLiveData<String> memo = new MutableLiveData<>("");
    public final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    public final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    public final MutableLiveData<Event<Boolean>> saved = new MutableLiveData<>();
    public final MediatorLiveData<List<CategoryEntity>> categories = new MediatorLiveData<>();

    private final EasyLedgerApp app;
    private boolean started;
    private boolean editing;
    private String editingId;
    private LocalTime editingTime = DateTimes.now();
    private List<CategoryEntity> allCategories = Collections.emptyList();

    public InputViewModel(@NonNull Application application) {
        super(application);
        app = (EasyLedgerApp) application;
        LiveData<List<CategoryEntity>> source = app.categories().observeAll();
        categories.addSource(source, value -> {
            allCategories = value == null ? Collections.emptyList() : value;
            publishCategories();
            refreshQuickAmounts();
        });
        categories.addSource(type, ignored -> publishCategories());
        categories.addSource(categoryId, ignored -> publishCategories());
        categories.addSource(categoryExpanded, ignored -> publishCategories());
    }

    public boolean isEditing() {
        return editing;
    }

    public void start(String transactionId) {
        if (started) {
            return;
        }
        started = true;
        editing = transactionId != null && !transactionId.isEmpty();
        editingId = transactionId;
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                if (!editing) {
                    payment.postValue(app.settings().get(SettingKeys.DEFAULT_PAYMENT, PaymentMethod.CASH.name()));
                    syncUiState();
                    return;
                }
                TransactionEntity entity = app.transactions().get(editingId);
                if (entity == null) {
                    message.postValue(new Event<>("거래를 찾을 수 없습니다."));
                    return;
                }
                CategoryEntity category = app.categories().get(entity.categoryId);
                if (category == null) {
                    message.postValue(new Event<>("카테고리를 찾을 수 없습니다. 카테고리 관리에서 확인해 주세요."));
                }
                type.postValue(entity.type);
                digits.postValue(Long.toString(entity.amount));
                date.postValue(LocalDate.parse(entity.transactionDate));
                editingTime = LocalTime.parse(entity.transactionTime);
                categoryId.postValue(entity.categoryId);
                categoryName.postValue(category == null ? "" : category.name);
                categoryType.postValue(category == null ? entity.type : category.type);
                payment.postValue(entity.paymentMethod);
                memo.postValue(entity.memo == null ? "" : entity.memo);
                quickAmounts.postValue(category == null ? Collections.emptyList() : category.quickAmounts());
                categoryExpanded.postValue(false);
                detailsVisible.postValue(true);
            } catch (RuntimeException exception) {
                AppLog.error("Input", "load", exception);
                message.postValue(new Event<>(UserMessages.LOAD_FAILED));
            }
        });
    }

    public void setType(String value) {
        if (value.equals(type.getValue())) {
            return;
        }
        type.setValue(value);
        if (categoryType.getValue() != null && !value.equals(categoryType.getValue())) {
            clearCategory();
            categoryExpanded.setValue(true);
            syncUiState();
        }
    }

    public void append(String addition) {
        digits.setValue(Money.appendDigits(digits.getValue(), addition));
    }

    public void backspace() {
        digits.setValue(Money.backspace(digits.getValue()));
    }

    public void setAmount(long amount) {
        if (amount <= 0) {
            digits.setValue("0");
            return;
        }
        digits.setValue(Long.toString(amount));
    }

    public void setDate(LocalDate value) {
        date.setValue(value);
    }

    public void onCategoryClicked(CategoryEntity category) {
        if (category == null) {
            return;
        }
        String current = categoryId.getValue();
        if (category.id.equals(current)) {
            boolean expanded = Boolean.TRUE.equals(categoryExpanded.getValue());
            categoryExpanded.setValue(!expanded);
            syncUiState();
            return;
        }
        categoryId.setValue(category.id);
        categoryName.setValue(category.name);
        categoryType.setValue(category.type);
        quickAmounts.setValue(category.quickAmounts());
        categoryExpanded.setValue(false);
        syncUiState();
    }

    public void setPayment(String value) {
        payment.setValue(value);
    }

    public void setMemo(String value) {
        if (value != null && !value.equals(memo.getValue())) {
            memo.setValue(value);
        }
    }

    public void save() {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        String amountError = TransactionRules.validateDigits(digits.getValue());
        if (amountError != null) {
            message.setValue(new Event<>(amountError));
            return;
        }
        if (categoryId.getValue() == null || categoryId.getValue().isEmpty()) {
            message.setValue(new Event<>("카테고리를 선택해 주세요."));
            return;
        }
        String memoValue = memo.getValue() == null ? "" : memo.getValue();
        if (memoValue.length() > 200) {
            message.setValue(new Event<>("메모는 200자까지 입력할 수 있습니다."));
            return;
        }
        saving.setValue(true);
        LocalTime chosen = editing ? editingTime : DateTimes.now();
        TransactionEntity entity = new TransactionEntity();
        entity.id = editing ? editingId : "";
        entity.type = type.getValue();
        entity.amount = Money.toAmount(digits.getValue());
        entity.transactionDate = date.getValue().toString();
        entity.transactionTime = DateTimes.formatTime(chosen);
        entity.categoryId = categoryId.getValue();
        entity.paymentMethod = payment.getValue();
        entity.memo = memoValue;
        EasyLedgerDatabase.executor().execute(() -> {
            try {
                if (editing) {
                    app.transactions().update(entity);
                } else {
                    app.transactions().insert(entity);
                    app.settings().put(SettingKeys.DEFAULT_PAYMENT, entity.paymentMethod);
                }
                saved.postValue(new Event<>(editing));
            } catch (UserVisibleException exception) {
                message.postValue(new Event<>(exception.getMessage()));
            } catch (RuntimeException exception) {
                AppLog.error("Input", "save", exception);
                message.postValue(new Event<>(UserMessages.SAVE_FAILED));
            } finally {
                saving.postValue(false);
            }
        });
    }

    public void onCreated() {
        digits.setValue("0");
        clearCategory();
        memo.setValue("");
        editingTime = DateTimes.now();
        categoryExpanded.setValue(true);
        syncUiState();
    }

    private void clearCategory() {
        categoryId.setValue(null);
        categoryName.setValue(null);
        categoryType.setValue(null);
        quickAmounts.setValue(Collections.emptyList());
    }

    private void syncUiState() {
        String id = categoryId.getValue();
        boolean selected = id != null && !id.isEmpty();
        boolean expanded = Boolean.TRUE.equals(categoryExpanded.getValue());
        boolean visible = selected && !expanded;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            detailsVisible.setValue(visible);
        } else {
            detailsVisible.postValue(visible);
        }
    }

    private void publishCategories() {
        String currentType = type.getValue() == null ? TransactionType.EXPENSE.name() : type.getValue();
        String selected = categoryId.getValue();
        List<CategoryEntity> rows = new ArrayList<>();
        for (CategoryEntity category : allCategories) {
            if (!currentType.equals(category.type)) {
                continue;
            }
            boolean pinned = selected != null && selected.equals(category.id);
            if (category.hidden && !pinned) {
                continue;
            }
            rows.add(category);
        }
        rows.sort((left, right) -> {
            if (left.favorite != right.favorite) {
                return left.favorite ? -1 : 1;
            }
            int order = Integer.compare(left.displayOrder, right.displayOrder);
            if (order != 0) {
                return order;
            }
            return left.name.compareTo(right.name);
        });
        if (Boolean.TRUE.equals(categoryExpanded.getValue()) || selected == null || selected.isEmpty()) {
            categories.setValue(rows);
            return;
        }
        List<CategoryEntity> collapsed = new ArrayList<>(1);
        for (CategoryEntity category : rows) {
            if (category.id.equals(selected)) {
                collapsed.add(category);
                break;
            }
        }
        categories.setValue(collapsed.isEmpty() ? rows : collapsed);
    }

    private void refreshQuickAmounts() {
        String id = categoryId.getValue();
        if (id == null || id.isEmpty()) {
            return;
        }
        for (CategoryEntity category : allCategories) {
            if (id.equals(category.id)) {
                quickAmounts.setValue(category.quickAmounts());
                return;
            }
        }
    }
}
