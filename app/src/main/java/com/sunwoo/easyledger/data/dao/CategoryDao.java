package com.sunwoo.easyledger.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sunwoo.easyledger.data.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(CategoryEntity entity);

    @Update
    void update(CategoryEntity entity);

    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity get(String id);

    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("SELECT * FROM categories ORDER BY type, display_order, name")
    LiveData<List<CategoryEntity>> observeAll();

    @Query("SELECT * FROM categories ORDER BY type, display_order, name")
    List<CategoryEntity> all();

    @Query("UPDATE categories SET is_hidden = :hidden, updated_at = :updatedAt WHERE id = :id")
    void setHidden(String id, boolean hidden, long updatedAt);

    @Query("UPDATE categories SET is_favorite = :favorite, updated_at = :updatedAt WHERE id = :id")
    void setFavorite(String id, boolean favorite, long updatedAt);

    @Query("SELECT COALESCE(MAX(display_order), -1) + 1 FROM categories "
            + "WHERE type = :type AND IFNULL(parent_id, '') = :parentId")
    int nextOrder(String type, String parentId);

    @Query("DELETE FROM categories WHERE id NOT IN "
            + "(SELECT parent_id FROM categories WHERE parent_id IS NOT NULL)")
    int deleteLeaves();

    @Query("SELECT * FROM categories WHERE type = :type AND is_hidden = 0 "
            + "AND (parent_id IS NULL OR parent_id IN (SELECT id FROM categories WHERE is_hidden = 0)) "
            + "AND (:useQuery = 0 OR name LIKE :pattern ESCAPE '\\') "
            + "ORDER BY is_favorite DESC, "
            + "(SELECT COUNT(*) FROM transactions t WHERE t.category_id = categories.id) DESC, "
            + "display_order ASC, name ASC")
    List<CategoryEntity> picker(String type, int useQuery, String pattern);
}
