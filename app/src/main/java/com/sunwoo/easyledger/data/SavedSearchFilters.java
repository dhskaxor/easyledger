package com.sunwoo.easyledger.data;

import com.sunwoo.easyledger.common.SettingKeys;
import com.sunwoo.easyledger.data.repository.SettingsRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SavedSearchFilters {
    public static final int MAX = 8;

    public static final class Filter {
        public final String name;
        public final String start;
        public final String end;
        public final String memo;
        public final String payment;
        public final Long minAmount;
        public final Long maxAmount;
        public final List<String> categoryIds;

        public Filter(
                String name,
                String start,
                String end,
                String memo,
                String payment,
                Long minAmount,
                Long maxAmount,
                List<String> categoryIds
        ) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.memo = memo == null ? "" : memo;
            this.payment = payment;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.categoryIds = categoryIds == null ? Collections.emptyList() : categoryIds;
        }
    }

    private SavedSearchFilters() {
    }

    public static List<Filter> load(SettingsRepository settings) {
        String raw = settings.get(SettingKeys.SAVED_SEARCH_FILTERS, "[]");
        List<Filter> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                List<String> cats = new ArrayList<>();
                JSONArray ids = row.optJSONArray("categories");
                if (ids != null) {
                    for (int j = 0; j < ids.length(); j++) {
                        cats.add(ids.getString(j));
                    }
                }
                Long min = row.has("min") && !row.isNull("min") ? row.getLong("min") : null;
                Long max = row.has("max") && !row.isNull("max") ? row.getLong("max") : null;
                out.add(new Filter(
                        row.optString("name", "조건"),
                        row.optString("start"),
                        row.optString("end"),
                        row.optString("memo", ""),
                        row.isNull("payment") ? null : row.optString("payment", null),
                        min,
                        max,
                        cats
                ));
            }
        } catch (JSONException ignored) {
            return Collections.emptyList();
        }
        return out;
    }

    public static void save(SettingsRepository settings, List<Filter> filters) {
        JSONArray array = new JSONArray();
        int limit = Math.min(filters.size(), MAX);
        for (int i = 0; i < limit; i++) {
            Filter filter = filters.get(i);
            JSONObject row = new JSONObject();
            try {
                row.put("name", filter.name);
                row.put("start", filter.start);
                row.put("end", filter.end);
                row.put("memo", filter.memo == null ? "" : filter.memo);
                if (filter.payment == null) {
                    row.put("payment", JSONObject.NULL);
                } else {
                    row.put("payment", filter.payment);
                }
                if (filter.minAmount == null) {
                    row.put("min", JSONObject.NULL);
                } else {
                    row.put("min", filter.minAmount);
                }
                if (filter.maxAmount == null) {
                    row.put("max", JSONObject.NULL);
                } else {
                    row.put("max", filter.maxAmount);
                }
                JSONArray cats = new JSONArray();
                for (String id : filter.categoryIds) {
                    cats.put(id);
                }
                row.put("categories", cats);
                array.put(row);
            } catch (JSONException ignored) {
            }
        }
        settings.put(SettingKeys.SAVED_SEARCH_FILTERS, array.toString());
    }

    public static void add(SettingsRepository settings, Filter filter) {
        List<Filter> current = new ArrayList<>(load(settings));
        current.removeIf(item -> item.name.equals(filter.name));
        current.add(0, filter);
        if (current.size() > MAX) {
            current = new ArrayList<>(current.subList(0, MAX));
        }
        save(settings, current);
    }

    public static void remove(SettingsRepository settings, String name) {
        List<Filter> current = new ArrayList<>(load(settings));
        current.removeIf(item -> item.name.equals(name));
        save(settings, current);
    }
}
