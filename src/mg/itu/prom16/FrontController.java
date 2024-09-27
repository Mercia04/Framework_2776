package mg.itu.prom16;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;

import com.google.gson.Gson;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.itu.prom16.annotation.FieldName;
import mg.itu.prom16.annotation.MyAnnotation;
import mg.itu.prom16.annotation.MyGet;
import mg.itu.prom16.annotation.MyParam;
import mg.itu.prom16.annotation.ParamObject;
import mg.itu.prom16.annotation.Restapi;

public class FrontController extends HttpServlet {
    protected HashMap<String, MyMapping> carte = new HashMap<>();

    public HashMap<String, MyMapping> getCarte() {
        return carte;
    }

    public void setCarte(HashMap<String, MyMapping> carte) {
        this.carte = carte;
    }

    public void init() throws ServletException {
        super.init();
        String controleur = getServletContext().getInitParameter("controler");
        try {
            carte = this.getControllerList(carte, controleur);
            if (carte.isEmpty()) {
                throw new ServletException("Le package ne contient aucun contrôleur.");
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation des contrôleurs: " + e.getMessage(), e);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    protected String mySplit(String texte) {
        String[] parties = texte.split("/");
        return parties[parties.length - 1];
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        try {
            String url = mySplit(req.getRequestURL().toString());
            MyMapping mapping = carte.get(url);
            if (mapping == null) {
                throw new ServletException("Aucun mapping trouvé pour l'URL : " + url);
            }

            Class<?> classe = Class.forName(mapping.getClasse());
            Object instance = classe.getDeclaredConstructor().newInstance();

            Method[] methods=classe.getDeclaredMethods();
            Method methode=null;
            for (Method method1 : methods) {
                if (method1.getName().equals(mapping.getMethode())) {
                    methode = method1;
                    break;
                }
            }
            Parameter[] parametres = methode.getParameters();
            Object[] arguments = new Object[parametres.length];

            for (int i = 0; i < parametres.length; i++) {
                Parameter parametre = parametres[i];
                if (parametre.isAnnotationPresent(MyParam.class)) {
                    MyParam annotationMyParam = parametre.getAnnotation(MyParam.class);
                    arguments[i] = req.getParameter(annotationMyParam.name());
                } else if (parametre.isAnnotationPresent(ParamObject.class)) {
                    arguments[i] = creerObjetDepuisRequete(parametre.getType(), req);
                } else if (parametre.getType() == MySession.class) {
                    arguments[i] = new MySession(req.getSession());
                } else {
                    throw new ServletException("ETU002776-Mila asina annotation ny parametre");
                }
            }

            Object retour = methode.invoke(instance, arguments);

            //verifier_na le annotation restapi
            if (methode.isAnnotationPresent(Restapi.class)) {
                res.setContentType("application/json");
                Gson gson = new Gson();

                //mamadika json
                if (retour instanceof ModelView) {
                    ModelView vueModele = (ModelView) retour;
                    out.print(gson.toJson(vueModele.getData()));
                } else {
                    out.print(gson.toJson(retour));
                }
            } else {
                //raha tsisy annotation @Restapi
                if (retour instanceof String) {
                    out.println(retour);
                } else if (retour instanceof ModelView) {
                    ModelView vueModele = (ModelView) retour;
                    HashMap<String, Object> donnees = vueModele.getData();
                    String urlVue = vueModele.getUrl();

                    // Ajouter les données
                    donnees.forEach(req::setAttribute);

                    RequestDispatcher dispatcher = req.getRequestDispatcher(urlVue);
                    dispatcher.forward(req, res);
                } else {
                    throw new ServletException("Type de retour non supporte : " + retour.getClass().getName());
                }
            }
        } catch (ServletException e) {
            gererErreur(req, res, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(out);
            gererErreur(req, res, "Erreur interne du serveur.");
        }
    } 

    // Créer un objet à partir des paramètres de la requête
    private Object creerObjetDepuisRequete(Class<?> classe, HttpServletRequest req) throws Exception {
        Object obj = classe.getDeclaredConstructor().newInstance();
        Field[] champs = classe.getDeclaredFields();
        
        for (Field champ : champs) {
            champ.setAccessible(true);
            String nomParam = champ.getName();
            
            if (champ.isAnnotationPresent(FieldName.class)) {
                FieldName annotationFieldName = champ.getAnnotation(FieldName.class);
                nomParam = annotationFieldName.value();
            }
    
            String valeurParam = req.getParameter(nomParam);
            
            if (valeurParam != null && !valeurParam.isEmpty()) {
                champ.set(obj, convertirTypeChamp(champ, valeurParam));
            }
        }
        return obj;
    }

    // Convertir les valeurs des paramètres de la requête en types appropriés
    private Object convertirTypeChamp(Field champ, String valeurParam) {
        Class<?> typeChamp = champ.getType();
        if (typeChamp == int.class || typeChamp == Integer.class) {
            return Integer.parseInt(valeurParam);
        } else if (typeChamp == long.class || typeChamp == Long.class) {
            return Long.parseLong(valeurParam);
        } else if (typeChamp == double.class || typeChamp == Double.class) {
            return Double.parseDouble(valeurParam);
        } else if (typeChamp == boolean.class || typeChamp == Boolean.class) {
            return Boolean.parseBoolean(valeurParam);
        }
        // Pour les types String ou d'autres types, retournez simplement la valeur brute
        return valeurParam;
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

    // Gérer les erreurs et afficher un message approprié
    private void gererErreur(HttpServletRequest req, HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = res.getWriter();
        out.println("<html><body>");
        out.println("<h2>Error:</h2>");
        out.println("<p>" + message + "</p>");
        out.println("</body></html>");
    }
}
