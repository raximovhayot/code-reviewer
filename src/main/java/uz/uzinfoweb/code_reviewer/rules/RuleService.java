package uz.uzinfoweb.code_reviewer.rules;

import org.springframework.stereotype.Service;

@Service
public class RuleService {

    private final RuleProvider ruleProvider;

    public RuleService(RuleProvider ruleProvider) {
        this.ruleProvider = ruleProvider;
    }

    public String loadRules() {
        return ruleProvider.getRules();
    }
}
