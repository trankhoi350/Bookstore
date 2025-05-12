import {Routes, Route, BrowserRouter} from "react-router-dom";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import "./index.css"
import HomePage from "./pages/HomePage.jsx";
import BookDetail from "./pages/BookDetail.jsx";
import CartPage from "./pages/CartPage.jsx";

const App = () => {
    return (
        <Routes>
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<Login />} />
            <Route path="/home" element={<HomePage />} />
            <Route path="/book/:source/:id" element={<BookDetail />} />
            <Route path="/cart" element={<CartPage />} />
        </Routes>
    );
};

export default App;


