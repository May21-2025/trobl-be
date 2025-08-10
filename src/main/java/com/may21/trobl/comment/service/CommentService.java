package com.may21.trobl.comment.service;

import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.report.ReportDto;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface CommentService {

    @Transactional(readOnly = true)
    List<CommentDto.Response> getComments(Long postId, Long userId);

    @Transactional
    CommentDto.Response createComment(Long postId, CommentDto.Request request, Long id);

    @Transactional
    CommentDto.Response updateComment(Long userId, CommentDto.Request request, Long commentId);

    @Transactional
    CommentDto.Response likeComment(Long commentId, Long id);

    @Transactional
    boolean deleteComment(Long id, Long commentId);

    @Transactional(readOnly = true)
    Page<CommentDto.MyComments> getMyComments(Long id, int page, int size);

    @Transactional(readOnly = true)
    Map<Long, Integer> getPostCommentMap(List<Posting> posts);

    @Transactional(readOnly = true)
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Transactional
    boolean reportComment(Long id, Long commentId, ReportDto.Request reportRequest);

    @Transactional
    CommentDto.Response createVirtualComment(AdminDto.VirtualCommentRequest createRequest);

    @Transactional
    boolean deleteVirtualComments(Long commentId);

    @Transactional(readOnly = true)
    List<AdminDto.CommentInfo> getAdminCommentInfo(Long postId);
    @Transactional
    CommentDto.Response updateVirtualComments(Long commentId, AdminDto.VirtualCommentRequest createRequest);

    int getCommentCountByPostId(Long postId);
}
