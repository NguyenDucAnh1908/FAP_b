package com.fap.settings.repository;

import com.fap.settings.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

	List<SystemSetting> findByCategory(String category);

	List<SystemSetting> findByCategoryOrderBySettingKeyAsc(String category);

	Optional<SystemSetting> findByCategoryAndSettingKey(String category, String settingKey);

	boolean existsByCategoryAndSettingKey(String category, String settingKey);
}

