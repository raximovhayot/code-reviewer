package uz.uzinfoweb.code_reviewer.gitlab;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.webhook.NoteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uz.uzinfoweb.code_reviewer.core.CodeReviewService;

@Service
public class GitLabNoteProcessor {

    private static final Logger log = LoggerFactory.getLogger(GitLabNoteProcessor.class);

    private final CodeReviewService codeReviewService;
    private final GitLabApi gitLabApi;

    public GitLabNoteProcessor(
            CodeReviewService codeReviewService,
            @Value("${gitlab.url}") String gitlabUrl,
            @Value("${gitlab.personal-access-token}") String gitlabToken) {
        
        this.codeReviewService = codeReviewService;
        this.gitLabApi = new GitLabApi(gitlabUrl, gitlabToken);
    }

    @Async
    public void handleNoteEvent(NoteEvent event) {
        try {
            if (event.getObjectAttributes() == null || !"MergeRequest".equals(event.getObjectAttributes().getNoteableType())) {
                return; // We only care about MR notes
            }
            
            String noteText = event.getObjectAttributes().getNote();
            Long projectId = event.getProject().getId();
            Long mergeRequestIid = event.getMergeRequest().getIid();
            String discussionId = event.getObjectAttributes().getDiscussionId();

            if (noteText.contains("@bot") || isReplyToBot(projectId, mergeRequestIid, discussionId)) {
                log.info("Chatbot triggered on MR !{} by note {}", mergeRequestIid, event.getObjectAttributes().getId());
                
                String context = getDiscussionContext(projectId, mergeRequestIid, discussionId);
                String response = codeReviewService.chat(context, noteText);
                
                replyToDiscussion(projectId, mergeRequestIid, discussionId, response);
            }
        } catch (Exception e) {
            log.error("Error processing note event", e);
        }
    }

    private boolean isReplyToBot(Long projectId, Long mergeRequestIid, String discussionId) {
        try {
            var discussions = gitLabApi.getDiscussionsApi().getMergeRequestDiscussions(projectId, mergeRequestIid);
            for (var discussion : discussions) {
                if (discussion.getId().equals(discussionId) && discussion.getNotes() != null && !discussion.getNotes().isEmpty()) {
                    var firstNote = discussion.getNotes().get(0);
                    String firstNoteText = firstNote.getBody();
                    if (firstNoteText != null && (firstNoteText.contains("AI Code Review Summary") || firstNoteText.contains("**Rule Violated:**"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to check if note is reply to bot", e);
        }
        return false;
    }

    private String getDiscussionContext(Long projectId, Long mergeRequestIid, String discussionId) {
        try {
            var discussions = gitLabApi.getDiscussionsApi().getMergeRequestDiscussions(projectId, mergeRequestIid);
            StringBuilder context = new StringBuilder();
            for (var discussion : discussions) {
                if (discussion.getId().equals(discussionId) && discussion.getNotes() != null) {
                    for (var note : discussion.getNotes()) {
                        context.append(note.getAuthor().getUsername()).append(": ").append(note.getBody()).append("\n");
                    }
                    break;
                }
            }
            return context.toString();
        } catch (Exception e) {
            log.error("Failed to get discussion context", e);
            return "";
        }
    }

    private void replyToDiscussion(Long projectId, Long mergeRequestIid, String discussionId, String reply) throws GitLabApiException {
        // Fallback to top-level note if the specific threaded reply method is unavailable in this gitlab4j version.
        // In newer versions, use: gitLabApi.getDiscussionsApi().addMergeRequestDiscussionNote(...)
        gitLabApi.getNotesApi().createMergeRequestNote(projectId, mergeRequestIid, reply);
    }
}
