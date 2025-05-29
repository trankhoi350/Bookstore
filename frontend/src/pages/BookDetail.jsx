import { useParams, useLocation, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";

const BookDetail = () => {
    const { source, id } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    const [book, setBook] = useState(location.state?.book || null);
    const [loading, setLoading] = useState(!book);
    const [error, setError] = useState("");

    const [qty, setQty] = useState(1);
    const [adding, setAdding] = useState(false);
    const [updating, setUpdating] = useState(false);

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
                const combined = [
                    ...(data.localResults || []).map(b => ({
                        ...b,
                        source: "INTERNAL",
                        isbn: b.isbn || "N/A"
                    })),
                    ...(data.googleBookResults || []).map(b => ({
                        ...b,
                        source: "GOOGLE",
                        isbn: b.isbn || "N/A"
                    })),
                    ...(data.openLibraryResults || []).map(b => ({
                        ...b,
                        source: "OPENLIBRARY",
                        isbn: b.isbn || "N/A"
                    })),
                    ...(data.amazonBookResults || []).map(b => ({
                        ...b,
                        source: "AMAZON",
                        isbn: b.isbn || "N/A"
                    })),
                ];
                const found = combined.find(
                    b => b.id.toString() === id && b.source === source.toUpperCase()
                );
                if (!found) throw new Error("Book not found");
                setBook(found);
                console.log("Fetched book:", found);
            })
            .catch(e => setError(e.message))
            .finally(() => setLoading(false));
    }, [source, id]);

    if (loading) return <p>Loading book details…</p>;
    if (error) return <p style={{ color: "red" }}>Error: {error}</p>;
    if (!book) return <p>Book not found</p>;

    const handleAddToCart = async () => {
        const token = localStorage.getItem("token");
        if (!token) {
            alert("Please log in to add to cart");
            return;
        }
        const displayedPrice = book.price ?? book.saleInfo?.listPrice?.amount ?? 0;

        setAdding(true);
        const payload = {
            quantity: qty,
            itemType: "BOOK",
            itemSource: book.source === "INTERNAL" ? "INTERNAL" : "EXTERNAL",
            bookId: book.source === "INTERNAL" ? book.id : undefined,
            externalId: book.source !== "INTERNAL" ? book.id : undefined,
            title: book.title,
            author: book.author,
            price: displayedPrice,
            isbn: book.isbn,
            imageUrl:
                book.source === "GOOGLE"
                    ? (
                        book.volumeInfo?.imageLinks?.extraLarge
                        || book.volumeInfo?.imageLinks?.large
                        || book.volumeInfo?.imageLinks?.medium
                        || book.volumeInfo?.imageLinks?.thumbnail
                    )
                    : book.source === "OPENLIBRARY" && book.cover_i
                        ? `https://covers.openlibrary.org/b/id/${book.cover_i}-L.jpg`
                        : book.imageUrl
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

            if (!res.ok) {
                if (res.status === 403) {
                    alert("Authentication error. Please log in again.");
                } else {
                    let text = await res.text();
                    throw new Error(`HTTP ${res.status}: ${text}`);
                }
            } else {
                alert(`Added ${qty}× "${book.title}" to cart!`);
            }
        } catch (e) {
            console.error("addToCart failed:", e);
            alert("Could not add to cart. See console for details.");
        } finally {
            setAdding(false);
        }
    };

    const handleUpdateInDatabase = async () => {
        const token = localStorage.getItem("token");

        if (!token) {
            alert("Please log in to update the database");
            return;
        }

        const displayedPrice = book.price ?? book.saleInfo?.listPrice?.amount ?? 29.99;

        setUpdating(true);
        const payload = {
            title: book.title,
            author: book.author,
            isbn: book.isbn || "N/A",
            price: displayedPrice
        };

        try {
            const res = await fetch("http://localhost:8080/api/bookstore/book/update", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                let text = await res.text();
                throw new Error(`HTTP ${res.status}: ${text}`);
            } else {
                const responseText = await res.text();
                alert(`Book ${book.title} updated/added to database successfully! ${responseText}`);
            }
        } catch (e) {
            console.error("updateInDatabase failed:", e);
            alert("Could not update/add book to database. See console for details.");
        } finally {
            setUpdating(false);
        }
    };

    return (
        <div style={{ maxWidth: 600, margin: "2rem auto", padding: "0 1rem" }}>
            <button
                onClick={() => navigate(-1)}
                style={{ marginBottom: "1rem" }}
                className="back button"
            >← Back to results</button>

            <img
                src={book.imageUrl || "/placeholder.jpg"}
                alt={book.title}
                style={{ width: "100%", borderRadius: 4 }}
            />

            <h1 style={{ margin: "1rem 0 0.5rem" }}>{book.title}</h1>
            <p><strong>Author:</strong> {book.author}</p>
            {book.isbn && <p><strong>ISBN:</strong> {book.isbn}</p>}
            {book.price && <p><strong>Price:</strong> ${book.price}</p>}
            {book.publicationYear && <p><strong>Year:</strong> {book.publicationYear}</p>}
            {book.pageCount && <p><strong>Pages:</strong> {book.pageCount}</p>}
            {book.genre && <p><strong>Genre:</strong> {book.genre}</p>}

            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", margin: "1rem 0" }}>
                <button
                    onClick={() => setQty(q => Math.max(1, q - 1))}
                    disabled={adding || qty <= 1}
                    className="quantity button"
                >−</button>
                <span>{qty}</span>
                <button
                    onClick={() => setQty(q => q + 1)}
                    disabled={adding}
                    className="quantity button"
                >+</button>
            </div>

            <div className="button-container">
                <button
                    onClick={handleAddToCart}
                    disabled={adding}
                    className="add-to-cart-button"
                >
                    {adding ? "Adding…" : "Add to Cart"}
                </button>

                <button
                    onClick={handleUpdateInDatabase}
                    disabled={updating}
                    className="update-button"
                >
                    {updating ? "Updating…" : "Update in Database"}
                </button>
            </div>
        </div>
    );
};

export default BookDetail;