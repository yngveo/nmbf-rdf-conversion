  import org.openrdf.model.Statement;
  import org.openrdf.model.ValueFactory;
  import org.openrdf.model.impl.SimpleValueFactory;
  import org.openrdf.model.vocabulary.RDF;
  import org.openrdf.model.vocabulary.SKOS;
  import org.openrdf.rio.RDFFormat;
  import org.openrdf.rio.RDFHandlerException;
  import org.openrdf.rio.RDFWriter;
  import org.openrdf.rio.Rio;

  import java.io.*;
  import java.util.ArrayList;

public class NmbfImport {

    String fileName, outputFilename;
    public final static String baseUri = "http://data.bbib.no/nmbf/";
    ValueFactory vf;
    ArrayList<Statement> allStatements;

    public NmbfImport(String fileName, String outputFilename) {
        this.fileName = fileName;
        this.outputFilename = outputFilename;
        vf = SimpleValueFactory.getInstance();
        allStatements = new ArrayList<>();
    }

    public void run() {
        BufferedReader br = null;

        try {

            String line;

            br = new BufferedReader(new FileReader(fileName));
            ArrayList<String> chunk = new ArrayList<>();

            while ((line = br.readLine()) != null) {

                line = line.trim();
                //System.out.println(line);
                if(line.equals("--NEXT--")) {
                    processChunk(chunk);
                    chunk = new ArrayList<>();
                } else {
                    line = line.trim();
                    chunk.add(line);
                }
            }
            generateRdfFile();
            System.out.println("Finished writing RDF file.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void generateRdfFile() {
        try {
            FileOutputStream out = new FileOutputStream(outputFilename);
            RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
            try {
                writer.startRDF();
                for (Statement st: allStatements) {
                    writer.handleStatement(st);
                }
                writer.endRDF();
            }
            catch (RDFHandlerException e) {
            }
        }
        catch (FileNotFoundException e) {
        }
    }

    public void processChunk(ArrayList<String> chunk) {

        if(chunk.size() <= 0)
            return;
        //System.out.println("ttst");
        // The subject is the first line in the chunk.
        String name = chunk.get(0);

        /*if(name.equals("Hardcore")) {
            System.out.println("d");
        }*/

        String id = generateUri(name);
        System.out.println(id);

        LineType currentLineType = LineType.UNKNOWN;
        LineType lastLineType = LineType.UNKNOWN;
        String cleanLine;
        int i = 0;
        boolean createConcept = true;
        for(String line : chunk) {
            i++;

            if(i==1)
                continue;

            currentLineType = getLineType(line);
            cleanLine = cleanLine(line);

            if(currentLineType == LineType.UNKNOWN && lastLineType != LineType.UNKNOWN) {
                currentLineType = lastLineType;
            }



            if(currentLineType == LineType.SEE_OTHER) {
                addStatement(vf.createStatement(vf.createIRI(generateUri(cleanLine)), SKOS.ALT_LABEL, vf.createLiteral(name)));
            }
            if(currentLineType == LineType.ALT_LABEL) {
                addStatement(vf.createStatement(vf.createIRI(id), SKOS.ALT_LABEL, vf.createLiteral(cleanLine)));
            }
            if(currentLineType == LineType.NARROWER) {
                addStatement(vf.createStatement(vf.createIRI(id), SKOS.NARROWER, vf.createIRI(generateUri(cleanLine))));
                addStatement(vf.createStatement(vf.createIRI(generateUri(cleanLine)), SKOS.BROADER, vf.createIRI(id)));
            }
            if(currentLineType == LineType.BROADER) {
                addStatement(vf.createStatement(vf.createIRI(id), SKOS.BROADER, vf.createIRI(generateUri(cleanLine))));
                addStatement(vf.createStatement(vf.createIRI(generateUri(cleanLine)), SKOS.NARROWER, vf.createIRI(id)));
            }

            lastLineType = currentLineType;

            System.out.println("\t" + line);
        }

        if(createConcept) {
            addStatement(vf.createStatement(vf.createIRI(id), RDF.TYPE, SKOS.CONCEPT));
            addStatement(vf.createStatement(vf.createIRI(id), SKOS.PREF_LABEL, vf.createLiteral(name)));
        }

    }

    public String cleanLine(String line) {
        line = line.trim();

        line = line.replace("OT1:", "").replace("OT2:", "").replace("OT3:", "").replace("OT4:", "").replace("OT5:", "");
        line = line.replace("UT1:", "").replace("UT2:", "").replace("UT3:", "").replace("UT4:", "").replace("UT5:", "");
        line = line.replace("SE:", "").replace("BF:", "").replace("SE OGSÅ:", "");

        line = line.trim();

        return line;
    }

    public LineType getLineType(String line) {
        line = line.trim();

        LineType lineType = LineType.UNKNOWN;

        if(line.startsWith("SE:") || line.startsWith("SE OGSÅ:"))
            lineType = LineType.SEE_OTHER;

        if(line.startsWith("BF:"))
            lineType = LineType.ALT_LABEL;

        if(line.startsWith("UT1:"))
            lineType = LineType.NARROWER;
        if(line.startsWith("UT2:"))
            lineType = LineType.IGNORE;
        if(line.startsWith("UT3:"))
            lineType = LineType.IGNORE;
        if(line.startsWith("UT4:"))
            lineType = LineType.IGNORE;
        if(line.startsWith("UT5:"))
            lineType = LineType.IGNORE;

        if(line.startsWith("OT1:"))
            lineType = LineType.BROADER;
        if(line.startsWith("OT2:"))
            lineType = LineType.IGNORE;
        if(line.startsWith("OT3:"))
            lineType = LineType.IGNORE;
        if(line.startsWith("OT4:"))
            lineType = LineType.IGNORE;
        if(line.startsWith("OT5:"))
            lineType = LineType.IGNORE;

        return lineType;
    }


    public void addStatement(Statement statement) {
        if(!allStatements.contains(statement)) {
            allStatements.add(statement);
        }
    }

    public String generateUri(String name) {
        String cleanedName = "";
        cleanedName = cleanedName.replace("å", "a").replace("Å", "å")
                        .replace("ø", "o").replace("Ø", "O")
                                .replace("æ", "æ").replace("Æ", "æ")
                                .replaceAll("[^\\w\\s]","").replace(" ", "").trim();

        String uri = baseUri + cleanedName;

        return uri;
    }

    public enum LineType {
        UNKNOWN, SEE_OTHER, NARROWER, BROADER, ALT_LABEL, IGNORE
    }


}
