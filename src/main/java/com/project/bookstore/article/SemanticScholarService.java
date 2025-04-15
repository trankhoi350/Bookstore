package com.project.bookstore.article;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SemanticScholarService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1/paper/search?query=";

    public List<ArticleDto> searchSemanticScholar(String query) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String fields = "&fields=title,authors,abstract,year";
                String requestUrl = SEMANTIC_SCHOLAR_API + URLEncoder.encode(query, StandardCharsets.UTF_8) + fields + "&limit=10";

                ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);

                // Check for successful response
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    // Log the error or handle specific status codes
                    System.err.println("Unsuccessful response: " + response.getStatusCode());

                    // Wait before retrying
                    if (attempt < maxRetries) {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                        continue;
                    }
                    return List.of();
                }

                JSONObject json = new JSONObject(response.getBody());
                JSONArray data = json.optJSONArray("data");

                if (data == null) {
                    return List.of();
                }

                List<ArticleDto> result = new ArrayList<>();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String paperId = item.optString("paperId", "N/A");
                    String title = item.optString("title", "N/A");

                    JSONArray authorsList = item.optJSONArray("authors");
                    String authors = "Unknown";
                    if (authorsList != null && authorsList.length() > 0) {
                        List<String> authorNames = new ArrayList<>();
                        for (int j = 0; j < authorsList.length(); j++) {
                            JSONObject authorObj = authorsList.getJSONObject(j);
                            String authorName = authorObj.optString("name", "Anonymous");
                            authorNames.add(authorName);
                        }
                        authors = String.join(", ", authorNames);
                    }

                    result.add(new ArticleDto(paperId, title, authors));
                }

                return result;
            } catch (Exception e) {
                System.err.println("Error on attempt " + attempt + ": " + e.getMessage());

                // If it's the last attempt, return an empty list
                if (attempt == maxRetries) {
                    e.printStackTrace();
                    return List.of();
                }

                // Wait before retrying
                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
        }

        return List.of();
    }
}