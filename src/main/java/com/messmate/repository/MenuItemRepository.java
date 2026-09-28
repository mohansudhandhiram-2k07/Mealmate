package com.messmate.repository;

import com.messmate.entity.Category;
import com.messmate.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByCategoryAndNameContainingIgnoreCase(Category category, String name);

    List<MenuItem> findByCategory(Category category);

    List<MenuItem> findByNameContainingIgnoreCase(String name);
}
