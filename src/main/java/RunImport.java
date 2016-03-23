/**
 * Created by yngve on 21.03.2016.
 */
public class RunImport {
    public static void main(String[] args) {
        //NmbfImport nmbfImport = new NmbfImport("C:\\Users\\yngve\\Documents\\nmbf-komposisjon-sjanger-with-tags.txt", "C:\\Users\\yngve\\Documents\\nmbf-komposisjon-sjanger-with-tags.rdf");
        NmbfImport nmbfImport = new NmbfImport("C:\\Users\\yngve\\Documents\\nmbf-instrumenter.txt", "C:\\Users\\yngve\\Documents\\nmbf-instrumenter.rdf");
        nmbfImport.run();
    }
}
