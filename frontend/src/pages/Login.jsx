import {useState, useContext, use} from "react";
import { useNavigate } from "react-router-dom";

import { AuthContext } from "../context/AuthContext.jsx";

const Login = () => {
    const {login} = useContext(AuthContext)
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("")
    const navigate = useNavigate()

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            await login({ email, password })
            alert("Login Successful")
            navigate("/home")
        } catch (error) {
            console.error("Login error: ", error);
            alert("Login failed");
        }
    };

    return (
        <div className="login-container">
            <div className="anchorPane">
                <h2 className="login">Login</h2>
                <form onSubmit={handleLogin}>
                    <input className="email" type="email" placeholder="Email"
                           onChange={(e) => setEmail(e.target.value)}/>
                    <input className="password" type="password" placeholder="Password"
                           onChange={(e) => setPassword(e.target.value)}/>
                    <button className="login-button" type="submit">Login</button>
                </form>
            </div>
        </div>
    );
};

export default Login;