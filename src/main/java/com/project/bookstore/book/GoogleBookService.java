package com.project.bookstore.book;

import com.mashape.unirest.http.Unirest;
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
    private static final String GOOGLE_VOLUME_URL = "https://www.googleapis.com/books/v1/volumes/";
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

                // ISBN extraction
                String isbn = "N/A";
                JSONArray ids = volumeInfo.optJSONArray("industryIdentifiers");
                if (ids != null) {
                    for (int j = 0; j < ids.length(); j++) {
                        JSONObject idObj = ids.getJSONObject(j);
                        if ("ISBN_13".equals(idObj.optString("type"))) {
                            isbn = idObj.optString("identifier");
                            break;
                        }
                    }
                }

                // imageLinks
                String imageUrl = null;
                JSONObject imgLinks = volumeInfo.optJSONObject("imageLinks");
                if (imgLinks != null) {
                    imageUrl = imgLinks.optString("thumbnail", null);
                    if (imageUrl == null)
                        imageUrl = imgLinks.optString("smallThumbnail", null);
                }
                if (imageUrl == null) {
                    // fallback placeholder
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

    public String findCoverUrlById(String volumeId) {
        try {
            // call Google Books volumes endpoint
            String json = Unirest
                    .get(GOOGLE_VOLUME_URL + volumeId)
                    .asString()
                    .getBody();

            JSONObject root = new JSONObject(json);
            JSONObject info = root.optJSONObject("volumeInfo");
            if (info != null) {
                JSONObject imgs = info.optJSONObject("imageLinks");
                if (imgs != null) {
                    // try extraLarge → large → thumbnail
                    String url = imgs.optString("extraLarge", null);
                    if (url == null) url = imgs.optString("large", null);
                    if (url == null) url = imgs.optString("thumbnail", null);
                    if (url != null) {
                        // ensure HTTPS
                        return url.replaceFirst("^http:", "https:");
                    }
                }
            }
        } catch (Exception e) {
            // silently swallow; we'll fall back elsewhere
        }
        return null;
    }
}
