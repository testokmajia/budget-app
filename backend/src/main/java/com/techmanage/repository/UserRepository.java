package com.techmanage.repository;

import com.techmanage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByOpenId(String openId);
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByNameIn(List<String> names);

    /** 通过邮箱前缀（@之前的部分）查找用户 */
    @Query("SELECT u FROM User u WHERE u.email LIKE CONCAT(:prefix, '@%')")
    List<User> findByEmailLocalPart(@org.springframework.data.repository.query.Param("prefix") String prefix);

    @Modifying
    @Query("UPDATE User u SET u.department = :newName WHERE u.department = :oldName")
    int updateDepartment(String oldName, String newName);

    List<User> findByEnabledTrue();
}
