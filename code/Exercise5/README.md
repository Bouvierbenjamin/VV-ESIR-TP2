# Exercice 5

We didn't suceed to do the graph of each class but we generated the ".dot". It is possible eto convert it in svg with ``` dot -Tsvg input.dot -o output.svg ``` 



## Tcc.java

```java

package fr.istic.vv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults;

import org.apache.commons.io.FileUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

// This class visits a compilation unit and
// prints all public enum, classes or interfaces along with their public methods
public class TCC extends VoidVisitorWithDefaults<Void> {

	
    @Override
    public void visit(CompilationUnit unit, Void arg) {
    	
        for(TypeDeclaration<?> type : unit.getTypes()) {
            type.accept(this, null);
        }
        System.out.println("test");
    }

    //Lecture récusive de code pour analyse de body ou d'autres expressions. 
    public void recursExpr(Node s, ArrayList<String> arrNames, ArrayList<String> arrMethod) {
    	
    	String[] classNameLong = s.getClass().toString().split("[.]");
		String className = classNameLong[(classNameLong.length)-1].toLowerCase();
		
		// Si c'est un nom d'expression ou un simpleName
    	if(className.equals("nameexpr") || className.equals("simplename")) {
    		arrNames.add(s.toString());
    		return;
    	}
    	
    	// Si c'est une méthode
    	if(className.equals("methodcallexpr")) {
    		if(arrMethod != null) {
	    		String AppelMethod =  s.toString().split("[(]")[0];
	    		String[] AppelMethodArr = AppelMethod.split("[.]");
	    		String nomMethod = AppelMethodArr[AppelMethodArr.length-1];
	    		arrMethod.add(nomMethod);
    		}
    	}
    	
    	// Pour chaque enfants du noeud
    	for(Node n: s.getChildNodes()) {
			recursExpr(n,arrNames,arrMethod);
		}
    	
    }
    
    public void visitTypeDeclaration(TypeDeclaration<?> declaration, Void arg) {
    	// Si la classe est publique
        if(!declaration.isPublic()) return;
        String content = "";
        
        content += "\n*********************  "+declaration.getNameAsString()+"  *****************\n\n";
        
        // Tableau des champs 
        ArrayList<String> fields = new ArrayList<>();
        // Tableau des métodes
        ArrayList<String> methods = new ArrayList<>();
        // map des liaisons méthodes(key) - attribut(values)
        Map <String,ArrayList<String>> hashMethodAttrs = new HashMap<>(); 
        // map des liaisons méthodes(key) - méthodes(values)
        Map <String,ArrayList<String>> hashMethodMethod = new HashMap<>(); 
        
        //Pour chaque variable de la classe
        for(FieldDeclaration field : declaration.getFields()) {
        	field.accept(this, arg);
        	//On les sauvegarde dans fields
        	recursExpr(field,fields,null);
        }
        
        //Pour chaque methodes de la classe
        for(MethodDeclaration method : declaration.getMethods()) {
            method.accept(this, arg);
            //on les sauvegardes dans methods
            methods.add(method.getNameAsString());
        }
        
        //Pour chaque méthodes de la classe --> Optimisable donc, mais plus simple à la compréhension
        for(MethodDeclaration method : declaration.getMethods()) {
        	//on va récupérer les attributs et les méthods utilisé dans le body de la méthode
    		ArrayList<String> tmpFields = new ArrayList<>();
            ArrayList<String> tmpMethods = new ArrayList<>();
            //s'il y a un body
            if(method.getBody().isPresent()) {
            	//pour chaque parties du body
	        	for (Statement n : method.getBody().get().getStatements()) {	                
	        		recursExpr(n,tmpFields,tmpMethods);
	        	}
	        	
	        	//Vérification si ce sont des attributs ou des méthodes de la classe.
	        	Iterator<String> iter = tmpFields.iterator();
	        	while(iter.hasNext()) {
	        		String possiblefield = iter.next();
	        		if(!fields.contains(possiblefield)) {
	        			iter.remove();
	        		}
	        	}
	        	Iterator<String> iter2 = tmpMethods.iterator();
	        	while(iter2.hasNext()) {
	        		String possiblemethod = iter2.next();
	        		if(!methods.contains(possiblemethod)) {
	        			iter2.remove();
	        		}
	        	}
            }
    		
	        	
            //On ajoute cette méthode aux map
    		hashMethodAttrs.put(method.getNameAsString().toString(),tmpFields);
    		hashMethodMethod.put(method.getNameAsString().toString(),tmpMethods);
        }
        content+="Variable trouvé : ";
        content += hashMethodAttrs.toString() +"\n";    
        content+="Méthode trouvé : " ;    
        content+=hashMethodMethod +"\n";
        

		// détection des liasions attribut entre méthodes
		Map <String, Set<String>> liaisons = new HashMap<>(); 
		for (String key : hashMethodAttrs.keySet()) {
			liaisons.put(key,new HashSet<>());
			for(String attr : hashMethodAttrs.get(key)) {
				for(String key2 : hashMethodAttrs.keySet()) {
					if( !key.equals(key2) && !liaisons.get(key).contains(key2)) {
						if(!(liaisons.containsKey(key2) && liaisons.get(key2).contains(key)))
							if(hashMethodAttrs.get(key2).contains(attr)) {
								liaisons.get(key).add(key2);
							}
					}
				}
			}
		}
		
		//J'avais pris les méthodes pour calculer le LCC mais ce n'était pas à faire
		
		//TODO détection liaisons méthodes
		
		
		content+="attribut de liaison trouvé : " ; 
		content+=liaisons+"\n";
		
		
		//Calcul TCC + dotCode
		Set<String> tmpmethods = new HashSet<>();
		float res =0;
		String dotFormat="";
		for (String key : liaisons.keySet()) {
			for(String key2 : liaisons.get(key) ) {
				tmpmethods.add(key);
				tmpmethods.add(key2);
				dotFormat+=key +" -- "+key2+";";
				res += 1;
			}
		}
		
		for(String method : methods) {
			if(!tmpmethods.contains(method)) {
				dotFormat+=method+";";
			}
		}
		
		int nbKey = liaisons.keySet().size();
		int denominateur = (nbKey*(nbKey-1))/2;
		
		if(denominateur>0) {
			res/=denominateur;
		}
		
			
		content +="TCC de : " + res;
		
		//creation .do, commande unix pour le générer : dot -Tsvg input.dot -o output.svg
        createDotGraph(dotFormat, declaration.getNameAsString());
        
        //création rapport
        String path = "./reportTcc.txt";
		
        try {
            Files.write(Paths.get(path), content.getBytes(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }

 
    }

    //Pas réussi à avoir les PDF
	public static void createDotGraph(String dotFormat,String className)
	{
		GraphViz gv=new GraphViz();
		gv.addln(gv.start_graph());
		gv.add(dotFormat);
		gv.addln(gv.end_graph());
		String type = "pdf";
		gv.decreaseDpi();
		gv.decreaseDpi();
		gv.getGraph( gv.getDotSource(), type, className );
		/*String type = "pdf";
		File out = new File(fileName+"."+ type); 
		gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );*/
	}
    
    
   @Override
    public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
        visitTypeDeclaration(declaration, arg);
    }

    @Override
    public void visit(EnumDeclaration declaration, Void arg) {
        visitTypeDeclaration(declaration, arg);
    }

}


```

