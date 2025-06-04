package com.kakaobase.snsapp.domain.follow.repository;


import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerUserAndFollowingUser(Member followerUser, Member followingUser);

    Optional<Follow> findByFollowerUserAndFollowingUser(Member followerUser, Member followingUser);

    @Query(value = """
    SELECT m.id, m.nickname, m.name, m.profile_image
    FROM follow f
    JOIN member m ON f.follower_user_id = m.id
    WHERE f.following_id = :followingId
      AND (:cursor IS NULL OR m.id > :cursor)
    ORDER BY m.id ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findFollowersByFollowingUserWithCursor(
            @Param("followingId") Long followingId,
            @Param("limit") Integer limit,
            @Param("cursor") Long cursor

    );

}
