package com.project.bookstore.book;

import com.project.bookstore.cart.PricingService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service
public class GoogleBookService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes?q=";
    private final PricingService pricingService;

    public GoogleBookService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public List<GoogleBookDto> searchGoogleBooks(String query) {
        try {
            String modifiedQuery = "intitle:" + query;
            String requestUrl = GOOGLE_BOOKS_API + modifiedQuery.replace(" ", "+") + "&maxResults=10";

            //Call the API
            ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            JSONObject json = new JSONObject(response.getBody());
            JSONArray items = json.optJSONArray("items");
            if (items == null) {
                return List.of();
            }

            List<GoogleBookDto> results = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject volumeInfo = item.optJSONObject("volumeInfo");
                if (volumeInfo == null) continue;

                String googleBookId = item.optString("id", "N/A");
                String title = volumeInfo.optString("title", "N/A");

                JSONArray authors = volumeInfo.optJSONArray("authors");
                String author = (authors != null && !authors.isEmpty()) ? authors.getString(0) : "Unknown";


                Integer publicationYear = volumeInfo.has("publishedDate") ? pricingService.extractYear(volumeInfo.optString("publishedDate")) : null;
                Integer pageCount = volumeInfo.has("pageCount") ? volumeInfo.optInt("pageCount") : null;
                //String genre = volumeInfo.has("categories") ? volumeInfo.optJSONArray("categories").optString(0, "General") : "General";

                JSONArray categoriesArray = volumeInfo.optJSONArray("categories");
                List<String> categories = new ArrayList<>();
                if (categoriesArray != null) {
                    for (int j = 0; j < categoriesArray.length(); j++) {
                        categories.add(categoriesArray.getString(j));
                    }
                }
                String genre = categories.isEmpty() ? "General" : String.join(", ", categories);

                BigDecimal price = pricingService.determinePrice(publicationYear, pageCount, genre);

                String isbn = "N/A";
                JSONArray industryIds = volumeInfo.optJSONArray("industryIdentifiers");
                if (industryIds != null) {
                    for (int j = 0; j < industryIds.length(); j++) {
                        JSONObject idObj = industryIds.getJSONObject(j);
                        if ("ISBN_13".equals(idObj.optString("type"))) {
                            isbn = idObj.optString("identifier", isbn);
                            break;
                        }
                    }
                }
                if (volumeInfo.has("industryIdentifiers")) {
                    JSONArray identifiers = volumeInfo.getJSONArray("industryIdentifiers");
                    for (int j = 0; j < identifiers.length(); j++) {
                        JSONObject identifier = identifiers.getJSONObject(j);
                        String type = identifier.optString("type");
                        String value = identifier.optString("identifier");
                        if ("ISBN_13".equals(type)) {
                            isbn = value;
                            break;
                        }
                        else if ("ISBN_10".equals(type)){
                            isbn = value;
                        }
                    }
                }
                String imageUrl = null;
                JSONObject imageLinks = volumeInfo.optJSONObject("imageLinks");
                if (imageLinks != null) {
                    imageUrl = imageLinks.optString("thumbnail", imageLinks.optString("smallThumbnail", null));
                }
                if (imageUrl == null) {
                    imageUrl = "/placeholder.jpg";
                }
                results.add(new GoogleBookDto(googleBookId, title, author, isbn, publicationYear, pageCount, genre, price, imageUrl));
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
