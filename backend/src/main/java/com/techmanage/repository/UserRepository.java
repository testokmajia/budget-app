package com.techmanage.repository;

import com.techmanage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByOpenId(String openId);
    Optional<User> findByName(String name);
    boolean existsByUsername(String username);
    List<User> findByNameIn(List<String> names);
}
