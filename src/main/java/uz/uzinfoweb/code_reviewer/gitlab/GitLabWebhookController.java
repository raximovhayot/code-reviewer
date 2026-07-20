package uz.uzinfoweb.code_reviewer.gitlab;

import org.gitlab4j.api.webhook.MergeRequestEvent;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/gitlab")
public class GitLabWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitLabWebhookController.class);

    private final GitLabEventProcessor eventProcessor;
    private final JsonMapper jsonMapper = new JsonMapper();

    public GitLabWebhookController(GitLabEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @PostMapping
    public void handleWebhook(@RequestHeader(value = "X-Gitlab-Event", required = false) String eventType, @RequestBody String payload) {
        try {
            if ("Merge Request Hook".equals(eventType)) {
                MergeRequestEvent event = jsonMapper.readValue(payload, MergeRequestEvent.class);
                eventProcessor.handleMergeRequestEvent(event);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse GitLab webhook payload as JSON", e);
        } catch (Exception e) {
            log.error("Unexpected error handling GitLab webhook", e);
        }
    }
}
