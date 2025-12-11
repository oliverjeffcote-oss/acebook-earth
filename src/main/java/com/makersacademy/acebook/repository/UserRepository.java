package com.makersacademy.acebook.repository;

import com.makersacademy.acebook.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    public Optional<User> findUserByUsername(String username);

    // Find users whose username contains the query (case-insensitive)
    List<User> findByUsernameContainingIgnoreCase(String query);
}
