package mg.itu.prom16;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.prom16.annotation.*;
public class FrontController extends HttpServlet{
    protected HashMap<String,MyMapping> map=new HashMap<>();

    public HashMap<String, MyMapping> getMap() {
        return map;
    }

    public void setMap(HashMap<String, MyMapping> map) {
        this.map = map;
    }

    public void init() throws ServletException{
        super.init();
        String cont=getServletContext().getInitParameter("controler");
        HashMap<String,MyMapping> maps=getMap();
        try {
            maps=this.getControllerList(maps,cont);
            this.setMap(maps); 
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            processRequest(req, res);
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            processRequest(req, res);
    }
    protected String mySplit(String text){
        String[] lolo=text.split("/");
        int length=lolo.length;
            return lolo[length-1];
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
        PrintWriter out = res.getWriter();
        try {
        String url=mySplit(req.getRequestURL().toString());
        HashMap<String,MyMapping> rep=getMap();    
        MyMapping lMapping=rep.get(url);
        String val="Classe= "+lMapping.getClasse()+"  Methode= "+lMapping.getMethode();
        Class<?> clazz=Class.forName(lMapping.getClasse());
        Object instance=clazz.getDeclaredConstructor().newInstance();
        Method method=clazz.getMethod(lMapping.getMethode());
        Object returnval=method.invoke(instance);
        if(returnval instanceof String){
            val=val+" Reponse= "+(String)returnval;
        }
        out.println(val);
        } catch (Exception e) {
            String ex=new String("Methode not found");
            out.println(ex);
        }
    }

    public HashMap<String,MyMapping> getControllerList(HashMap<String,MyMapping> map, String packagename) throws Exception {
        String bin_path = "WEB-INF/classes/" + packagename.replace(".", "/");
        bin_path = getServletContext().getRealPath(bin_path);

        File b = new File(bin_path);
        for (File onefile : b.listFiles()) {
            if (onefile.isFile() && onefile.getName().endsWith(".class")) {
                Class<?> clazz = Class.forName(packagename + "." + onefile.getName().split(".class")[0]);
                 if (clazz.isAnnotationPresent(MyAnnotation.class)){
                    Method[] methods=clazz.getMethods();
                    String classe=clazz.getName();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(MyGet.class)) {
                            String nomMethode=method.getName();
                            MyMapping myMapping=new MyMapping(classe,nomMethode);
                            map.put((String)method.getAnnotation(MyGet.class).value(),myMapping);
                        }
                    }
                }
            }
        }
        return map;
    }
}
