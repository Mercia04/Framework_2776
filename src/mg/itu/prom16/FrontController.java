package mg.itu.prom16;
import java.io.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
public class FrontController extends HttpServlet{
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            processRequest(req, res);
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
        PrintWriter out = res.getWriter();
        out.println(req.getRequestURL().toString());
    }
}