package com.sunwoo.easyledger.data.repository;

import androidx.lifecycle.LiveData;

import com.sunwoo.easyledger.common.SettingKeys;
import com.sunwoo.easyledger.data.dao.AppSettingDao;
import com.sunwoo.easyledger.data.dao.CategoryDao;
import com.sunwoo.easyledger.data.dao.CategoryLibraryDao;
import com.sunwoo.easyledger.data.dao.FixedOccurrenceDao;
import com.sunwoo.easyledger.data.dao.FixedTransactionDao;
import com.sunwoo.easyledger.data.dao.SchemaMetadataDao;
import com.sunwoo.easyledger.data.dao.TransactionDao;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.database.LibraryItem;
import com.sunwoo.easyledger.data.entity.AppSettingEntity;
import com.sunwoo.easyledger.data.entity.CategoryEntity;
import com.sunwoo.easyledger.data.entity.CategoryLibraryEntity;
import com.sunwoo.easyledger.data.entity.SchemaMetadataEntity;
import com.sunwoo.easyledger.domain.model.Limits;
import com.sunwoo.easyledger.domain.model.TransactionType;
import com.sunwoo.easyledger.domain.model.UserVisibleException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CategoryRepository {
    private final EasyLedgerDatabase database;
    private final CategoryDao categoryDao;
    private final CategoryLibraryDao libraryDao;
    private final TransactionDao transactionDao;
    private final FixedTransactionDao fixedDao;
    private final FixedOccurrenceDao occurrenceDao;
    private final AppSettingDao settingDao;
    private final SchemaMetadataDao metadataDao;

    public CategoryRepository(EasyLedgerDatabase database) {
        this.database = database;
        categoryDao = database.categoryDao();
        libraryDao = database.categoryLibraryDao();
        transactionDao = database.transactionDao();
        fixedDao = database.fixedTransactionDao();
        occurrenceDao = database.fixedOccurrenceDao();
        settingDao = database.appSettingDao();
        metadataDao = database.schemaMetadataDao();
    }

    public void ensureSeeded() {
        database.runInTransaction(() -> {
            for (LibraryItem item : LibraryItem.all()) {
                libraryDao.insert(toLibrary(item));
            }
            if (!"2".equals(metadataDao.get("schema_version"))) {
                SchemaMetadataEntity version = new SchemaMetadataEntity();
                version.key = "schema_version";
                version.value = "2";
                metadataDao.put(version);
            }
            if (categoryDao.count() > 0 && !"true".equals(settingDao.get(SettingKeys.ONBOARDING_DONE))) {
                putSetting(SettingKeys.ONBOARDING_DONE, "true");
            }
        });
    }

    public boolean needsOnboarding() {
        return !"true".equals(settingDao.get(SettingKeys.ONBOARDING_DONE));
    }

    public void completeOnboarding(Collection<String> selectedLibraryIds) throws UserVisibleException {
        database.runInTransaction(() -> {
            Set<String> selected = new HashSet<>();
            if (selectedLibraryIds != null) {
                selected.addAll(selectedLibraryIds);
            }
            boolean hasExpense = false;
            boolean hasIncome = false;
            Map<String, LibraryItem> catalog = new HashMap<>();
            for (LibraryItem item : LibraryItem.all()) {
                catalog.put(item.id, item);
            }
            for (String id : selected) {
                LibraryItem item = catalog.get(id);
                if (item == null || (item.parentId != null && !item.parentId.isEmpty())) {
                    continue;
                }
                if (TransactionType.EXPENSE.name().equals(item.type)) {
                    hasExpense = true;
                } else if (TransactionType.INCOME.name().equals(item.type)) {
                    hasIncome = true;
                }
            }
            if (!hasExpense) {
                selected.add("exp_etc");
            }
            if (!hasIncome) {
                selected.add("inc_etc");
            }
            Map<String, String> created = new HashMap<>();
            for (LibraryItem item : LibraryItem.all()) {
                if ((item.parentId == null || item.parentId.isEmpty()) && selected.contains(item.id)) {
                    insertCatalogItem(item, null, true, created);
                }
            }
            putSetting(SettingKeys.ONBOARDING_DONE, "true");
        });
    }

    public LiveData<List<CategoryEntity>> observeAll() {
        return categoryDao.observeAll();
    }

    public List<CategoryEntity> picker(String type, String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        int useQuery = query.isEmpty() ? 0 : 1;
        String pattern = useQuery == 0 ? "%" : com.sunwoo.easyledger.domain.model.SearchCriteria.likePattern(query);
        return categoryDao.picker(type, useQuery, pattern);
    }

    public List<CategoryEntity> all() {
        return categoryDao.all();
    }

    public CategoryEntity get(String id) {
        return categoryDao.get(id);
    }

    public void save(CategoryEntity draft) throws UserVisibleException {
        String name = draft.name == null ? "" : draft.name.trim();
        if (name.isEmpty() || name.length() > Limits.CATEGORY_NAME) {
            throw new UserVisibleException("카테고리 이름은 1~20자로 입력해 주세요.");
        }
        try {
            TransactionType.fromStorage(draft.type);
        } catch (RuntimeException exception) {
            throw new UserVisibleException("카테고리 유형을 선택해 주세요.");
        }
        if (draft.icon == null || draft.icon.isEmpty()) {
            draft.icon = "generic";
        }
        long now = System.currentTimeMillis();
        database.runInTransaction(() -> {
            if (draft.parentId != null && draft.parentId.isEmpty()) {
                draft.parentId = null;
            }
            if (draft.parentId != null) {
                CategoryEntity parent = categoryDao.get(draft.parentId);
                if (parent == null || parent.parentId != null) {
                    throw new UserVisibleException("상위 카테고리를 확인해 주세요.");
                }
                if (!parent.type.equals(draft.type)) {
                    throw new UserVisibleException("하위 카테고리는 상위와 같은 유형이어야 합니다.");
                }
            }
            normalizeQuickAmounts(draft);
            CategoryEntity existing = draft.id == null ? null : categoryDao.get(draft.id);
            if (existing == null) {
                draft.id = UUID.randomUUID().toString();
                draft.name = name;
                draft.displayOrder = categoryDao.nextOrder(draft.type, draft.parentId == null ? "" : draft.parentId);
                draft.createdAt = now;
                draft.updatedAt = now;
                categoryDao.insert(draft);
            } else {
                existing.name = name;
                existing.icon = draft.icon;
                existing.favorite = draft.favorite;
                existing.hidden = draft.hidden;
                existing.quickAmount1 = draft.quickAmount1;
                existing.quickAmount2 = draft.quickAmount2;
                existing.quickAmount3 = draft.quickAmount3;
                existing.updatedAt = now;
                categoryDao.update(existing);
            }
        });
    }

    private static void normalizeQuickAmounts(CategoryEntity draft) throws UserVisibleException {
        long[] raw = {draft.quickAmount1, draft.quickAmount2, draft.quickAmount3};
        long[] cleaned = new long[3];
        int index = 0;
        for (long amount : raw) {
            if (amount < 0) {
                throw new UserVisibleException("자주 쓰는 금액은 0보다 커야 합니다.");
            }
            if (amount == 0) {
                continue;
            }
            String digits = Long.toString(amount);
            if (digits.length() > Limits.AMOUNT_DIGITS) {
                throw new UserVisibleException("자주 쓰는 금액이 너무 큽니다.");
            }
            if (index < cleaned.length) {
                cleaned[index++] = amount;
            }
        }
        draft.quickAmount1 = cleaned[0];
        draft.quickAmount2 = cleaned[1];
        draft.quickAmount3 = cleaned[2];
    }

    public void setHidden(String id, boolean hidden) {
        categoryDao.setHidden(id, hidden, System.currentTimeMillis());
    }

    public void setFavorite(String id, boolean favorite) {
        categoryDao.setFavorite(id, favorite, System.currentTimeMillis());
    }

    public void move(String id, boolean up) throws UserVisibleException {
        database.runInTransaction(() -> {
            CategoryEntity current = categoryDao.get(id);
            if (current == null) {
                throw new UserVisibleException("카테고리를 찾을 수 없습니다.");
            }
            List<CategoryEntity> siblings = new ArrayList<>();
            for (CategoryEntity category : categoryDao.all()) {
                boolean sameParent = (current.parentId == null && category.parentId == null)
                        || (current.parentId != null && current.parentId.equals(category.parentId));
                if (sameParent && current.type.equals(category.type)) {
                    siblings.add(category);
                }
            }
            siblings.sort((left, right) -> Integer.compare(left.displayOrder, right.displayOrder));
            int index = -1;
            for (int i = 0; i < siblings.size(); i++) {
                if (siblings.get(i).id.equals(id)) {
                    index = i;
                    break;
                }
            }
            int target = up ? index - 1 : index + 1;
            if (index < 0 || target < 0 || target >= siblings.size()) {
                return;
            }
            CategoryEntity other = siblings.get(target);
            int order = current.displayOrder;
            current.displayOrder = other.displayOrder;
            other.displayOrder = order;
            long now = System.currentTimeMillis();
            current.updatedAt = now;
            other.updatedAt = now;
            categoryDao.update(current);
            categoryDao.update(other);
        });
    }

    public void resetAllData() {
        database.runInTransaction(() -> {
            occurrenceDao.deleteAll();
            transactionDao.deleteAll();
            fixedDao.deleteAll();
            deleteAllCategories();
            settingDao.deleteAll();
            for (LibraryItem item : LibraryItem.all()) {
                libraryDao.insert(toLibrary(item));
            }
        });
    }

    public void deleteAllCategories() {
        int guard = 0;
        while (categoryDao.count() > 0) {
            int deleted = categoryDao.deleteLeaves();
            if (deleted == 0 || guard++ > 20) {
                throw new IllegalStateException("categories");
            }
        }
    }

    private void insertCatalogItem(
            LibraryItem item,
            String parentCategoryId,
            boolean favorite,
            Map<String, String> created
    ) {
        String mapped = settingDao.get(SettingKeys.libraryKey(item.id));
        if (mapped != null && categoryDao.get(mapped) != null) {
            created.put(item.id, mapped);
            return;
        }
        CategoryEntity category = new CategoryEntity();
        category.id = UUID.randomUUID().toString();
        category.name = item.name;
        category.type = item.type;
        category.parentId = parentCategoryId;
        category.icon = item.icon;
        category.displayOrder = categoryDao.nextOrder(item.type, parentCategoryId == null ? "" : parentCategoryId);
        category.hidden = false;
        category.favorite = favorite;
        category.createdAt = System.currentTimeMillis();
        category.updatedAt = category.createdAt;
        categoryDao.insert(category);
        AppSettingEntity setting = new AppSettingEntity();
        setting.key = SettingKeys.libraryKey(item.id);
        setting.value = category.id;
        setting.updatedAt = category.createdAt;
        settingDao.put(setting);
        created.put(item.id, category.id);
    }

    private void putSetting(String key, String value) {
        AppSettingEntity setting = new AppSettingEntity();
        setting.key = key;
        setting.value = value;
        setting.updatedAt = System.currentTimeMillis();
        settingDao.put(setting);
    }

    private static CategoryLibraryEntity toLibrary(LibraryItem item) {
        CategoryLibraryEntity entity = new CategoryLibraryEntity();
        entity.id = item.id;
        entity.name = item.name;
        entity.type = item.type;
        entity.parentId = item.parentId == null || item.parentId.isEmpty() ? null : item.parentId;
        entity.icon = item.icon;
        entity.defaultEnabled = item.defaultEnabled;
        return entity;
    }
}
