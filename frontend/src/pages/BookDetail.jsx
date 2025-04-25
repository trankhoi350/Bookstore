import { useParams, useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";

const BookDetail = () => {
    const { source, id } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    // If we navigated here from HomePage, location.state.book is already available
    const [book, setBook] = useState(location.state?.book || null);
    const [loading, setLoading] = useState(!book);
    const [error, setError] = useState("");

    useEffect(() => {
        if (book) return;

        setLoading(true);
        fetch(`/api/bookstore/search?query=${encodeURIComponent(id)}`, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${localStorage.getItem("token")}`
            }
        })
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(data => {
                // collect all sources back into one array
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
    }, [source, id]);

    if (loading)  return <p>Loading book details…</p>;
    if (error)    return <p style={{ color: "red" }}>Error: {error}</p>;
    if (!book)    return <p>Book not found</p>;

    return (
        <div style={{ maxWidth: 600, margin: "2rem auto", padding: "0 1rem" }}>
            <button onClick={() => navigate(-1)} style={{ marginBottom: "1rem" }}>
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
            {book.isbn &&  <p><strong>ISBN:</strong> {book.isbn}</p>}

            {/* any extra fields your DTO carries: */}
            {book.publicationYear && <p><strong>Year:</strong> {book.publicationYear}</p>}
            {book.pageCount       && <p><strong>Pages:</strong> {book.pageCount}</p>}
            {book.genre           && <p><strong>Genre:</strong> {book.genre}</p>}

            {/* Add-to-cart button here, or a longer description */}
            <button onClick={() => {/* …reuse your handleAddToCart(book)… */}}>
                Add to Cart
            </button>
        </div>
    );
}

export default BookDetail