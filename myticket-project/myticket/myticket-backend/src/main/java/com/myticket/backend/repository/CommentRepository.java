package com.myticket.backend.repository;

import com.myticket.backend.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndParentIsNullOrderByCreatedAtDesc(Long eventId);

    void deleteByEventId(Long eventId);

    List<Comment> findByEventIdAndParentIsNullOrderByCreatedAtAsc(Long eventId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}
