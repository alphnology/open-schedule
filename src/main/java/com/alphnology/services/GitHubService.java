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

    private final GitHub github;
    private final String repositoryName;

    /**
     * Constructor that initializes the connection to the GitHub API.
     * It uses the token and repository name from application.properties.
     *
     * @param token          The personal access token for GitHub.
     * @param repositoryName The repository name in "owner/repo" format.
     * @throws IOException If the connection to GitHub fails.
     */
    public GitHubService(@Value("${github.api.token}") String token,
                         @Value("${github.repository.name}") String repositoryName) throws IOException {
        this.github = new GitHubBuilder().withOAuthToken(token).build();
        this.repositoryName = repositoryName;

        log.info("GitHubService initialized for repository: {}", repositoryName);

    }

    /**
     * Creates a new issue in the configured repository.
     *
     * @param title The title of the issue.
     * @param body  The description of the issue.
     * @throws IOException If creating the issue fails.
     */
    public void createIssue(String title, String body) throws IOException {
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
