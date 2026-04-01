package com.alphnology;

import com.alphnology.services.GitHubService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ApplicationTests {

    // github-api:1.327 is incompatible with Jackson 2.20+ (SNAKE_CASE field removed);
    // mock it until the library is upgraded to a compatible version.
    @MockitoBean
    private GitHubService gitHubService;

    @Test
    void contextLoads() {
    }
}
