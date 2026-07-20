package uz.uzinfoweb.code_reviewer.gitlab;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Position;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.uzinfoweb.code_reviewer.core.CodeReviewService;
import uz.uzinfoweb.code_reviewer.core.ReviewComment;
import uz.uzinfoweb.code_reviewer.rules.RuleService;

import java.util.List;

@Service
public class GitLabEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(GitLabEventProcessor.class);

    private final CodeReviewService codeReviewService;
    private final GitLabApi gitLabApi;
    private final RuleService ruleService;

    public GitLabEventProcessor(
            CodeReviewService codeReviewService,
            RuleService ruleService,
            @Value("${gitlab.url}") String gitlabUrl,
            @Value("${gitlab.personal-access-token}") String gitlabToken) {
        
        this.codeReviewService = codeReviewService;
        this.ruleService = ruleService;
        this.gitLabApi = new GitLabApi(gitlabUrl, gitlabToken);
    }

    @Async
    public void handleMergeRequestEvent(MergeRequestEvent event) {
        try {
            String state = event.getObjectAttributes().getState();
            String action = event.getObjectAttributes().getAction();
            
            if ("opened".equals(state) || "updated".equals(action)) {
                Long projectId = event.getProject().getId();
                Long mergeRequestIid = event.getObjectAttributes().getIid();

                log.info("Processing MR !{} for project {}", mergeRequestIid, projectId);

                String diff = getMrDiff(projectId, mergeRequestIid);
                
                // 1. Automated PR/MR Summarization
                String summary = codeReviewService.generateSummary(diff);
                postComment(projectId, mergeRequestIid, "### AI Code Review Summary\n\n" + summary);
                
                // 2. Context-Aware Code Review (Line-by-Line)
                String rules = ruleService.loadRules();
                List<ReviewComment> reviewComments = codeReviewService.reviewCode(diff, rules);

                if (reviewComments != null) {
                    for (ReviewComment rc : reviewComments) {
                        postDiffDiscussion(projectId, mergeRequestIid, rc);
                    }
                }
            }
        } catch (GitLabApiException e) {
            log.error("GitLab API error while processing MR event", e);
        } catch (Exception e) {
            log.error("Unexpected error while processing MR event", e);
        }
    }

    private String getMrDiff(Long projectId, Long mergeRequestIid) throws GitLabApiException {
        StringBuilder diffBuilder = new StringBuilder();
        var mrChanges = gitLabApi.getMergeRequestApi().getMergeRequestChanges(projectId, mergeRequestIid);
        for (var change : mrChanges.getChanges()) {
            diffBuilder.append("File: ").append(change.getNewPath()).append("\n");
            diffBuilder.append(change.getDiff()).append("\n\n");
        }
        return diffBuilder.toString();
    }

    private void postComment(Long projectId, Long mergeRequestIid, String comment) throws GitLabApiException {
        gitLabApi.getNotesApi().createMergeRequestNote(projectId, mergeRequestIid, comment);
        log.info("Posted review comment on MR !{}", mergeRequestIid);
    }

    private void postDiffDiscussion(Long projectId, Long mergeRequestIid, ReviewComment rc) {
        try {
            MergeRequest mr = gitLabApi.getMergeRequestApi().getMergeRequest(projectId, mergeRequestIid);
            
            Position position = new Position();
            position.setPositionType(Position.PositionType.TEXT);
            position.setBaseSha(mr.getDiffRefs().getBaseSha());
            position.setStartSha(mr.getDiffRefs().getStartSha());
            position.setHeadSha(mr.getDiffRefs().getHeadSha());
            position.setNewPath(rc.filePath());
            position.setNewLine(rc.lineNumber());

            String body = rc.comment() + "\n\n**Rule Violated:** " + rc.ruleViolated();
            gitLabApi.getDiscussionsApi().createMergeRequestDiscussion(
                    projectId, mergeRequestIid, body, null, null, position);
                    
            log.info("Posted diff discussion on MR !{} for file {} at line {}", mergeRequestIid, rc.filePath(), rc.lineNumber());
        } catch (Exception e) {
            log.error("Failed to post diff discussion for {} at line {}", rc.filePath(), rc.lineNumber(), e);
        }
    }
}