## GraphViz

```java

package fr.istic.vv;

// GraphViz.java - a simple API to call dot from Java programs

/*$Id$*/
/*
 ******************************************************************************
 *                                                                            *
 *                    (c) Copyright Laszlo Szathmary                          *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms of the GNU Lesser General Public License as published by   *
 * the Free Software Foundation; either version 2.1 of the License, or        *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public    *
 * License for more details.                                                  *
 *                                                                            *
 * You should have received a copy of the GNU Lesser General Public License   *
 * along with this program; if not, write to the Free Software Foundation,    *
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.                              *
 *                                                                            *
 ******************************************************************************
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * <dl>
 * <dt>Purpose: GraphViz Java API
 * <dd>
 *
 * <dt>Description:
 * <dd> With this Java class you can simply call dot
 *      from your Java programs.
 * <dt>Example usage:
 * <dd>
 * <pre>
 *    GraphViz gv = new GraphViz();
 *    gv.addln(gv.start_graph());
 *    gv.addln("A -> B;");
 *    gv.addln("A -> C;");
 *    gv.addln(gv.end_graph());
 *    System.out.println(gv.getDotSource());
 *
 *    String type = "gif";
 *    File out = new File("out." + type);   // out.gif in this example
 *    gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );
 * </pre>
 * </dd>
 *
 * </dl>
 *
 * @version v0.5.1, 2013/03/18 (March) -- Patch of Juan Hoyos (Mac support)
 * @version v0.5, 2012/04/24 (April) -- Patch of Abdur Rahman (OS detection + start subgraph + 
 * read config file)
 * @version v0.4, 2011/02/05 (February) -- Patch of Keheliya Gallaba is added. Now you
 * can specify the type of the output file: gif, dot, fig, pdf, ps, svg, png, etc.
 * @version v0.3, 2010/11/29 (November) -- Windows support + ability to read the graph from a text file
 * @version v0.2, 2010/07/22 (July) -- bug fix
 * @version v0.1, 2003/12/04 (December) -- first release
 * @author  Laszlo Szathmary (<a href="jabba.laci@gmail.com">jabba.laci@gmail.com</a>)
 */
public class GraphViz
{
    /**
     * Detects the client's operating system.
     */
    private final static String osName = System.getProperty("os.name").replaceAll("\\s","");

    /**
     * Load the config.properties file.
     */
    //private final static String cfgProp = "/private/student/r/er/bebouvier/mdi/VV-ESIR-TP2/code/javaparser-starter/src/main/java/fr/istic/vv/config.properties";
    private final static String cfgProp = "/home/benj/Bureau/mdi/VV-ESIR-TP2/code/javaparser-starter/src/main/java/fr/istic/vv/config.properties";
    private final static Properties configFile = new Properties() {
        private final static long serialVersionUID = 1L; {
            try {
                load(new FileInputStream(cfgProp));
            } catch (Exception e) {}
        }
    };

    /**
     * The dir. where temporary files will be created.
     */
  //private static String TEMP_DIR = "/private/student/r/er/bebouvier/mdi/VV-ESIR-TP2/code/javaparser-starter";
    private static String TEMP_DIR = "/home/benj/Bureau/mdi/VV-ESIR-TP2/code/javaparser-starter";
    /**
     * Where is your dot program located? It will be called externally.
     */
  private static String DOT = configFile.getProperty("dotFor" + osName);

    /**
     * The image size in dpi. 96 dpi is normal size. Higher values are 10% higher each.
     * Lower values 10% lower each.
     * 
     * dpi patch by Peter Mueller
     */
    private int[] dpiSizes = {46, 51, 57, 63, 70, 78, 86, 96, 106, 116, 128, 141, 155, 170, 187, 206, 226, 249};

    /**
     * Define the index in the image size array.
     */
    private int currentDpiPos = 7;

    /**
     * Increase the image size (dpi).
     */
    public void increaseDpi() {
        if ( this.currentDpiPos < (this.dpiSizes.length - 1) ) {
            ++this.currentDpiPos;
        }
    }

    /**
     * Decrease the image size (dpi).
     */
    public void decreaseDpi() {
        if (this.currentDpiPos > 0) {
            --this.currentDpiPos;
        }
    }

    public int getImageDpi() {
        return this.dpiSizes[this.currentDpiPos];
    }

    /**
     * The source of the graph written in dot language.
     */
    private StringBuilder graph = new StringBuilder();

    /**
     * Constructor: creates a new GraphViz object that will contain
     * a graph.
     */
    public GraphViz() {
    }

    /**
     * Returns the graph's source description in dot language.
     * @return Source of the graph in dot language.
     */
    public String getDotSource() {
        return this.graph.toString();
    }

    /**
     * Adds a string to the graph's source (without newline).
     */
    public void add(String line) {
        this.graph.append(line);
    }

    /**
     * Adds a string to the graph's source (with newline).
     */
    public void addln(String line) {
        this.graph.append(line + "\n");
    }

    /**
     * Adds a newline to the graph's source.
     */
    public void addln() {
        this.graph.append('\n');
    }

    public void clearGraph(){
        this.graph = new StringBuilder();
    }

    /**
     * Returns the graph as an image in binary format.
     * @param dot_source Source of the graph to be drawn.
     * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
     * @return A byte array containing the image of the graph.
     */
    public byte[] getGraph(String dot_source, String type)
    {
        File dot;
        byte[] img_stream = null;

        try {
            dot = writeDotSourceToFile(dot_source);
            if (dot != null)
            {
                img_stream = get_img_stream(dot, type);
                if (dot.delete() == false) 
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                return img_stream;
            }
            return null;
        } catch (java.io.IOException ioe) { return null; }
    }
    
    /**
     * Returns the graph as an image in binary format.
     * @param dot_source Source of the graph to be drawn.
     * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
     * @param name filename of the dot source.
     * @return A byte array containing the image of the graph.
     */
    public byte[] getGraph(String dot_source, String type, String name)
    {
        File dot;
        byte[] img_stream = null;

        try {
            dot = writeDotSourceToFile(dot_source,name);
            /*if (dot != null)
            {
                img_stream = get_img_stream(dot, type);
                if (dot.delete() == false) 
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                return img_stream;
            }*/
            return null;
        } catch (java.io.IOException ioe) { return null; }
    }

    /**
     * Writes the graph's image in a file.
     * @param img   A byte array containing the image of the graph.
     * @param file  Name of the file to where we want to write.
     * @return Success: 1, Failure: -1
     */
    public int writeGraphToFile(byte[] img, String file)
    {
        File to = new File(file);
        return writeGraphToFile(img, to);
    }

    /**
     * Writes the graph's image in a file.
     * @param img   A byte array containing the image of the graph.
     * @param to    A File object to where we want to write.
     * @return Success: 1, Failure: -1
     */
    public int writeGraphToFile(byte[] img, File to)
    {
        try {
            FileOutputStream fos = new FileOutputStream(to);
            fos.write(img);
            fos.close();
        } catch (java.io.IOException ioe) { return -1; }
        return 1;
    }

    /**
     * It will call the external dot program, and return the image in
     * binary format.
     * @param dot Source of the graph (in dot language).
     * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
     * @return The image of the graph in .gif format.
     */
    private byte[] get_img_stream(File dot, String type)
    {
        File img;
        byte[] img_stream = null;

        try {
            img = File.createTempFile("graph_", "."+type, new File(GraphViz.TEMP_DIR));
            Runtime rt = Runtime.getRuntime();

            // patch by Mike Chenault
            String[] args = {DOT, "-T"+type, "-Gdpi="+dpiSizes[this.currentDpiPos], dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
            Process p = rt.exec(args);

            p.waitFor();

            FileInputStream in = new FileInputStream(img.getAbsolutePath());
            img_stream = new byte[in.available()];
            in.read(img_stream);
            // Close it if we need to
            if( in != null ) in.close();

            if (img.delete() == false) 
                System.err.println("Warning: " + img.getAbsolutePath() + " could not be deleted!");
        }
        catch (java.io.IOException ioe) {
            System.err.println("Error:    in I/O processing of tempfile in dir " + GraphViz.TEMP_DIR+"\n");
            System.err.println("       or in calling external command");
            ioe.printStackTrace();
        }
        catch (java.lang.InterruptedException ie) {
            System.err.println("Error: the execution of the external program was interrupted");
            ie.printStackTrace();
        }

        return img_stream;
    }

    /**
     * Writes the source of the graph in a file, and returns the written file
     * as a File object.
     * @param str Source of the graph (in dot language).
     * @return The file (as a File object) that contains the source of the graph.
     */
    private File writeDotSourceToFile(String str) throws java.io.IOException
    {
        File temp;
        try {
            temp = File.createTempFile("dorrr",".dot", new File(GraphViz.TEMP_DIR));
            FileWriter fout = new FileWriter(temp);
            fout.write(str);
                       BufferedWriter br=new BufferedWriter(new FileWriter("dotsource.dot"));
                       br.write(str);
                       br.flush();
                       br.close();
            /*fout.close();*/
        }
        catch (Exception e) {
            System.err.println("Error: I/O error while writing the dot source to temp file!");
            return null;
        }
        return temp;
    }
    
    /**
     * Writes the source of the graph in a file, and returns the written file
     * as a File object.
     * @param str Source of the graph (in dot language).
     * @param name filename
     * @return The file (as a File object) that contains the source of the graph.
     */
    private File writeDotSourceToFile(String str, String name) throws java.io.IOException
    {
        File temp;
        try {
        	/*temp = File.createTempFile("dorrr",".dot", new File(GraphViz.TEMP_DIR));
            FileWriter fout = new FileWriter(temp);
            fout.write(str);*/
                       BufferedWriter br=new BufferedWriter(new FileWriter(name + ".dot"));
                       br.write(str);
                       br.flush();
                       br.close();
            //fout.close();
        }
        catch (Exception e) {
            System.err.println("Error: I/O error while writing the dot source to temp file!");
            return null;
        }
        return null;
    }

    /**
     * Returns a string that is used to start a graph.
     * @return A string to open a graph.
     */
    public String start_graph() {
        return "digraph G {";
    }

    /**
     * Returns a string that is used to end a graph.
     * @return A string to close a graph.
     */
    public String end_graph() {
        return "}";
    }

    /**
     * Takes the cluster or subgraph id as input parameter and returns a string
     * that is used to start a subgraph.
     * @return A string to open a subgraph.
     */
    public String start_subgraph(int clusterid) {
        return "subgraph cluster_" + clusterid + " {";
    }

    /**
     * Returns a string that is used to end a graph.
     * @return A string to close a graph.
     */
    public String end_subgraph() {
        return "}";
    }

    /**
     * Read a DOT graph from a text file.
     * 
     * @param input Input text file containing the DOT graph
     * source.
     */
    public void readSource(String input)
    {
        StringBuilder sb = new StringBuilder();

        try
        {
            FileInputStream fis = new FileInputStream(input);
            DataInputStream dis = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dis));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            dis.close();
        } 
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        this.graph = sb;
    }

} // end of class GraphViz

```


