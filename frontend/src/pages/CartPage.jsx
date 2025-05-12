// src/pages/CartPage.jsx
import { useEffect, useState, useContext } from 'react'
import { AuthContext } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'

const CartPage = () => {
    const { user, logout } = useContext(AuthContext)
    const [cart, setCart] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const navigate = useNavigate()

    useEffect(() => {
        const token = localStorage.getItem('token')
        if (!token) {
            // not logged in, redirect to log in
            return navigate('/login')
        }

        fetch('http://localhost:8080/api/v1/cart', {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        })
            .then(res => {
                if (res.status === 401 || res.status === 403) {
                    logout()
                    throw new Error('Session expired')
                }
                if (!res.ok) throw new Error(`HTTP ${res.status}`)
                return res.json()
            })
            .then(data => {
                setCart(data)
            })
            .catch(err => {
                setError(err.message)
            })
            .finally(() => setLoading(false))
    }, [logout, navigate])

    if (loading) return <p>Loading your cart…</p>
    if (error)   return <p style={{ color: 'red' }}>{error}</p>
    if (!cart || !cart.items.length) {
        return (
            <div style={{ padding: '2rem' }}>
                <h2>Your cart is empty</h2>
                <button onClick={() => navigate('/home')}>← Back to search</button>
            </div>
        )
    }


    const total = cart.items.reduce((sum, item) =>
        sum + (item.price || 0) * item.quantity, 0)



    return (
        <div style={{ maxWidth: 800, margin: '2rem auto', padding: '0 1rem' }}>
            <h1>Your Cart</h1>
            <ul style={{ listStyle: 'none', padding: 0 }}>
                {cart.items.map(item => (
                    <li key={item.id} style={{
                        display: 'flex', alignItems: 'center', marginBottom: '1rem',
                        padding: '1rem', border: '1px solid #ddd', borderRadius: 6
                    }}>
                        <img
                            src={item.book?.imageUrl || item.externalId && item.imageUrl || '/placeholder.jpg'}
                            alt={item.book?.title || item.externalTitle}
                            style={{ width: 80, height: 120, objectFit: 'cover', marginRight: 16 }}
                        />
                        <div style={{ flex: 1 }}>
                            <h3 style={{ margin: '0 0 .5rem' }}>
                                {item.book?.title || item.externalTitle}
                            </h3>
                            <p style={{ margin: '0 .5rem 0' }}>
                                Author: {item.book?.author || item.externalAuthor}
                            </p>
                            <p style={{ fontWeight: "bold" }}>
                                Price: ${ (item.book?.price ?? item.price ?? 0).toFixed(2) }
                            </p>
                            <p style={{ margin: '0 .5rem 0' }}>
                                Quantity: {item.quantity}
                            </p>
                        </div>
                    </li>
                ))}
            </ul>

            <h2>Total: ${total.toFixed(2)}</h2>
            <button
                onClick={() => alert('…checkout flow not implemented yet…')}
                style={{
                    padding: '0.75rem 1.5rem',
                    background: '#0070f3',
                    color: 'white',
                    border: 'none',
                    borderRadius: 4,
                    cursor: 'pointer',
                }}
            >
                Checkout
            </button>
        </div>
    )
}

export default CartPage;
