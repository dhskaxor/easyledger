package com.sunwoo.easyledger.data.repository;

import com.sunwoo.easyledger.data.dao.AppSettingDao;
import com.sunwoo.easyledger.data.database.EasyLedgerDatabase;
import com.sunwoo.easyledger.data.entity.AppSettingEntity;

public class SettingsRepository {
    private final AppSettingDao dao;

    public SettingsRepository(EasyLedgerDatabase database) {
        dao = database.appSettingDao();
    }

    public String get(String key, String fallback) {
        String value = dao.get(key);
        return value == null ? fallback : value;
    }

    public void put(String key, String value) {
        AppSettingEntity entity = new AppSettingEntity();
        entity.key = key;
        entity.value = value == null ? "" : value;
        entity.updatedAt = System.currentTimeMillis();
        dao.put(entity);
    }
}
