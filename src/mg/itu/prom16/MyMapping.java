package mg.itu.prom16;

import java.util.ArrayList;
import java.util.List;

public class MyMapping {
    private String classe;
    private List<VerbMethode> list;

    public MyMapping(String a){
        this.setClasse(a);
        this.list = new ArrayList<>();  // Initialize the list here
    }

    public String getClasse() {
        return classe;
    }
    public void setClasse(String classe) {
        this.classe = classe;
    }
    public List<VerbMethode> getList() {
        return list;
    }
    public void setList(List<VerbMethode> list) {
        this.list = list;
    }

    public void addList(VerbMethode verbMethode){
        List<VerbMethode> tem= this.getList();
        tem.add(verbMethode);
    }

}
