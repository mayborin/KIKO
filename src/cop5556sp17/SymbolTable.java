package cop5556sp17;



import cop5556sp17.AST.Dec;
import java.util.*;

public class SymbolTable {
	//create a class to store the context of an Ident
	private class Context{
		int scopeId;
		Dec dec;
		Context(int scopeId, Dec dec){
			this.scopeId = scopeId;
			this.dec = dec;
		}
		@Override
		public String toString(){
			StringBuilder res = new StringBuilder();
			res.append(scopeId);
			res.append(", Value=");
			res.append(dec.toString());
			return res.toString();
		}
	}
	//TODO  add fields
	private int currentScope;
	private int nextScope;
	private LinkedList<Integer> scopes;
	private HashMap<String, LinkedList<Context>> symbolTable;

	public SymbolTable() {
		//TODO:  IMPLEMENT THIS
		this.currentScope = 0;
		this.nextScope = 0;
		scopes =  new LinkedList<Integer>();
		symbolTable = new HashMap<String, LinkedList<Context>>();
		enterScope();
	}
	
	/** 
	 * to be called when block entered
	 */
	public void enterScope(){
		//TODO:  IMPLEMENT THIS
		currentScope = nextScope++;
		scopes.push(currentScope);
	}
	
	
	/**
	 * leaves scope
	 */
	public void leaveScope(){
		//TODO:  IMPLEMENT THIS
		scopes.pop();
		currentScope = scopes.peek();
	}
	
	public boolean insert(String ident, Dec dec){
		//TODO:  IMPLEMENT THIS
		if(!symbolTable.containsKey(ident)){
			symbolTable.put(ident, new LinkedList<Context>());
		}else{
			if(symbolTable.get(ident).peek().scopeId==currentScope) return false;
		}
		symbolTable.get(ident).offerFirst(new Context(currentScope, dec));
		return true;
	}
	
	public Dec lookup(String ident){
		//TODO:  IMPLEMENT THIS
		if(!symbolTable.containsKey(ident)) return null;
		Set<Integer> nestedScope = new HashSet<Integer>(scopes);
		Iterator<Context> iterator = symbolTable.get(ident).iterator();
		while(iterator.hasNext()){
			Context tmp = iterator.next();
			if(tmp.scopeId <= currentScope && nestedScope.contains(tmp.scopeId)) return tmp.dec;
		}
		return null;
	}


	@Override
	public String toString() {
		//TODO:  IMPLEMENT THIS
		StringBuilder res = new StringBuilder();
		res.append("Symbol Table :\n");
		for(String key : symbolTable.keySet()){
			res.append(key+" ::");
			LinkedList<Context> list = symbolTable.get(key);
			for(Context context : list){
				res.append("\t["+context.toString()+"]\n");
			}
		}
		return res.toString();
	}
}
