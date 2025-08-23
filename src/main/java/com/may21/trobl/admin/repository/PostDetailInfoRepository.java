package com.may21.trobl.admin.repository;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.admin.domain.PostDetailInfo;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostDetailInfoRepository extends JpaRepository<PostDetailInfo, Long> {

    List<PostDetailInfo> findAllByPostIdIn(List<Long> oldPostIds);

    @Query("SELECT p FROM PostDetailInfo p WHERE p.postType IN :postingTypes")
    Page<PostDetailInfo> findAllContainsTagsFilteredByTypes(List<PostingType> postingTypes, Pageable pageable);
    // TOTAL_ENGAGEMENT: 좋아요 + 댓글 + 조회수 + 투표수의 합계로 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY (p.likeCount + p.commentCount + p" +
            ".viewCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsOrderByTotalEngagementDesc();

    // VIEW_COUNT: 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY p.viewCount DESC LIMIT 10")
    List<Long> findAllPostIdsOrderByViewCountDesc();

    // LIKE_COUNT: 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY p.likeCount DESC LIMIT 10")
    List<Long> findAllPostIdsOrderByLikeCountDesc();

    // PARTICIPANT_COUNT: 참여자 수 (댓글 + 투표로 가정)
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY (p.commentCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsOrderByParticipantCountDesc();

    // LIKE_COMMENT_COUNT: 좋아요 + 댓글 수
    @Query("SELECT p.postId FROM PostDetailInfo p ORDER BY (p.likeCount + p.commentCount) DESC LIMIT 10")
    List<Long> findAllPostIdsOrderByLikeCommentCountDesc();

    // TOTAL_ENGAGEMENT: 특정 날짜 이후, 총 참여도 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByTotalEngagementDesc(@Param("fromDate") LocalDateTime fromDate);


    // VIEW_COUNT: 특정 날짜 이후, 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.createdAt >= :fromDate ORDER BY p.viewCount DESC LIMIT 10")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByViewCountDesc(@Param("fromDate") LocalDateTime fromDate);

    // LIKE_COUNT: 특정 날짜 이후, 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.createdAt >= :fromDate ORDER BY p.likeCount DESC LIMIT 10")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByLikeCountDesc(@Param("fromDate") LocalDateTime fromDate);

    // PARTICIPANT_COUNT: 특정 날짜 이후, 참여자 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.createdAt >= :fromDate " +
            "ORDER BY (p.commentCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByParticipantCountDesc(@Param("fromDate") LocalDateTime fromDate);

    // LIKE_COMMENT_COUNT: 특정 날짜 이후, 좋아요 + 댓글 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByCreatedAtAfterOrderByLikeCommentCountDesc(@Param("fromDate") LocalDateTime fromDate);
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressOrderByTotalEngagementDesc(@Param("address") String address);

    // VIEW_COUNT: 특정 주소, 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address ORDER BY p.viewCount DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressOrderByViewCountDesc(@Param("address") String address);

    // LIKE_COUNT: 특정 주소, 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address ORDER BY p.likeCount DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressOrderByLikeCountDesc(@Param("address") String address);

    // PARTICIPANT_COUNT: 특정 주소, 참여자 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address " +
            "ORDER BY (p.commentCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressOrderByParticipantCountDesc(@Param("address") String address);

    // LIKE_COMMENT_COUNT: 특정 주소, 좋아요 + 댓글 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address " +
            "ORDER BY (p.likeCount + p.commentCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressOrderByLikeCommentCountDesc(@Param("address") String address);


    // 주소 + 시간 조건이 모두 있는 경우

    // TOTAL_ENGAGEMENT: 특정 주소 + 특정 날짜 이후, 총 참여도 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount + p.viewCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByTotalEngagementDesc(
            @Param("address") String address,
            @Param("fromDate") LocalDateTime fromDate);


    // VIEW_COUNT: 특정 주소 + 특정 날짜 이후, 조회수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY p.viewCount DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByViewCountDesc(
            @Param("address") String address,
            @Param("fromDate") LocalDateTime fromDate);

    // LIKE_COUNT: 특정 주소 + 특정 날짜 이후, 좋아요 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY p.likeCount DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByLikeCountDesc(
            @Param("address") String address,
            @Param("fromDate") LocalDateTime fromDate);

    // PARTICIPANT_COUNT: 특정 주소 + 특정 날짜 이후, 참여자 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY (p.commentCount + p.voteCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByParticipantCountDesc(
            @Param("address") String address,
            @Param("fromDate") LocalDateTime fromDate);

    // LIKE_COMMENT_COUNT: 특정 주소 + 특정 날짜 이후, 좋아요 + 댓글 수 기준 정렬
    @Query("SELECT p.postId FROM PostDetailInfo p WHERE p.address = :address AND p.createdAt >= :fromDate " +
            "ORDER BY (p.likeCount + p.commentCount) DESC LIMIT 10")
    List<Long> findAllPostIdsByAddressCreatedAtAfterOrderByLikeCommentCountDesc(
            @Param("address") String address,
            @Param("fromDate") LocalDateTime fromDate);


}
