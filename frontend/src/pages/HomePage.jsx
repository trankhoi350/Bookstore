import React, {useState, useContext, use, useEffect} from "react";
import { Search, Bell, ShoppingCart, Menu } from 'lucide-react';
import stringSimilarity from "string-similarity";
import {AuthContext} from "../context/AuthContext.jsx";
import { Link } from "react-router-dom";

const HomePage = () => {
    const { user } = useContext(AuthContext);
    const [searchQuery, setSearchQuery] = useState('');
    const [books, setBooks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const token     = localStorage.getItem("token");
        const lastToken = localStorage.getItem("lastToken");
        if (token && lastToken && token === lastToken) {
            const savedBooks  = localStorage.getItem("lastBooks");
            const savedQuery  = localStorage.getItem("lastQuery");
            if (savedBooks) {
                setBooks(JSON.parse(savedBooks));
                setSearchQuery(savedQuery || "");
            }
        }
    }, []);


    const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
    };

    const handleSearchSubmit = async (e) => {
        e.preventDefault();
        if (!searchQuery.trim()) return;
        setLoading(true);
        setError(null);

        try {
            const token = localStorage.getItem("token");
            const response = await fetch(`http://localhost:8080/api/bookstore/search?query=${encodeURIComponent(searchQuery)}`, {
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                }
            });

            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const data = await response.json();

            if (data.single) {
                setBooks([data.singleResult]);
            } else {
                const combined = [
                    ...(data.localResults || []).map(b => ({ ...b, source: "INTERNAL" })),
                    ...(data.googleBookDto || []).map(b => ({ ...b, source: "GOOGLE" })),
                    ...(data.openLibraryResults || []).map(b => ({ ...b, source: "OPENLIBRARY" })),
                    ...(data.amazonResult || []).map(b => ({ ...b, source: "AMAZON" })),
                ];

                // Log what's coming from the API
                console.log("Combined results before deduplication:", combined.length);

                // More lenient deduplication approach
                const uniqueBooks = [];
                const seen = new Map(); // Use Map to track existing books with their similarity scores

                for (const book of combined) {
                    if (!book.title || !book.author) continue;

                    // Create a book signature that considers only *exact* matches as duplicates
                    const exactIsbn = (book.isbn || "").replace(/-/g, "").trim();

                    // Check for ISBN matches first (highest confidence)
                    if (exactIsbn && exactIsbn !== "n/a" && exactIsbn !== "N/A") {
                        if ([...seen.values()].some(b =>
                            (b.isbn || "").replace(/-/g, "").trim() === exactIsbn
                        )) {
                            continue; // Skip this duplicate
                        }
                    }

                    // Extract core title and language information
                    const normalizedTitle = book.title.toLowerCase().trim();
                    const normalizedAuthor = book.author.toLowerCase().trim();

                    // Parse language information more carefully
                    const techMatch = normalizedTitle.match(/\b(in|with)\s+([a-z#][\w+.#]*)\b/i);
                    const tech = techMatch ? techMatch[2].toLowerCase() : "";

                    // Check for edition information
                    const editionMatch = normalizedTitle.match(/\b(\d+[a-z]{0,2}|second|third|fourth|fifth)\s+edition\b/i);
                    const edition = editionMatch ? editionMatch[0].toLowerCase() : "";

                    // Extract the base title without tech or edition qualifiers
                    const baseTitle = normalizedTitle
                        .replace(/\s+(in|with)\s+[a-z#][\w+.#]*/gi, "")
                        .replace(/\s+-\s+.*edition/gi, "")
                        .replace(/\s+[\w\s]*edition/gi, "")
                        .replace(/\s*-\s*a\s+handbook\s+of.*$/i, "")
                        .replace(/\s*:\s*a\s+handbook\s+of.*$/i, "")
                        .replace(/^\s*the\s+art\s+of\s+/i, "")
                        .trim();

                    // Create a "signature" for the book that distinguishes tech variations
                    const signature = `${baseTitle}|${tech}|${edition}|${normalizedAuthor}`;

                    // Check if we've seen this exact signature before
                    if (seen.has(signature)) continue;

                    // Consider this a unique book based on our criteria
                    seen.set(signature, book);
                    uniqueBooks.push(book);
                }

                console.log("Unique books after deduplication:", uniqueBooks.length);
                setBooks(uniqueBooks);
                localStorage.setItem("lastQuery", searchQuery);
                localStorage.setItem("lastBooks", JSON.stringify(uniqueBooks));
                localStorage.setItem("lastToken", token);
            }
        } catch (error) {
            console.error("Search error:", error);
            setError("Failed to fetch books. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const handleAddToCart = async (books) => {
        const token = localStorage.getItem("token");
        console.log("addToCart jwt: ", token);
        if (!token) {
            alert("You must be logged in to add to cart");
            return;
        }

        if (!token || token.split(".").length !== 3) {
            alert("You must be logged in (or your session expired).");
            setLoading(false);
            return;
        }
        const payload = {
            quantity: 1,
            itemType: "BOOK",
            itemSource: books.source,
            bookId: books.source === "INTERNAL" ? books.id : undefined,
            externalId: books.source !== "INTERNAL" ? books.id : undefined,
            title: books.title,
            author: books.author,
            price: books.price,
            isbn: books.isbn,
            imageUrl: books.imageUrl
        };

        try {
            const res = await fetch("http://localhost:8080/api/v1/cart/add", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            alert("Added to cart!");
        } catch (e) {
            console.error("addToCart failed:", e)
            alert("Could not add to cart. See console for details")
        }
    }

    return (
        <div className="bookhub-container">
            {/* Header */}
            <header className="bookhub-header">
                <div className="header-content">
                    {/* Logo */}
                    <h1 className="bookhub-logo">BookHub</h1>

                    {/* Primary Search Bar */}
                    <div className="search-container">
                        <form onSubmit={handleSearchSubmit} className="search-form">
                            <input
                                type="text"
                                placeholder="Search for books..."
                                className="search-input"
                                value={searchQuery}
                                onChange={handleSearchChange}
                            />
                            <button type="submit" className="search-button">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                            </button>
                        </form>
                    </div>


                    {/* Navigation Icons */}
                    <div className="nav-icons">
                        <button className="icon-button">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                            </svg>
                        </button>
                        <button className="icon-button">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <circle cx="9" cy="21" r="1"></circle>
                                <circle cx="20" cy="21" r="1"></circle>
                                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                            </svg>
                        </button>
                        <button className="icon-button">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <line x1="3" y1="12" x2="21" y2="12"></line>
                                <line x1="3" y1="6" x2="21" y2="6"></line>
                                <line x1="3" y1="18" x2="21" y2="18"></line>
                            </svg>
                        </button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="bookhub-main">
                <div className="main-content">
                    <div className="welcome-section">
                        <h2 className="welcome-title">Welcome to BookHub</h2>
                        <p className="welcome-text">
                            Your digital gateway to a world of books. Use the search bar above to discover your next great read.
                        </p>
                        <p className="welcome-subtext">
                            Our collection features thousands of titles across all genres. Start your search to explore!
                        </p>
                    </div>

                    {/* Feature Highlights */}
                    <div className="features-grid">
                        <div className="feature-card">
                            <h3 className="feature-title">Discover</h3>
                            <p className="feature-text">Find new releases and bestsellers with our curated collections</p>
                        </div>
                        <div className="feature-card">
                            <h3 className="feature-title">Connect</h3>
                            <p className="feature-text">Join reading groups and share reviews with fellow book lovers</p>
                        </div>
                        <div className="feature-card">
                            <h3 className="feature-title">Save</h3>
                            <p className="feature-text">Enjoy special offers and member discounts on selected titles</p>
                        </div>
                    </div>
                    {/* Feedback */}
                    {loading && <p>Loading…</p>}
                    {error && <p style={{ color: "red" }}>{error}</p>}

                    {/* Results */}
                    <div className="results-grid">
                        {books.length === 0 && !loading && <p>No results</p>}
                        {books.map((b) => (
                            <div key={b.id} className="book-card">
                                <Link key={`${b.source}-${b.id}`}
                                      to={`/book/${b.source}/${encodeURIComponent(b.id)}`}
                                      state={{ book: b }}
                                      className="book-card">
                                <img
                                    src={b.imageUrl.startsWith("http") ? b.imageUrl : `${API_BASE}${b.imageUrl}`}
                                    alt={b.title}
                                    className="book-cover"
                                    onError={e => {
                                        e.currentTarget.onerror = null;
                                        e.currentTarget.src = "/placeholder.jpg";
                                    }}
                                />

                                <h3>{b.title}</h3>
                                <p><strong>Author:</strong> {b.author}</p>
                                {b.price != null && <p><strong>Price:</strong> {b.price}</p>}
                                {b.isbn && <p><strong>ISBN:</strong> {b.isbn}</p>}
                                <button className="add-to-cart-button" onClick={() => handleAddToCart(b)}>Add to Cart</button>
                                </Link>
                            </div>
                        ))}
                    </div>
                </div>
            </main>

            {/* Footer */}
            <footer className="bookhub-footer">
                <div className="footer-content">
                    <div className="footer-main">
                        <div className="footer-brand">
                            <h2 className="footer-logo">BookHub</h2>
                            <p className="footer-tagline">
                                Your trusted source for books across all genres and interests.
                            </p>
                            <div className="social-links">
                                <a href="#" className="social-link">Facebook</a>
                                <a href="#" className="social-link">Twitter</a>
                                <a href="#" className="social-link">Instagram</a>
                            </div>
                        </div>

                        <div className="footer-links">
                            <div className="footer-column">
                                <h3 className="footer-heading">About</h3>
                                <ul className="footer-list">
                                    <li><a href="#">About Us</a></li>
                                    <li><a href="#">Contact Us</a></li>
                                </ul>
                            </div>

                            <div className="footer-column">
                                <h3 className="footer-heading">Help</h3>
                                <ul className="footer-list">
                                    <li><a href="#">FAQ</a></li>
                                    <li><a href="#">Returns</a></li>
                                </ul>
                            </div>

                            <div className="footer-column">
                                <h3 className="footer-heading">Policies</h3>
                                <ul className="footer-list">
                                    <li><a href="#">Privacy</a></li>
                                    <li><a href="#">Terms</a></li>
                                </ul>
                            </div>
                        </div>
                    </div>

                    <div className="footer-bottom">
                        <p className="copyright">&copy; {new Date().getFullYear()} BookHub. All rights reserved.</p>
                    </div>
                </div>
            </footer>
        </div>
    );
};


const dedupeFuzzy = (arr, threshold = 0.8) => {
    const kept = [];
    for (const book of arr) {
        // see if we already kept something “close” in title
        const match = kept.find(b => {
            const sim = stringSimilarity.compareTwoStrings(
                b.title.toLowerCase().trim(),
                book.title.toLowerCase().trim()
            );
            return sim >= threshold;
        });
        if (!match) kept.push(book);
    }
    return kept;
}
export default HomePage