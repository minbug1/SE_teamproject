package its.model;

/*
 * DTO model for developer recommendation
 * 
 * @author hanung
 */

public class DeveloperRecommendation implements Comparable<DeveloperRecommendation> {

    private final User developer;
    private final double score;
    private final int matchedIssueCount;
    private final int resolvedIssueCount;

    public DeveloperRecommendation(
            User developer,
            double score,
            int matchedIssueCount,
            int totalSolvedIssueCount
    ) {
        this.developer = developer;
        this.score = score;
        this.matchedIssueCount = matchedIssueCount;
        this.resolvedIssueCount = totalSolvedIssueCount;
    }

    public User getDeveloper() { return developer; }
    public double getScore() { return score; }
    public int getMatchedIssueCount() { return matchedIssueCount; }
    public int getTotalSolvedIssueCount() { return resolvedIssueCount; }

    @Override
    public int compareTo(DeveloperRecommendation other) {
        // similarity score
        int scoreCompare = Double.compare(other.getScore(), this.score);
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // assigned issues
        int matchedCompare = Integer.compare(other.getMatchedIssueCount(), this.matchedIssueCount);
        if (matchedCompare != 0) {
            return matchedCompare;
        }

        // fixed issues
        int solvedCompare = Integer.compare(other.getTotalSolvedIssueCount(), this.resolvedIssueCount);
        if (solvedCompare != 0) {
            return solvedCompare;
        }

        // login id
        return this.developer.getLoginId().compareTo(other.getDeveloper().getLoginId());
    }

    @Override
    public String toString() {
        return "DeveloperRecommendation{" +
                "developer=" + developer.getLoginId() +
                ", score=" + score +
                ", matchedIssueCount=" + matchedIssueCount +
                ", totalSolvedIssueCount=" + resolvedIssueCount +
                '}';
    }
}