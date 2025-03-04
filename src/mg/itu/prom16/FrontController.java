package mg.itu.prom16;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.itu.prom16.annotation.*;
import mg.itu.prom16.validation.*;

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
        HashMap<String,String> erreurs=new HashMap<>();
        HashMap<String, String> submittedValues = new HashMap<>();
        try {
            String url = mySplit(req.getRequestURL().toString());
            MyMapping mapping = carte.get(url);
            if (mapping == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.setContentType("text/plain");
                res.getWriter().println("Erreur 404 : url tsy misy ao @ mapping");
            }

            Class<?> classe = Class.forName(mapping.getClasse());
            Object instance = classe.getDeclaredConstructor().newInstance();

            Method[] methods=classe.getDeclaredMethods();
            Method methode=null;
            for (Method method1 : methods) {
                List<VerbMethode> lverbMethodes=mapping.getList();
                for (VerbMethode verbMethode : lverbMethodes) {
                    System.out.println("Recherche de méthode : " + method1.getName() + " avec " + verbMethode.getVerb());
                    if (method1.getName().equals(verbMethode.getVerb())) {
                        methode = method1;
                        break;
                    }
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
                    arguments[i] = creerObjetDepuisRequete(parametre.getType(),req,erreurs,submittedValues);
                } else if (parametre.getType() == MySession.class) {
                    arguments[i] = new MySession(req.getSession());
                } else {
                    throw new ServletException("ETU002776-Mila asina annotation ny parametre");
                }
            }

            Object retour = methode.invoke(instance, arguments);
            
            // Ajouter dans la méthode processRequest avant l'invocation de la méthode
            if (methode.isAnnotationPresent(Auth.class)) {
                Auth auth = methode.getAnnotation(Auth.class);
                MySession session = new MySession(req.getSession());
                String profilUtilisateur = (String) session.get("profil");
                
                boolean autorise = false;
                for (String profil : auth.profils()) {
                    if (profil.equals(profilUtilisateur)) {
                        autorise = true;
                        break;
                    }
                }
            } else {
                //raha tsisy annotation @Restapi
                if (retour instanceof String) {
                    out.println(retour);
                } else if (retour instanceof ModelView) {
                    ModelView vueModele = (ModelView) retour;
                    HashMap<String, Object> donnees = vueModele.getData();
                    String urlVue = vueModele.getUrl();
                    String UrlError=vueModele.getUrlError();
                    if (!erreurs.isEmpty()) {
                        //get erreur sy submittedvalue dia atao anaty Hmap
                        this.ajouterHashMapDansRequest(req,erreurs);
                        this.ajouterHashMapDansRequest(req,submittedValues);
                        RequestDispatcher dispatcher = req.getRequestDispatcher(UrlError);
                        dispatcher.forward(req, res);
                    }else{
                        // Ajouter les données
                        donnees.forEach(req::setAttribute);
                        RequestDispatcher dispatcher = req.getRequestDispatcher(urlVue);
                        dispatcher.forward(req, res);
                    }
                    
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

    // Créer-na le object amin'ny alalan'ny parametre 
    private Object creerObjetDepuisRequete(Class<?> classe, HttpServletRequest req, HashMap<String,String> erreurs, HashMap<String,String> submittedValues) throws Exception {
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
            Object valeur = convertirTypeChamp(champ, valeurParam, req, erreurs, submittedValues);
            if (valeur != null) {
                champ.set(obj, valeur);
            }
        }
        return obj;
    }
    
    

    // Convertir les valeurs des paramètres de la requête en types appropriés
    private Object convertirTypeChamp(Field champ, String valeurParam, HttpServletRequest req,HashMap<String,String> erreurs,HashMap<String,String> submittedValues) {
        submittedValues.put(champ.getName(), valeurParam); // Conservez la valeur soumise
        
        if (champ.isAnnotationPresent(MyRequired.class)) {
            MyRequired requiredAnnotation = champ.getAnnotation(MyRequired.class);
            if (valeurParam == null || valeurParam.trim().isEmpty()) {
                erreurs.put(champ.getName()+"Error", requiredAnnotation.message());
                return null;
            }
        }
        if (champ.isAnnotationPresent(MyNumeric.class)) {
            MyNumeric numericAnnotation = champ.getAnnotation(MyNumeric.class);
            if (!NumericValidator.isValidNumeric(valeurParam, numericAnnotation.min(), numericAnnotation.max()) || valeurParam==null  ) {
                erreurs.put(champ.getName()+"Error", numericAnnotation.message());
                return null; // Valeur par défaut si erreur
            }
            if (champ.getType() == int.class || champ.getType() == Integer.class) {
                return Integer.parseInt(valeurParam);
            } else if (champ.getType() == double.class || champ.getType() == Double.class) {
                return Double.parseDouble(valeurParam);
            }
        }
        

        if (champ.isAnnotationPresent(MyDate.class)) {
            MyDate dateAnnotation = champ.getAnnotation(MyDate.class);
            if (!DateValidator.isValidDate(valeurParam, dateAnnotation.format()) || valeurParam==null) {
                erreurs.put(champ.getName()+"Error", dateAnnotation.message());
                return null;
            }
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(dateAnnotation.format());
                return dateFormat.parse(valeurParam);
            } catch (ParseException e) {
                erreurs.put(champ.getName()+"Error", "Erreur de conversion de date.");
                return null;
            }
        }
        if (valeurParam == null || valeurParam.trim().isEmpty()) {
            return null;
        }    
        Class<?> typeChamp = champ.getType();
        if (typeChamp == FileUpload.class) {
            try {
                return FileUpload.handleFileUpload(req);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (typeChamp == int.class || typeChamp == Integer.class) {
            return Integer.parseInt(valeurParam);
        } else if (typeChamp == long.class || typeChamp == Long.class) {
            return Long.parseLong(valeurParam);
        } else if (typeChamp == double.class || typeChamp == Double.class) {
            return Double.parseDouble(valeurParam);
        } else if (typeChamp == boolean.class || typeChamp == Boolean.class) {
            return Boolean.parseBoolean(valeurParam);
        }
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
                            String temp="GET";
                            if (map.containsKey(url)) {
                                throw new ServletException("L'URL " + url + " est gerer par plusieurs controleurs.");
                            }
                            if (method.isAnnotationPresent(Get.class)) {
                                temp="GET";
                            } else if (method.isAnnotationPresent(Post.class)) {
                                temp="POST";
                            }
                            VerbMethode verbMethode=new VerbMethode(method.getName(), temp);
                            MyMapping myMapping = new MyMapping(clazz.getName());
                            myMapping.addList(verbMethode);
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

    public void ajouterHashMapDansRequest(HttpServletRequest request,HashMap<String,String> zavatra) {
        // Ajouter chaque entrée de la HashMap en tant qu'attribut dans la requête
        for (HashMap.Entry<String, String> entry : zavatra.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}

