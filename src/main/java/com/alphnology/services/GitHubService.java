package com.alphnology.services;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author me@fredpena.dev
 * @created 11/07/2025  - 18:20
 */

@Slf4j
@Service
public class GitHubService {

    private GitHub github;
    private final String repositoryName;

    public GitHubService(@Value("${github.api.token}") String token,
                         @Value("${github.repository.name}") String repositoryName) {
        this.repositoryName = repositoryName;
        try {
            this.github = new GitHubBuilder().withOAuthToken(token).build();
            log.info("GitHubService initialized for repository: {}", repositoryName);
        } catch (LinkageError e) {
            // github-api:1.327 references PropertyNamingStrategy.SNAKE_CASE which was
            // removed in Jackson 2.16+. Bug reporting is disabled until the library is upgraded.
            log.warn("GitHub API library incompatible with current Jackson version — bug reporting disabled: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Could not initialize GitHub service — bug reporting disabled: {}", e.getMessage());
        }
    }

    public void createIssue(String title, String body) throws IOException {
        if (github == null) {
            log.warn("GitHub service unavailable, skipping issue creation for: {}", title);
            return;
        }
        String finalBody = body + "\n\n---\n*This issue was submitted automatically from the Open Schedule application.*";
        log.info("Attempting to create GitHub issue with title: '{}'", title);
        GHRepository repository = github.getRepository(repositoryName);
        repository.createIssue(title)
                .body(finalBody)
                .label("bug")
                .label("from-app")
                .create();
        log.info("Successfully created GitHub issue in repository '{}'.", repositoryName);
    }
}
