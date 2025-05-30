package com.project.bookstore.book;

import com.project.bookstore.cart.PricingService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenLibraryService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String OPEN_LIBRARY_API = "https://openlibrary.org/search.json?title=";
    private final PricingService pricingService;

    public OpenLibraryService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public List<OpenLibraryBookDto> searchOpenLibraryBook(String query) {
        List<OpenLibraryBookDto> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = OPEN_LIBRARY_API + encodedQuery + "&limit=10&offset=10";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = new JSONObject(response.getBody());
                JSONArray docs = json.optJSONArray("docs");
                if (docs != null) {
                    for (int i = 0; i < docs.length(); i++) {
                        JSONObject doc = docs.getJSONObject(i);
                        String title = doc.optString("title", "No Title");

                        String author = "Unknown";
                        if (doc.has("author_name")) {
                            JSONArray authors = doc.getJSONArray("author_name");
                            author = getString(author, authors);
                        }
                        String openLibraryKey = doc.optString("key", "N/A");
                        Integer publicationYear = doc.has("first_publish_year") ? doc.optInt("first_publish_year") : null;
                        Integer pageCount = doc.has("number_of_pages_median") ? doc.optInt("number_of_pages_median") : null;
                        String genre = "General";
                        if (doc.has("subject")) {
                            JSONArray subjects = doc.getJSONArray("subject");
                            genre = getString(genre, subjects);
                        }
                        else if (doc.has("subject_facet")) {
                            JSONArray subjects = doc.getJSONArray("subject_facet");
                            genre = getString(genre, subjects);
                        }
                        else {
                            String detailUrl = "https://openlibrary.org" + openLibraryKey + ".json";
                            try {
                                ResponseEntity<String> detailResponse = restTemplate.getForEntity(detailUrl, String.class);
                                if (detailResponse.getStatusCode().is2xxSuccessful() && detailResponse.getBody() != null) {
                                    JSONObject detailJson = new JSONObject(detailResponse.getBody());
                                    JSONArray detailSubjects = detailJson.optJSONArray("subjects");
                                    if (detailSubjects != null && !detailSubjects.isEmpty()) {
                                        List<String> detailSubjectList = new ArrayList<>();
                                        for (int k = 0; k < detailSubjects.length(); k++) {
                                            detailSubjectList.add(detailSubjects.getString(k));
                                        }
                                        genre = String.join(", ", detailSubjectList);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        String isbn = "N/A";
                        if (doc.has("isbn")) {
                            JSONArray isbnArray = doc.getJSONArray("isbn");
                            if (!isbnArray.isEmpty()) {
                                isbn = isbnArray.optString(0);
                            }
                        }

                        // cover image logic
                        String imageUrl = null;
                        int coverId = doc.optInt("cover_i", -1);
                        if (coverId > 0) {
                            imageUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
                        } else if (doc.has("isbn") && !doc.getJSONArray("isbn").isEmpty()) {
                            String isbn0 = doc.getJSONArray("isbn").optString(0);
                            imageUrl = "https://covers.openlibrary.org/b/isbn/" + isbn0 + "-L.jpg";
                        } else if (doc.has("edition_key") && !doc.getJSONArray("edition_key").isEmpty()) {
                            String olid = doc.getJSONArray("edition_key").optString(0);
                            imageUrl = "https://covers.openlibrary.org/b/olid/" + olid + "-L.jpg";
                        }
                        if (imageUrl == null) {
                            imageUrl = "/placeholder.jpg";
                        }

                        BigDecimal price = pricingService.determinePrice(publicationYear, pageCount, genre);

                        results.add(new OpenLibraryBookDto(openLibraryKey, title, author, isbn, publicationYear, pageCount, genre, price, imageUrl));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private String getString(String genre, JSONArray subjects) {
        if (!subjects.isEmpty()) {
            List<String> subjectList = new ArrayList<>();
            for (int k = 0; k < subjects.length(); k++) {
                subjectList.add(subjects.getString(k));
            }
            genre = String.join(", ", subjectList);
        }
        return genre;
    }

    public String findCoverUrlById(String olidOrIsbn) {
        if (olidOrIsbn == null) return null;

        // if it looks like an OLID (e.g. OL123M), use /b/olid/
        if (olidOrIsbn.toUpperCase().startsWith("OL")) {
            return "https://covers.openlibrary.org/b/olid/" +
                    olidOrIsbn +
                    "-L.jpg";
        }
        // otherwise treat it as ISBN
        return "https://covers.openlibrary.org/b/isbn/" +
                olidOrIsbn +
                "-L.jpg";
    }
}