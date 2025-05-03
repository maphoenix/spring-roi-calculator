import axios from "axios";

const axiosClient = axios.create({
  baseURL: "http://localhost:8080", // Your Spring Boot backend URL
  headers: {
    "Content-Type": "application/json",
  },
});

export default axiosClient;
