import { useState } from "react";
import axios from "axios";

const Register = () => {
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await axios.post("http://localhost:8080/api/v1/auth/register", { firstName, lastName, email, password });
            alert("Registration Successful");
        } catch(error) {
            alert("Registration Failed");
        }
    };

    return (
        <div className="register-container">
            <div className="anchorPane">
                <h2 className="register">Register</h2>
                <form onSubmit={handleSubmit}>
                    <input className="firstName" type="first-name" placeholder="First Name" onChange={(e) => setFirstName(e.target.value)}/>
                    <input className="lastName" type="last-name" placeholder="Last Name" onChange={(e) => setLastName(e.target.value)}/>
                    <input className="email" type="email" placeholder="Email" onChange={(e) => setEmail(e.target.value)}/>
                    <input className="password" type="password" placeholder="Password" onChange={(e) => setPassword(e.target.value)}/>
                    <button className="register-button" type="submit">Register</button>
                </form>
            </div>
        </div>
    );
};

export default Register;