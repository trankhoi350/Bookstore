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
    const [mostSearched, setMostSearched] = useState([]);
    const [showDropdown, setShowDropdown] = useState(false);

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

    useEffect(() => {
        // Fetch most-searched books on component mount
        const fetchMostSearched = async () => {
            try {
                const res = await fetch(`${API_BASE}/api/bookstore/most-searched`, {
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${localStorage.getItem("token")}`
                    }
                });
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                const data = await res.json();
                console.log("Most searched books:", data);
                setMostSearched(data);
            } catch (err) {
                console.error("Failed to fetch most-searched books:", err);
                setMostSearched([]); // Ensure state is set even on error
            }
        };
        fetchMostSearched();
    }, []);




    const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
        setShowDropdown(true);
        console.log("showDropdown set to true");
    };

    const handleSearchSubmit = async (e) => {
        e.preventDefault();
        if (!searchQuery.trim()) return;
        setBooks([]); // Clear previous searched results
        setLoading(true);
        setError(null);
        setShowDropdown(false);

        try {
            const token = localStorage.getItem("token");
            console.log("Sending request to:", `http://localhost:8080/api/bookstore/search?query=${encodeURIComponent(searchQuery)}`);
            console.log("Authorization token:", token);
            const res = await fetch(
                `http://localhost:8080/api/bookstore/search?query=${encodeURIComponent(searchQuery)}`,
                {
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${token}`,
                    },
                }
            );
            console.log("Response status:", res.status);
            if (res.status === 403) {
                setError("Access denied (403). Your session may have expired. Please log in again.");
                localStorage.removeItem("token");
                setLoading(false);
                return;
            }
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            console.log("Backend response:", data);

            // 1) If the backend told us “single,” just show that one.
            if (data.single) {
                setBooks([
                    {
                        ...data.singleResult,
                        source: "INTERNAL",
                    },
                ]);
                return;
            }

            // 2) Pull out local results first.
            const local = (data.localResults || []).map((b) => ({
                ...b,
                source: "INTERNAL",
                isbn: b.isbn || "N/A",
                price: b.price != null ? b.price : 29.99 // Default price
            }));

            // 3) Check for an exact-title match in that local list.
            const exactLocal = local.find(
                (b) => b.title && b.title.trim().toLowerCase() === searchQuery.trim().toLowerCase()
            );
            if (exactLocal) {
                setBooks([exactLocal]);
                return;
            }

            // 4) Merge in the external hits
            const external = [
                ...(data.googleBookResults || []).map((b) => ({ ...b, source: "GOOGLE", isbn: b.isbn || "N/A", price: b.price != null ? b.price : 29.99 })),
                ...(data.openLibraryResults || []).map((b) => ({ ...b, source: "OPENLIBRARY", isbn: b.isbn || "N/A", price: b.price != null ? b.price : 29.99 })),
                ...(data.amazonBookResults || []).map((b) => ({ ...b, source: "AMAZON", isbn: b.isbn || "N/A", price: b.price != null ? b.price : 29.99 })),
            ];

            // 5) Normalize query for startsWith and exact match checks
            const normalizedQuery = searchQuery.toLowerCase().replaceAll("[^a-z0-9 ]", "");

            // 6) Reconstruct unified list, prioritizing startsWith books with sub-ordering
            const allBooks = [...local, ...external];

            // Split into startsWithBooks and containsBooks
            const startsWithBooks = allBooks.filter((book) => {
                const title = book.title ? book.title.toLowerCase().replaceAll("[^a-z0-9 ]", "") : "";
                return title.startsWith(normalizedQuery) || title.startsWith(normalizedQuery + " ");
            });

            // Sort startsWithBooks: exact matches (score 115) before other startsWith (score 95)
            startsWithBooks.sort((a, b) => {
                const titleA = a.title ? a.title.toLowerCase().replaceAll("[^a-z0-9 ]", "") : "";
                const titleB = b.title ? b.title.toLowerCase().replaceAll("[^a-z0-9 ]", "") : "";
                const aExactMatch = titleA === normalizedQuery;
                const bExactMatch = titleB === normalizedQuery;
                if (aExactMatch && !bExactMatch) return -1; // Prioritize exact matches (score 115)
                if (!aExactMatch && bExactMatch) return 1;
                return 0; // Maintain relative order for other startsWith books (score 95)
            });

            const containsBooks = allBooks.filter((book) => {
                const title = book.title ? book.title.toLowerCase().replaceAll("[^a-z0-9 ]", "") : "";
                return !title.startsWith(normalizedQuery) && !title.startsWith(normalizedQuery + " ") && title.includes(normalizedQuery);
            });

            const unifiedList = [...startsWithBooks, ...containsBooks];

            // 7) Deduplicate while preserving the order
            const seen = new Set();
            const unique = [];
            for (const book of unifiedList) {
                const title = book.title ? book.title.toString() : "";
                const author = book.author ? book.author.toString() : "";
                if (!book.title) continue;

                // ISBN-based dedupe
                const isbn = (book.isbn || "").replace(/[^0-9Xx]/g, "");
                if (isbn && seen.has(`ISBN:${isbn}`)) continue;
                if (isbn) seen.add(`ISBN:${isbn}`);

                // Fallback title-author signature
                const sig = `${book.title.trim().toLowerCase()}|${(book.author || "").trim().toLowerCase()}`;
                if (seen.has(sig)) continue;
                seen.add(sig);

                unique.push(book);
            }

            // 8) Set books without additional sorting
            console.log("Unique books:", unique);
            console.log("Sorted unique books:", unique.map(b => b.title));
            setBooks(unique);
            localStorage.setItem("lastQuery", searchQuery);
            localStorage.setItem("lastBooks", JSON.stringify(unique));
            localStorage.setItem("lastToken", token);
        } catch (err) {
            console.error("Search error:", err);
            setError("Failed to fetch books. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const handleSuggestionClick = (suggestion) => {
        setSearchQuery(suggestion);
        setShowDropdown(false);
        handleSearchSubmit({ preventDefault: () => {} }); // Trigger search
    };


    return (
        <div className="bookhub-container">
            {/* Header */}
            <header className="bookhub-header">
                <div className="header-content">
                    {/* Logo */}
                    <h1 className="bookhub-logo">BookHub</h1>

                    {/* Primary Search Bar */}
                    <div className="search-container" style={{ position: "relative" }}>
                        <form onSubmit={handleSearchSubmit} className="search-form">
                            <input
                                type="text"
                                placeholder="Search for books..."
                                className="search-input"
                                value={searchQuery}
                                onChange={handleSearchChange}
                                onFocus={() => setShowDropdown(true)}
                                onBlur={() => setTimeout(() => setShowDropdown(false), 200)}
                            />
                            <button type="submit" className="search-button">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                            </button>
                        </form>
                        {showDropdown && (
                            <ul className="search-dropdown" style={{
                                position: "absolute",
                                top: "100%",
                                left: 0,
                                right: 0,
                                backgroundColor: "white",
                                border: "1px solid #ddd",
                                borderRadius: "4px",
                                boxShadow: "0 2px 5px rgba(0,0,0,0.1)",
                                listStyle: "none",
                                padding: 0,
                                margin: 0,
                                zIndex: 10
                            }}>
                                {mostSearched.length > 0 ? (
                                    mostSearched.map((suggestion, index) => (
                                        <li
                                            key={index}
                                            onClick={() => handleSuggestionClick(suggestion)}
                                            style={{
                                                padding: "8px 12px",
                                                cursor: "pointer",
                                                borderBottom: index < mostSearched.length - 1 ? "1px solid #ddd" : "none"
                                            }}
                                            onMouseDown={(e) => e.preventDefault()}
                                        >
                                            {suggestion}
                                        </li>
                                    ))
                                ) : (
                                    <li style={{ padding: "8px 12px", color: "#888" }}>
                                        No search history available
                                    </li>
                                )}
                            </ul>
                        )}
                    </div>


                    {/* Navigation Icons */}
                    <div className="nav-icons">
                        <button className="icon-button">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                            </svg>
                        </button>
                        <Link to="/cart" className="icon-button" title="View Cart">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                                 fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                 strokeLinejoin="round">
                                <circle cx="9" cy="21" r="1"></circle>
                                <circle cx="20" cy="21" r="1"></circle>
                                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                            </svg>
                        </Link>

                        <button className="icon-button">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                                 fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                 strokeLinejoin="round">
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
                            Your digital gateway to a world of books. Use the search bar above to discover your next
                            great read.
                        </p>
                        <p className="welcome-subtext">
                            Our collection features thousands of titles across all genres. Start your search to explore!
                        </p>
                    </div>

                    {/* Feature Highlights */}
                    <div className="features-grid">
                        <div className="feature-card">
                            <h3 className="feature-title">Discover</h3>
                            <p className="feature-text">Find new releases and bestsellers with our curated
                                collections</p>
                        </div>
                        <div className="feature-card">
                            <h3 className="feature-title">Connect</h3>
                            <p className="feature-text">Join reading groups and share reviews with fellow book
                                lovers</p>
                        </div>
                        <div className="feature-card">
                            <h3 className="feature-title">Save</h3>
                            <p className="feature-text">Enjoy special offers and member discounts on selected titles</p>
                        </div>
                    </div>
                    {/* Feedback */}
                    {loading && <p>Loading…</p>}
                    {error && <p style={{color: "red"}}>{error}</p>}


                    <div className="results-grid">
                        {books.length === 0 && !loading && <p>No results</p>}
                        {books.map(b => (
                            <div key={`${b.source}-${b.id}`} className="book-card">
                                <Link
                                    to={`/book/${b.source}/${encodeURIComponent(b.id)}`}
                                    state={{book: b}}
                                    className="book-link"
                                >
                                    <img
                                        src={b.imageUrl && b.imageUrl.startsWith("http")
                                            ? b.imageUrl
                                            : b.imageUrl
                                                ? `${API_BASE}${b.imageUrl}` : "/placeholder.jpg"}
                                        alt={b.title || "No title"}
                                        className="book-cover"
                                        onError={e => {
                                            e.currentTarget.onerror = null;
                                            e.currentTarget.src = "/placeholder.jpg";
                                        }}
                                    />
                                    <h3 className="book-title">{b.title}</h3>
                                </Link>
                                <p className="book-info"><strong>Author:</strong> {b.author}</p>
                                <p className="book-info"><strong>Price:</strong> ${b.price != null ? b.price : "N/A"}
                                </p>
                                <p className="book-info"><strong>ISBN:</strong> {b.isbn || "N/A"}</p>
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