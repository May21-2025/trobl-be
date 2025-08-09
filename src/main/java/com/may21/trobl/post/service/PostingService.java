package com.may21.trobl.post.service;

import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.report.ReportDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PostingService {

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getPostsList(Pageable pageable, Long userId);

    @Transactional(readOnly = true)
    List<PostDto.Card> getTop10Views(String type, Long userId);

    @Transactional
    PostDto.Detail getPostDetail(Long postId, Long userId);

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

    @Transactional(readOnly = true)
    Page<PostDto.MyListItem> getMyPosts(Long userId, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getLikedPosts(Long userId, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getVisitedPosts(Long userId, int page, int size);

    @Transactional
    PostDto.FairViewItem setFairView(Long fairViewId, Long userId, PostDto.FairViewRequest fairView);

    @Transactional(readOnly = true)
    List<PostDto.QuickPoll> getRandomQuickPoll(Long userId);

    @Transactional
    boolean bookmarkPost(Long postId, Long userId);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getBookmarkedPosts(Long userId, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> getVotedPosts(Long userId, int page, int size);

    @Transactional(readOnly = true)
    Page<PostDto.RequestedItem> getFairViewConfirmList(Long userId, Pageable pageable);

    @Transactional
    boolean confirmFairViewPost(Long userId, Long postId);

    @Transactional
    boolean confirmFairView(Long userId, Long fairViewId);

    @Transactional(readOnly = true)
    Page<PostDto.ListItem> searchPostsByKeyword(Long userId, String keyword, Pageable pageable);

    @Transactional
    void setNickname(Long id, String nickname);

    @CacheEvict(value = "topPosts", allEntries = true)
    void evictAllTopPosts();

    @Transactional
    boolean reportPost(Long userId, Long postId, ReportDto.Request reportRequest);

    @Transactional(readOnly = true)
    Page<PostDto.RequestedListItem> getFairViewRequestedList(Long id, int page, int size);

    @Transactional(readOnly = true)
    List<PostDto.ListItem> getAllReportedPosts();

    @Transactional
    boolean unblockPost(Long postId);

    @Transactional
    boolean deletePostByAdmin(Long postId);

    @Transactional(readOnly = true)
    Page<PostDto.HotFairView> getFairViewList(Long userId, int page, int size);

    @Transactional
    PostDto.ListItem createVirtualPost(AdminDto.VirtualPostRequest createRequest);

    @Transactional
    PostDto.ListItem updateVirtualPost(Long postId, PostDto.Request updateRequest);

    void evictTopPostsCache(Long userId);

    @Transactional
    PostDto.ListItem  createFairViewByAdmin(AdminDto.FairViewPostRequest request);

    @Transactional(readOnly = true)
    List<PostDto.QuickPoll> getQuickPolls(Long userId);

    @Transactional
    PostDto.FairViewItem updateVirtualFairView(Long fairViewId, PostDto.FairViewRequest request);

    @Transactional
    PostDto.PollDto updatePoll(Long pollId, PostDto.PollRequest request);

    @Transactional(readOnly = true)
    Page<PostDto.AdminListItem> getAdminAllPosts(int size, int page, String sortType, boolean asc,
            List<String> postType);

    @Transactional(readOnly = true)
    AdminDto.PostInfo getAdminPostInfo(Long postId);
}
