package uz.uzinfoweb.code_reviewer.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import java.util.Map;

@Service
public class CodeReviewService {

    private final ChatClient chatClient;

    public CodeReviewService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String reviewCode(String diff, String rules) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                "You are an AI code reviewer for a GitLab merge request. " +
                "Your job is to read the git diff and provide constructive feedback based on the company rules.\n" +
                "Company Rules:\n{rules}\n\n" +
                "If the code violates a rule, provide a comment. If it's good, approve it.");
        
        String systemPrompt = systemPromptTemplate.render(Map.of("rules", rules));
        
        return chatClient.prompt()
                .system(systemPrompt)
                .user("Here is the Git Diff:\n\n" + diff)
                .call()
                .content();
    }
}
