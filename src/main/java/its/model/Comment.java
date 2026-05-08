package its.model;

import java.time.LocalDateTime;

public class Comment {

    private String commentId;
    private String content;
    private User author;
    private LocalDateTime writtenDate;

    public Comment(String commentId, String content, User author, LocalDateTime writtenDate) {
        this.commentId = commentId;
        this.content = content;
        this.author = author;
        this.writtenDate = writtenDate;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getContent() {
        return content;
    }

    public User getAuthor() {
        return author;
    }

    public LocalDateTime getWrittenDate() {
        return writtenDate;
    }
}
