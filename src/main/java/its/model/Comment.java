package its.model;

import java.time.LocalDateTime;

public class Comment {

    private int commentId;
    private String content;
    private User author;
    private CommentStatus commentStatus = CommentStatus.GENERAL;
    private LocalDateTime writtenDate;

    public Comment(int commentId, String content, User author, LocalDateTime writtenDate) {
        this.commentId = commentId;
        this.content = content;
        this.author = author;
        this.writtenDate = writtenDate;
    }

    public Comment(int commentId, String content, User author,
                LocalDateTime writtenDate, CommentStatus commentStatus) {
        this(commentId, content, author, writtenDate);
        setCommentStatus(commentStatus);
    }

    public int getCommentId() {
        return commentId;
    }

    public String getContent() {
        return content;
    }

    public User getAuthor() {
        return author;
    }

    public CommentStatus getCommentStatus() {
        return commentStatus;
    }

    public LocalDateTime getWrittenDate() {
        return writtenDate;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void setCommentStatus(CommentStatus commentStatus) {
        if (commentStatus == null) {
            throw new IllegalArgumentException("Comment Status must not be null.");
        }
        this.commentStatus = commentStatus;
    }

    public void setWrittenDate(LocalDateTime writtenDate) {
        this.writtenDate = writtenDate;
    }
}
