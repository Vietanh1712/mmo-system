import javax.script.*;
public class Test {
    public static void main(String[] args) throws Exception {
        ScriptEngineManager m = new ScriptEngineManager();
        ScriptEngine e = m.getEngineByName("nashorn");
        if(e==null) e = m.getEngineByName("JavaScript");
        try {
            e.eval(new java.io.FileReader("d:/mmo-system/MMO_Market/MMO_Market (3)/MMO_Market/apps/frontend/static/js/seller/seller-console.js"));
            System.out.println("OK");
        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
