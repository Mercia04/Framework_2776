package mg.itu.prom16;

public class MyMapping {
    private String classe;
    private String methode;
    public String getClasse() {
        return classe;
    }
    public void setClasse(String classe) {
        this.classe = classe;
    }
    public String getMethode() {
        return methode;
    }
    public void setMethode(String methode) {
        this.methode = methode;
    }
    public MyMapping(String classe,String methode){
        this.setClasse(classe);
        this.setMethode(methode);
    }

}
