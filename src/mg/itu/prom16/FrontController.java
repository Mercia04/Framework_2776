package mg.itu.prom16;
import java.io.*;
import java.util.ArrayList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.prom16.annotation.*;
public class FrontController extends HttpServlet{
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            processRequest(req, res);
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
        PrintWriter out = res.getWriter();
        // out.println(req.getRequestURL().toString());
        try {
            getControllerList(getServletContext().getInitParameter("controler"));

            for (String contr : controllerArrayList) {
                out.println(contr);
            }
        } catch (Exception e) {
            out.println(e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                out.println(ste);
            }
        }
    }
    protected ArrayList<String> controllerArrayList = new ArrayList<String>();

    public void getControllerList(String packagename) throws Exception {
        String bin_path = "WEB-INF/classes/" + packagename.replace(".", "/");

        bin_path = getServletContext().getRealPath(bin_path);

        File b = new File(bin_path);
        for (File onefile : b.listFiles()) {
            if (onefile.isFile() && onefile.getName().endsWith(".class")) {
                Class<?> clazz = Class.forName(packagename + "." + onefile.getName().split(".class")[0]);
                if (clazz.isAnnotationPresent(MyAnnotation.class))
                    controllerArrayList.add(clazz.getName());
            }
        }
    }
}