package com.projeto.workflowengine.repository.user;

import com.projeto.workflowengine.core.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
