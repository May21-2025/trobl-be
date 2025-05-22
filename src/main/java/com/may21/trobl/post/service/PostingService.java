package com.may21.trobl.post.service;

import com.may21.trobl.post.dto.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PostingService {

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getPostsList(Pageable pageable);

    @Transactional(readOnly = true)
    List<PostDto.View> getTop5Views();

    @Transactional(readOnly = true)
    List<PostDto.ListItem> getTop10Views(String type);

    @Transactional(readOnly = true)
    PostDto.Detail getPostDetail(Long postId);

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

    List<PostDto.ListItem> getMyPosts(Long id);

    List<PostDto.ListItem> getLikedPosts(Long id);

    List<PostDto.ListItem> getVisitedPosts(Long id);

    PostDto.Detail addPairView(Long postId, Long id, PostDto.OpinionItem opinionItem);

    @Transactional(readOnly = true)
    List<PostDto.View> getRandomQuickPoll();
}
