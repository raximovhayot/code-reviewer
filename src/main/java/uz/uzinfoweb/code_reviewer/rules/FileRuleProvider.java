package uz.uzinfoweb.code_reviewer.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class FileRuleProvider implements RuleProvider {

    private static final Logger log = LoggerFactory.getLogger(FileRuleProvider.class);

    private final String rulesFilePath;

    public FileRuleProvider(@Value("${rules.file.path:rules.md}") String rulesFilePath) {
        this.rulesFilePath = rulesFilePath;
    }

    @Override
    public String getRules() {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(rulesFilePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Could not load rules from {}, falling back to default.", rulesFilePath, e);
            return "Review the code for clean code practices and potential bugs.";
        }
    }
}
