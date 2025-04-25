```mermaid
   User

  User --> (Search for books)
  User --> (View book details)
  User --> (Add book to cart)
  User --> (View cart)
  User --> (Login / Logout)

  (Add book to cart) ..> (Login / Logout) : <<extend>>
  (View cart)          ..> (Login / Logout) : <<extend>>
```