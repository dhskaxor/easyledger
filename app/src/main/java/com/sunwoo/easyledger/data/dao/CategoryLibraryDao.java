package com.sunwoo.easyledger.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.sunwoo.easyledger.data.entity.CategoryLibraryEntity;

import java.util.List;

@Dao
public interface CategoryLibraryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(CategoryLibraryEntity entity);

    @Query("SELECT * FROM category_library WHERE id = :id")
    CategoryLibraryEntity get(String id);

    @Query("SELECT * FROM category_library ORDER BY type, name")
    List<CategoryLibraryEntity> all();
}
