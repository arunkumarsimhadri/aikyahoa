package com.aikya.domain.repository;

import com.aikya.domain.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    boolean existsByPollIdAndUnitId(Long pollId, Long unitId);

    List<PollVote> findByPollId(Long pollId);

    Optional<PollVote> findByPollIdAndUnitId(Long pollId, Long unitId);
}
