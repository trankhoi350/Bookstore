// src/pages/BookDetail.jsx
import { useParams, useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect, useContext } from "react";
import { AuthContext } from "../context/AuthContext.jsx";

export default function BookDetail() {
    const { user } = useContext(AuthContext);
    const token = user?.token;
    const { source, id } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    const [book, setBook]       = useState(location.state?.book || null);
    const [loading, setLoading] = useState(!book);
    const [error, setError]     = useState("");
    const [qty, setQty]         = useState(1);
    const [adding, setAdding]   = useState(false);

    // If we arrived via Link, we already have the book in location.state.
    // Otherwise, fetch it by searching your unified API:
    useEffect(() => {
        if (book) return;

        setLoading(true);
        fetch(`/api/bookstore/search?query=${encodeURIComponent(id)}`, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            }
        })
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(data => {
                const combined = [
                    ...(data.localResults || []).map(b => ({ ...b, source: "INTERNAL" })),
                    ...(data.googleBookDto || []).map(b => ({ ...b, source: "GOOGLE" })),
                    ...(data.openLibraryResults || []).map(b => ({ ...b, source: "OPENLIBRARY" })),
                    ...(data.amazonResult || []).map(b => ({ ...b, source: "AMAZON" })),
                ];
                const found = combined.find(
                    b => b.id.toString() === id && b.source === source.toUpperCase()
                );
                if (!found) throw new Error("Book not found");
                setBook(found);
            })
            .catch(e => setError(e.message))
            .finally(() => setLoading(false));
    }, [source, id, token]);

    if (loading) return <p>Loading book details…</p>;
    if (error)   return <p style={{ color: "red" }}>Error: {error}</p>;
    if (!book)   return <p>Book not found</p>;

    const handleAddToCart = async () => {
        if (!token) {
            alert("Please log in to add to cart");
            return;
        }

        setAdding(true);
        const payload = {
            quantity:   qty,
            itemType:   "BOOK",
            itemSource: book.source,
            // depending on source, backend looks at bookId or externalId
            bookId:     book.source === "INTERNAL" ? book.id : undefined,
            externalId: book.source !== "INTERNAL" ? book.id : undefined,
            // and title/author must match your DTO fields
            title:      book.title,
            author:     book.author,
            price:      book.price,
            isbn:       book.isbn,
            imageUrl:   book.imageUrl
        };

        try {
            const res = await fetch("http://localhost:8080/api/v1/cart/add", {
                method: "POST",
                headers: {
                    "Content-Type":  "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            alert(`✅ Added ${qty}× "${book.title}" to cart!`);
        } catch (e) {
            console.error("addToCart failed:", e);
            alert("❌ Could not add to cart. See console for details.");
        } finally {
            setAdding(false);
        }
    };

    return (
        <div style={{ maxWidth: 600, margin: "2rem auto", padding: "0 1rem" }}>
            <button
                onClick={() => navigate(-1)}
                className="button secondary"
                style={{ marginBottom: "1rem" }}
            >
                ← Back to results
            </button>

            <img
                src={book.imageUrl || "/placeholder.jpg"}
                alt={book.title}
                style={{ width: "100%", borderRadius: 4 }}
            />

            <h1 style={{ margin: "1rem 0 0.5rem" }}>{book.title}</h1>
            <p><strong>Author:</strong> {book.author}</p>
            {book.price && <p><strong>Price:</strong> ${book.price}</p>}
            {book.isbn  && <p><strong>ISBN:</strong> {book.isbn}</p>}
            {book.publicationYear && <p><strong>Year:</strong> {book.publicationYear}</p>}
            {book.pageCount       && <p><strong>Pages:</strong> {book.pageCount}</p>}
            {book.genre           && <p><strong>Genre:</strong> {book.genre}</p>}

            {/* Quantity selector */}
            <div style={{ display: "flex", alignItems: "center", margin: "1rem 0" }}>
                <button
                    onClick={() => setQty(q => Math.max(1, q - 1))}
                    className="button secondary"
                    disabled={qty <= 1 || adding}
                >–</button>
                <span style={{ margin: "0 1rem", fontSize: "1.2rem" }}>{qty}</span>
                <button
                    onClick={() => setQty(q => q + 1)}
                    className="button secondary"
                    disabled={adding}
                >+</button>
            </div>

            {/* Add to cart */}
            <button
                onClick={handleAddToCart}
                className="button"
                disabled={adding}
            >
                {adding ? "Adding…" : "Add to Cart"}
            </button>
        </div>
    );
}
