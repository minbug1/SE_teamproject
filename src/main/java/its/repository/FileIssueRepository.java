package its.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import its.model.Comment;
import its.model.Issue;
import its.model.Priority;
import its.model.Status;
import its.model.User;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileIssueRepository implements IssueRepository {

    private static final String DEFAULT_FILE_PATH = "data/issues.json";

    private final File file;
    private final Gson gson;
    private final UserRepository userRepository;

    public FileIssueRepository() {
        this(DEFAULT_FILE_PATH, null);
    }

    public FileIssueRepository(UserRepository userRepository) {
        this(DEFAULT_FILE_PATH, userRepository);
    }

    public FileIssueRepository(String filePath, UserRepository userRepository) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be empty.");
        }

        this.file = new File(filePath);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.userRepository = userRepository;

        initializeFile();
    }

    private void initializeFile() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            if (file.length() == 0) {
                writeAll(new ArrayList<>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize issue data file.", e);
        }
    }

    @Override
    public void save(Issue issue) {
        validateIssue(issue);

        List<Issue> issues = findAll();

        for (Issue existing : issues) {
            if (isSameIssue(existing, issue)) {
                throw new IllegalArgumentException("Issue ID already exists in this project.");
            }
        }

        issues.add(issue);
        writeAll(issues);
    }

    @Override
    public Issue findById(int issueId) { 
        if (issueId <= 0) return null;

        for (Issue issue : findAll()) {
            if (issue.getIssueId() != null && issue.getIssueId().intValue() == issueId) return issue;
        }
        return null;
    }

    @Override
    public Issue findByProjectIdAndIssueId(int projectId, int issueId) {
        if (projectId <= 0 || issueId <= 0) return null;

        for (Issue issue : findAll()) {
            if ((issue.getProjectId() == projectId || issue.getProjectId() == 0)
                    && issue.getIssueId() != null
                    && issue.getIssueId().intValue() == issueId) {
                return issue;
            }
        }
        return null;
    }

    @Override
    public List<Issue> findAll() {
        List<IssueRecord> records = readAllRecords();
        List<Issue> issues = new ArrayList<>();

        for (IssueRecord record : records) {
            issues.add(record.toIssue(userRepository));
        }
        return issues;
    }

    @Override
    public List<Issue> findByProjectId(int projectId) {
        List<Issue> result = new ArrayList<>();
        if (projectId <= 0) {
            return result;
        }

        for (Issue issue : findAll()) {
            if (issue.getProjectId() == projectId) {
                result.add(issue);
            }
        }
        return result;
    }

    @Override
    public void update(Issue issue) {
        validateIssue(issue);

        List<Issue> issues = findAll();
        boolean updated = false;

        for (int i = 0; i < issues.size(); i++) {
            if (isSameIssue(issues.get(i), issue)) {
                issues.set(i, issue);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new IllegalArgumentException("Issue does not exist.");
        }

        writeAll(issues);
    }

    @Override
    public void delete(int issueId) {  // long → int
        if (issueId <= 0) throw new IllegalArgumentException("Issue ID must be positive.");

        List<Issue> issues = findAll();
        boolean deleted = issues.removeIf(i -> i.getIssueId() == issueId);

        if (!deleted) throw new IllegalArgumentException("Issue does not exist.");

        writeAll(issues);
    }

    @Override
    public void delete(int projectId, int issueId) {
        if (projectId <= 0) throw new IllegalArgumentException("Project ID must be positive.");
        if (issueId <= 0) throw new IllegalArgumentException("Issue ID must be positive.");

        List<Issue> issues = findAll();
        boolean deleted = issues.removeIf(i -> (i.getProjectId() == projectId || i.getProjectId() == 0)
                && i.getIssueId() != null
                && i.getIssueId().intValue() == issueId);

        if (!deleted) throw new IllegalArgumentException("Issue does not exist.");

        writeAll(issues);
    }

    @Override
    public int generateIssueId() {  
        int maxId = 0;
        for (Issue issue : findAll()) {
            if (issue.getIssueId() != null && issue.getIssueId() > maxId) {
                maxId = issue.getIssueId().intValue();
            }
        }
        return maxId + 1;
    }

    @Override
    public int generateIssueId(int projectId) {
        int maxId = 0;
        for (Issue issue : findAll()) {
            if (issue.getProjectId() == projectId
                    && issue.getIssueId() != null
                    && issue.getIssueId() > maxId) {
                maxId = issue.getIssueId().intValue();
            }
        }
        return maxId + 1;
    }

    private boolean isSameIssue(Issue left, Issue right) {
        return left.getIssueId() != null
                && right.getIssueId() != null
                && left.getIssueId().intValue() == right.getIssueId().intValue()
                && (left.getProjectId() == right.getProjectId()
                        || left.getProjectId() == 0
                        || right.getProjectId() == 0);
    }

    private List<IssueRecord> readAllRecords() {
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<IssueRecord>>() {}.getType();
            List<IssueRecord> records = gson.fromJson(reader, listType);
            return records != null ? records : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("Failed to parse issue data file. The JSON format is invalid.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read issue data file.", e);
        }
    }

    private void writeAll(List<Issue> issues) {
        List<IssueRecord> records = new ArrayList<>();
        for (Issue issue : issues) {
            records.add(IssueRecord.fromIssue(issue));
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            gson.toJson(records, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write issue data file.", e);
        }
    }

    private void validateIssue(Issue issue) {
        if (issue == null) throw new IllegalArgumentException("Issue must not be null.");
        if (issue.getIssueId() <= 0)  // int라서 null 체크 제거
            throw new IllegalArgumentException("Issue ID must be positive.");
    }

    private static class IssueRecord {
        private long issueId;
        private int projectId;
        private String title;
        private String description;
        private String reportedDate;
        private Long reporterId;
        private Long assigneeId;
        private Long fixerId;
        private String priority;
        private String status;
        private List<CommentRecord> comments;

        private Issue toIssue(UserRepository userRepo) {
            User reporter = (userRepo != null && reporterId != null)
                    ? userRepo.findByUserId(reporterId) : null;

            LocalDateTime date = (reportedDate != null)
                    ? LocalDateTime.parse(reportedDate) : LocalDateTime.now();

            Issue issue = new Issue(issueId, title, description, reporter, date);
            issue.setProjectId(projectId);

            if (priority != null) issue.setPriority(Priority.valueOf(priority));
            if (status != null)   issue.setStatus(Status.valueOf(status));

            if (userRepo != null) {
                if (assigneeId != null) issue.setAssignee(userRepo.findByUserId(assigneeId));
                if (fixerId != null)    issue.setFixer(userRepo.findByUserId(fixerId));
            }

            if (comments != null) {
                for (CommentRecord cr : comments) {
                    issue.addComment(cr.toComment(userRepo));
                }
            }

            return issue;
        }

        private static IssueRecord fromIssue(Issue issue) {
            IssueRecord r = new IssueRecord();
            r.issueId     = issue.getIssueId();
            r.projectId   = issue.getProjectId();
            r.title       = issue.getTitle();
            r.description = issue.getDescription();
            r.reportedDate = issue.getReportedDate() != null
                    ? issue.getReportedDate().toString() : null;
            r.reporterId  = issue.getReporter()  != null ? issue.getReporter().getUserId()  : null;
            r.assigneeId  = issue.getAssignee()  != null ? issue.getAssignee().getUserId()  : null;
            r.fixerId     = issue.getFixer()     != null ? issue.getFixer().getUserId()     : null;
            r.priority    = issue.getPriority()  != null ? issue.getPriority().name()  : null;
            r.status      = issue.getStatus()    != null ? issue.getStatus().name()    : null;

            r.comments = new ArrayList<>();
            for (Comment c : issue.getComments()) {
                r.comments.add(CommentRecord.fromComment(c));
            }
            return r;
        }
    }

    private static class CommentRecord {
        private int commentId;
        private String content;
        private Long authorId;
        private String writtenDate;

        private Comment toComment(UserRepository userRepo) {
            User author = (userRepo != null && authorId != null)
                    ? userRepo.findByUserId(authorId) : null;
            LocalDateTime date = (writtenDate != null)
                    ? LocalDateTime.parse(writtenDate) : LocalDateTime.now();
            return new Comment(commentId, content, author, date);
        }

        private static CommentRecord fromComment(Comment comment) {
            CommentRecord r = new CommentRecord();
            r.commentId   = comment.getCommentId();
            r.content     = comment.getContent();
            r.authorId    = comment.getAuthor() != null ? comment.getAuthor().getUserId() : null;
            r.writtenDate = comment.getWrittenDate() != null
                    ? comment.getWrittenDate().toString() : null;
            return r;
        }
    }
}
