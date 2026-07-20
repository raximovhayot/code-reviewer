package uz.uzinfoweb.code_reviewer.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

@Service
public class CodeReviewService {

    private final ChatClient chatClient;

    public CodeReviewService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateSummary(String diff) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                "You are an AI code reviewer. Your task is to provide a high-level summary of the following git diff. " +
                "Do not review the code line by line, just summarize what features or bug fixes are introduced."
        );
        
        return chatClient.prompt()
                .system(systemPromptTemplate.render())
                .user("Here is the Git Diff:\n\n" + diff)
                .call()
                .content();
    }

    public List<ReviewComment> reviewCode(String diff, String rules) {
        var outputConverter = new BeanOutputConverter<>(new ParameterizedTypeReference<List<ReviewComment>>() {});

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                "You are an AI code reviewer for a GitLab merge request. " +
                "Your job is to read the git diff and provide constructive feedback based on the company rules.\n" +
                "Company Rules:\n{rules}\n\n" +
                "Provide feedback only for lines with issues. " +
                "For each issue, specify the file path, the exact line number where the issue occurs, a comment, and the rule violated.\n" +
                "Respond ONLY with a valid JSON array of objects conforming to the format requested. Do not include any other text.\n" +
                "{format}"
        );
        
        String systemPrompt = systemPromptTemplate.render(Map.of(
            "rules", rules,
            "format", outputConverter.getFormat()
        ));
        
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user("Here is the Git Diff:\n\n" + diff)
                .call()
                .content();

        return outputConverter.convert(response);
    }

    public String chat(String context, String userMessage) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                "You are an AI code reviewer for a GitLab merge request interacting in a chat discussion thread. " +
                "You are asked a question or asked to clarify a previous comment.\n" +
                "Here is the discussion context so far:\n{context}\n\n" +
                "Please respond to the user's latest message in a helpful and concise manner."
        );
        
        return chatClient.prompt()
                .system(systemPromptTemplate.render(Map.of("context", context)))
                .user(userMessage)
                .call()
                .content();
    }
}
