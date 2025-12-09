package com.makersacademy.acebook.repository;

import com.makersacademy.acebook.enums.Status;
import com.makersacademy.acebook.model.Relationship;
import com.makersacademy.acebook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    Optional<Relationship> findByRequesterAndReceiver(User receiver, User requester);

    @Query("SELECT r FROM Relationship r JOIN FETCH r.requester WHERE r.receiver = :receiver AND r.status = :status")
    List<Relationship> findPendingRequests(@Param("receiver") User receiver, @Param("status") Status status);
}

