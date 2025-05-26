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
    boolean votePoll(Long pollOptionId, Long id);

    @Transactional
    boolean sharePost(Long postId, Long id);
  @Transactional
    boolean viewPost(Long postId, Long id);

    @Transactional
    boolean likePost(Long postId, Long id);

    @Transactional
    boolean deletePost(Long id, Long postId);

    Page<PostDto.ListItem> getMyPosts(Long id, int page, int size);

    Page<PostDto.ListItem> getLikedPosts(Long id, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getVisitedPosts(Long id, int page, int size);

    @Transactional
    PostDto.Detail addPairView(Long postId, Long id, PostDto.OpinionItem opinionItem);

    @Transactional(readOnly = true)
    List<PostDto.QuickPoll> getRandomQuickPoll();

    @Transactional
    boolean bookmarkPost(Long postId, Long id);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getBookmarkedPosts(Long id, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getVotedPosts(Long id, int page, int size);
}
