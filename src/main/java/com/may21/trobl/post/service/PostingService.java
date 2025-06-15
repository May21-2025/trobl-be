package com.may21.trobl.post.service;

import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PostingService {

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getPostsList(Pageable pageable, Long userId);

    @Transactional(readOnly = true)
    List<PostDto.Card> getTop10Views(String type);

    @Transactional
    PostDto.Detail getPostDetail(Long postId, User user);

    @Transactional
    PostDto.Detail createPost(PostDto.Request request, Long id);

    @Transactional
    PostDto.Detail updatePost(PostDto.Request request, Long userId, Long postId);

    @Transactional
    boolean votePoll(Long pollOptionId, Long userId);

    @Transactional
    boolean sharePost(Long postId, Long userId);

    @Transactional
    boolean viewPost(Long postId, Long userId);

    @Transactional
    PostDto.ListItem likePost(Long postId, Long userId);

    @Transactional
    boolean deletePost(Long userId, Long postId);

    Page<PostDto.ListItem> getMyPosts(Long userId, int page, int size);

    Page<PostDto.ListItem> getLikedPosts(Long userId, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getVisitedPosts(Long userId, int page, int size);

    @Transactional
    PostDto.Detail addPairView(Long postId, Long userId, PostDto.OpinionItem opinionItem);

    @Transactional(readOnly = true)
    List<PostDto.QuickPoll> getRandomQuickPoll();

    @Transactional
    boolean bookmarkPost(Long postId, Long userId);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getBookmarkedPosts(Long userId, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getVotedPosts(Long userId, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getFairViewConfirmList(Long userId, Pageable pageable);

    @Transactional
    boolean confirmFairViewPost(Long userId, Long postId);

    boolean confirmFairView(Long userId, Long fairViewId);

    @Transactional(readOnly = true)
    List<PostDto.ListItem> searchPostsByKeyword(Long userId, String keyword);
}
