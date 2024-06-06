package mg.itu.prom16;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.itu.prom16.annotation.MyAnnotation;
import mg.itu.prom16.annotation.MyGet;

public class FrontController extends HttpServlet {
    protected HashMap<String, MyMapping> map = new HashMap<>();

    public HashMap<String, MyMapping> getMap() {
        return map;
    }

    public void setMap(HashMap<String, MyMapping> map) {
        this.map = map;
    }

    public void init() throws ServletException {
        super.init();
        String cont = getServletContext().getInitParameter("controler");
        try {
            map = this.getControllerList(map, cont);
            if (map.isEmpty()) {
                throw new ServletException("Le package ne contient aucun controleur.");
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation des controleurs: " + e.getMessage(), e);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected String mySplit(String text) {
        String[] lolo = text.split("/");
        return lolo[lolo.length - 1];
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        try {
            String url = mySplit(req.getRequestURL().toString());
            MyMapping lMapping = map.get(url);
            if (lMapping == null) {
                throw new ServletException("Aucun mapping trouver pour l'URL : " + url);
            }

            String val = "Classe= " + lMapping.getClasse() + "  Methode= " + lMapping.getMethode();
            Class<?> clazz = Class.forName(lMapping.getClasse());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getMethod(lMapping.getMethode());
            Object returnval = method.invoke(instance);

            if (returnval instanceof String) {
                val += " Reponse= " + returnval;
                out.println(val);
            } else if (returnval instanceof ModelView) {
                ModelView modelView = (ModelView) returnval;
                HashMap<String, Object> data = modelView.getData();
                String urlData = modelView.getUrl();

                data.forEach(req::setAttribute);

                RequestDispatcher dispatcher = req.getRequestDispatcher(urlData);
                dispatcher.forward(req, res);
            } else {
                throw new ServletException("Type de retour non supporter : " + returnval.getClass().getName());
            }
        } catch (ServletException e) {
            handleError(req, res, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(out); // Afficher la trace complète de l'exception
            handleError(req, res, "Erreur interne du serveur.");
        }
    }

    public HashMap<String, MyMapping> getControllerList(HashMap<String, MyMapping> map, String packagename) throws Exception {
        String bin_path = "WEB-INF/classes/" + packagename.replace(".", "/");
        bin_path = getServletContext().getRealPath(bin_path);

        File b = new File(bin_path);
        if (!b.exists() || !b.isDirectory()) {
            throw new ServletException("Le package specifie n'existe pas ou ne contient aucun controleur.");
        }

        for (File onefile : b.listFiles()) {
            if (onefile.isFile() && onefile.getName().endsWith(".class")) {
                Class<?> clazz = Class.forName(packagename + "." + onefile.getName().split(".class")[0]);
                if (clazz.isAnnotationPresent(MyAnnotation.class)) {
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(MyGet.class)) {
                            String url = method.getAnnotation(MyGet.class).value();
                            if (map.containsKey(url)) {
                                throw new ServletException("L'URL " + url + " est gerer par plusieurs controleurs.");
                            }
                            MyMapping myMapping = new MyMapping(clazz.getName(), method.getName());
                            map.put(url, myMapping);
                        }
                    }
                }
            }
        }
        return map;
    }

    private void handleError(HttpServletRequest req, HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = res.getWriter();
        out.println("<html><body>");
        out.println("<h2>Error:</h2>");
        out.println("<p>" + message + "</p>");
        out.println("</body></html>");
    }
}
