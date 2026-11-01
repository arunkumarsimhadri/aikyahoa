package com.aikya.domain.repository;

import com.aikya.domain.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {

    List<Poll> findAllByOrderByCreatedAtDesc();

    List<Poll> findByStatusOrderByCreatedAtDesc(String status);
}
