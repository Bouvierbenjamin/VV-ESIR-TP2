package fr.istic.vv;

import java.io.File;
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


// This class visits a compilation unit and
// prints all public enum, classes or interfaces along with their public methods
public class TCC extends VoidVisitorWithDefaults<Void> {

	
    @Override
    public void visit(CompilationUnit unit, Void arg) {
    	
        for(TypeDeclaration<?> type : unit.getTypes()) {
            type.accept(this, null);
        }
    }
//TODO FieldAccessExpr
    public void recursExpr(Node s, ArrayList<String> arrNames, ArrayList<String> arrMethod) {
    	
    	String[] classNameLong = s.getClass().toString().split("[.]");
		String className = classNameLong[(classNameLong.length)-1].toLowerCase();
    	
		
		
    	/*if(className.equals("returnstmt") || className.equals("express")) {
			System.out.println(s.getChildNodes().get(0).getClass());
		}*/
		
		
    	if(className.equals("nameexpr") || className.equals("simplename")) {
    		arrNames.add(s.toString());
    		return;
    	}
    		
    	if(className.equals("methodcallexpr")) {
    		String AppelMethod =  s.toString().split("[(]")[0];
    		String[] AppelMethodArr = AppelMethod.split("[.]");
    		String nomMethod = AppelMethodArr[AppelMethodArr.length-1];
    		arrMethod.add(nomMethod);
    	}
    	
    	for(Node n: s.getChildNodes()) {
			recursExpr(n,arrNames,arrMethod);
		}
    	
    }
    
    public void visitTypeDeclaration(TypeDeclaration<?> declaration, Void arg) {
    	if(declaration.getNameAsString().equals("TestPrint")) {
	        if(!declaration.isPublic()) return;
	        System.out.println("\n*********************  "+declaration.getNameAsString()+"  *****************");
	
	        ArrayList<String> fields = new ArrayList<>();
	        ArrayList<String> methods = new ArrayList<>();
	        Map <String,ArrayList<String>> hashMethodAttrs = new HashMap<>(); 
	        Map <String,ArrayList<String>> hashMethodMethod = new HashMap<>(); 
	        
	        for(FieldDeclaration field : declaration.getFields()) {
	        	field.accept(this, arg);
	        	//System.out.println(field.getChildNodes());
	        	recursExpr(field,fields,null);
	        }
	        
	        for(MethodDeclaration method : declaration.getMethods()) {
	            method.accept(this, arg);
	            methods.add(method.getNameAsString());
	        }
	        
	        
	        for(MethodDeclaration method : declaration.getMethods()) {
	            
	    		ArrayList<String> tmpFields = new ArrayList<>();
	            ArrayList<String> tmpMethods = new ArrayList<>();
	            
	        	for (Statement n : method.getBody().get().getStatements()) {
	        		//System.out.println(n.getClass());
	                
	        		recursExpr(n,tmpFields,tmpMethods);
	        		/*String[] classNameLong = n.getClass().toString().split("[.]");
	        		String className = classNameLong[(classNameLong.length)-1].toLowerCase();*/
	        		/*if(className.equals("returnstmt")) {
	        			System.out.println(n.getChildNodes().get(0).getClass());
	        		}*/
	        	}
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
	
	    		
		        	
	
	    		hashMethodAttrs.put(method.getNameAsString().toString(),tmpFields);
	    		hashMethodMethod.put(method.getNameAsString().toString(),tmpMethods);
	        }
	        
	        System.out.println(hashMethodAttrs);        
			System.out.println(hashMethodMethod);
	        
			/*TODO détecter les return  à partir des méthodes appelé*/
			
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
			System.out.println(liaisons);
			int res =0;
			String dotFormat="";
			for (String key : liaisons.keySet()) {
				for(String key2 : liaisons.get(key) ) {
					dotFormat+=key +" -- "+key2+";";
					res += 1;
				}
			}
			
				
			System.out.println( "TCC de :" + res );
			
			System.out.println( "Code dot :" + dotFormat );
	        createDotGraph(dotFormat, "DotGraph");
    	}
        
 
    }

	public static void createDotGraph(String dotFormat,String fileName)
	{
		GraphViz gv=new GraphViz();
		gv.addln(gv.start_graph());
		gv.add(dotFormat);
		gv.addln(gv.end_graph());
	// String type = "gif";
		String type = "pdf";
	// gv.increaseDpi();
		gv.decreaseDpi();
		gv.decreaseDpi();
		File out = new File(fileName+"."+ type); 
		gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );
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
