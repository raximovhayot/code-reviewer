package uz.uzinfoweb.code_reviewer.gitlab;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.uzinfoweb.code_reviewer.core.CodeReviewService;
import uz.uzinfoweb.code_reviewer.rules.RuleService;

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
                String rules = ruleService.loadRules();
                String reviewComment = codeReviewService.reviewCode(diff, rules);

                postComment(projectId, mergeRequestIid, reviewComment);
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
}
