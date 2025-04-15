import {Routes, Route, BrowserRouter} from "react-router-dom";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import "./index.css"
import HomePage from "./pages/HomePage.jsx";

const App = () => {
    return (
        <Routes>
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<Login />} />
            <Route path="/home" element={<HomePage />} />
        </Routes>
    );
};

export default App;


