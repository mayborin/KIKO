package cop5556sp17;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Scanner {

	/**
	 * Kind enum
	 */
	
	public static enum Kind {
		IDENT(""), INT_LIT(""), 
		KW_INTEGER("integer"), KW_BOOLEAN("boolean"), KW_IMAGE("image"), KW_URL("url"), KW_FILE("file"), 
		KW_FRAME("frame"), KW_WHILE("while"), KW_IF("if"), KW_TRUE("true"), KW_FALSE("false"), 
		SEMI(";"), COMMA(","), LPAREN("("), RPAREN(")"), LBRACE("{"), RBRACE("}"), 
		ARROW("->"), BARARROW("|->"), OR("|"), AND("&"), EQUAL("=="), NOTEQUAL("!="), LT("<"), GT(">"), 
		LE("<="), GE(">="), PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), MOD("%"), NOT("!"), ASSIGN("<-"), 
		OP_BLUR("blur"), OP_GRAY("gray"), OP_CONVOLVE("convolve"), 
		KW_SCREENHEIGHT("screenheight"), KW_SCREENWIDTH("screenwidth"), 
		OP_WIDTH("width"), OP_HEIGHT("height"), KW_XLOC("xloc"), KW_YLOC("yloc"), 
		KW_HIDE("hide"), KW_SHOW("show"), KW_MOVE("move"), OP_SLEEP("sleep"), 
		KW_SCALE("scale"), 
		EOF("eof");

		Kind(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
	}
	
	public static enum State{
		START, IDENTPART, DIGITS, NOT, LT, GT, HALFEQUAL, OR, HALFBARARROW, DIV, STARTCOMMENT, ENDCOMMENT, MINUS;
	}
	
	class TrieNode{
		TrieNode[] child;
		boolean isLeaf;
		Kind type;
		
		TrieNode(){
			this.child=new TrieNode[26];
			this.type=null;
			this.isLeaf=false;
		}
	}
	/*
	 * Use tire to store all the reserved keyword and provide function to check whether a IDENT is reserved key word, if true return it's Kind
	 */
	class Trie{
		TrieNode root;
		Trie(){
			root=new TrieNode();
		}
		
		public void add(Kind kind, String word){
			TrieNode ptr=root;
			for(char x:word.toCharArray()){
				if(ptr.child[x-'a']==null) ptr.child[x-'a']=new TrieNode();
				ptr=ptr.child[x-'a'];
			}
			ptr.isLeaf=true;
			ptr.type=kind;
		}
		
		public Kind search(String word){
			TrieNode ptr=root;
			for(char x: word.toCharArray()){
				int index=x-'a';
				if(index<0 || index>25 || ptr.child[index]==null) return null;
				ptr=ptr.child[index];
			}
			return ptr.isLeaf?ptr.type:null;
		}
	}

	/**
	 * Holds the line and position in the line of a token.
	 */
	static class LinePos {
		public final int line;
		public final int posInLine;
		
		public LinePos(int line, int posInLine) {
			super();
			this.line = line;
			this.posInLine = posInLine;
		}

		@Override
		public String toString() {
			return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
		}
	}
		
	public class Token {
		public final Kind kind;
		public final int pos;  //position in input array
		public final int length;  
		//returns the text of this Token
		public final String text;
		
		Token(Kind kind, int pos, int length) {
			this.kind = kind;
			this.pos = pos;
			this.length = length;
			this.text = chars.substring(pos,pos+length);
		}
		
		public String getText() {
			//TODO IMPLEMENT THIS
			return text;
		}
		
		//returns a LinePos object representing the line and column of this Token
		LinePos getLinePos(){
			//TODO IMPLEMENT THIS
			int insertIndex=Collections.binarySearch(newlineposition, pos);
			insertIndex=-(insertIndex+1);
			int posinline=insertIndex==0?pos:pos-newlineposition.get(insertIndex-1)-1;
			return new LinePos(insertIndex, posinline);
		}

		/** 
		 * Precondition:  kind = Kind.INT_LIT,  the text can be represented with a Java int.
		 * Note that the validity of the input should have been checked when the Token was created.
		 * So the exception should never be thrown.
		 * 
		 * @return  int value of this token, which should represent an INT_LIT
		 * @throws NumberFormatException
		 */
		public int intVal() throws NumberFormatException{
			//TODO IMPLEMENT THIS
			return Integer.parseInt(getText());
		}

		public boolean isKind(Kind k){
			return this.kind==k;
		}

		@Override
	  	public int hashCode() {
		   final int prime = 31;
		   int result = 1;
		   result = prime * result + getOuterType().hashCode();
		   result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		   result = prime * result + length;
		   result = prime * result + pos;
		   return result;
	  	}

	  	@Override
	  	public boolean equals(Object obj) {
		   if (this == obj) {
		    return true;
		   }
		   if (obj == null) {
		    return false;
		   }
		   if (!(obj instanceof Token)) {
		    return false;
		   }
		   Token other = (Token) obj;
		   if (!getOuterType().equals(other.getOuterType())) {
		    return false;
		   }
		   if (kind != other.kind) {
		    return false;
		   }
		   if (length != other.length) {
		    return false;
		   }
		   if (pos != other.pos) {
		    return false;
		   }
		   return true;
	  	}
	  	
	  	@Override
	  	public String toString(){
	  		return this.text;
	  	}
	  	private Scanner getOuterType() {
	   		return Scanner.this;
	  	}
		
	}//end of Token

	/**
	 * Thrown by Scanner when an illegal character is encountered
	 */
		@SuppressWarnings("serial")
		public static class IllegalCharException extends Exception {
			public IllegalCharException(String message) {
				super(message);
			}
		}
		
		/**
		 * Thrown by Scanner when an int literal is not a value that can be represented by an int.
		 */
		@SuppressWarnings("serial")
		public static class IllegalNumberException extends Exception {
		public IllegalNumberException(String message){
			super(message);
			}
		}
		
		
	final ArrayList<Token> tokens;
	final String chars;
	int tokenNum;
	final ArrayList<Integer> newlineposition;
	final Map<Character, Kind> uniqueOpe;
	Trie trie;
	 
	Scanner(String chars) {
		this.tokenNum = 0;
		this.chars = chars;
		tokens = new ArrayList<Token>();
		newlineposition=new ArrayList<Integer>();
		uniqueOpe=new HashMap<Character, Kind>();
		char[] uo={';',',','(',')','{','}','&','+','%','*'};
		Kind[] uok={Kind.SEMI,Kind.COMMA,Kind.LPAREN, Kind.RPAREN, Kind.LBRACE, Kind.RBRACE, Kind.AND, Kind.PLUS, Kind.MOD,Kind.TIMES};
		for(int i=0;i<uo.length;i++) uniqueOpe.put(uo[i], uok[i]);
		this.trie=new Trie();

		trie.add(Kind.KW_INTEGER, "integer");
		trie.add(Kind.KW_BOOLEAN,"boolean"); 
		trie.add(Kind.KW_IMAGE, "image"); 
		trie.add(Kind.KW_URL, "url"); 
		trie.add(Kind.KW_FILE, "file"); 
		trie.add(Kind.KW_FRAME, "frame"); 
		trie.add(Kind.KW_WHILE, "while"); 
		trie.add(Kind.KW_IF, "if");
		trie.add(Kind.KW_TRUE, "true"); 
		trie.add(Kind.KW_FALSE, "false"); 
		trie.add(Kind.OP_BLUR, "blur");
		trie.add(Kind.OP_GRAY, "gray"); 
		trie.add(Kind.OP_CONVOLVE, "convolve");
		trie.add(Kind.KW_SCREENHEIGHT, "screenheight");
		trie.add(Kind.KW_SCREENWIDTH, "screenwidth");
		trie.add(Kind.OP_WIDTH, "width");
		trie.add(Kind.OP_HEIGHT, "height"); 
		trie.add(Kind.KW_XLOC, "xloc");
		trie.add(Kind.KW_YLOC, "yloc"); 
		trie.add(Kind.KW_HIDE, "hide"); 
		trie.add(Kind.KW_SHOW, "show");
		trie.add(Kind.KW_MOVE, "move");
		trie.add(Kind.OP_SLEEP, "sleep"); 
		trie.add(Kind.KW_SCALE, "scale"); 
	}


	
	/**
	 * Initializes Scanner object by traversing chars and adding tokens to tokens list.
	 * 
	 * @return this scanner
	 * @throws IllegalCharException
	 * @throws IllegalNumberException
	 */
	public Scanner scan() throws IllegalCharException, IllegalNumberException { 
		//TODO IMPLEMENT THIS!!!!
		int len=chars.length();
		int ptr=0;
		int startptr=0;
		State state=State.START;
		ptr=skipWhiteSpace(ptr);
		startptr=ptr;
		while(ptr<len){
			char x=chars.charAt(ptr);
			switch(state){
				case START:{
					switch(x){
						case '0': tokens.add(new Token(Kind.INT_LIT, ptr, 1));break;
						case '!': state=State.NOT;break;
						case '/': state=State.DIV;break;
						case '<': state=State.LT;break;
						case '>': state=State.GT;break;
						case '=': state=State.HALFEQUAL;break;
						case '-': state=State.MINUS;break;
						case '|': state=State.OR;break;
						default:{
							if(uniqueOpe.containsKey(x)){
								tokens.add(new Token(uniqueOpe.get(x), ptr, 1));
							}else if(Character.isDigit(x)){
								state=State.DIGITS;
							}else if(Character.isJavaIdentifierStart(x)){
								state=State.IDENTPART;
							}else{
								throw new IllegalCharException( "illegal char " + x +" at pos "+ptr);
							}
						}
					}
					ptr++;
				}break;
				case DIV:{
					if(x=='*') {
						state=State.STARTCOMMENT;
						ptr++;
					}
					else{
						state=State.START;
						tokens.add(new Token(Kind.DIV, ptr-1, 1));
					}
				}break;
				case STARTCOMMENT:{
					if(x=='*') state=State.ENDCOMMENT;
					ptr++;
				}break;
				case ENDCOMMENT:{
					if(x=='/') {
						ptr++;
						state=State.START;
					}
					else state=State.STARTCOMMENT;
				}break;
				case NOT:{
					if(x=='=') {
						tokens.add(new Token(Kind.NOTEQUAL, ptr-1,2));
						ptr++;
					}
					else	 tokens.add(new Token(Kind.NOT, ptr-1, 1));
					state=State.START;
				}break;
				case LT:{
					if(x=='=') {
						tokens.add(new Token(Kind.LE, ptr-1, 2));
						ptr++;
					}
					else if(x=='-') {
						tokens.add(new Token(Kind.ASSIGN, ptr-1, 2));
						ptr++;
					}else tokens.add(new Token(Kind.LT, ptr-1, 1));
					state=State.START;
				}break;
				case GT:{
					if(x=='='){
						tokens.add(new Token(Kind.GE, ptr-1, 2));
						ptr++;
					}else tokens.add(new Token(Kind.GT, ptr-1, 1));
					state=State.START;
				}break;
				case HALFEQUAL:{
					if(x=='='){
						tokens.add(new Token(Kind.EQUAL, ptr-1, 2));
						ptr++;
						state=State.START;
					}else throw new IllegalCharException( "illegal char " + x +" at pos "+ptr);
				}break;
				case OR:{
					if(x=='-'){
						state=State.HALFBARARROW;
						ptr++;
					}else{
						tokens.add(new Token(Kind.OR, ptr-1, 1));
						state=State.START;
					}
				}break;
				case HALFBARARROW:{
					if(x=='>'){
						tokens.add(new Token(Kind.BARARROW, ptr-2, 3));
						ptr++;
					}else{
						tokens.add(new Token(Kind.OR, ptr-2, 1));
						tokens.add(new Token(Kind.MINUS, ptr-1, 1));
					}
					state=State.START;
				}break;
				case DIGITS:{
					if(Character.isDigit(x)) ptr++;
					else{
						String number=chars.substring(startptr,ptr);
						if(checkNumber(number)){
							tokens.add(new Token(Kind.INT_LIT, startptr, ptr-startptr));
							state=State.START;
						}else throw new IllegalNumberException( "Number start at "+startptr+" is out of Integer range");
					}
				}break;
				case MINUS:{
					if(x=='>'){
						tokens.add(new Token(Kind.ARROW, ptr-1, 2));
						ptr++;
					}else{
						tokens.add(new Token(Kind.MINUS, ptr-1, 1));
					}
					state=State.START;
				}break;
				case IDENTPART:{
					if(Character.isJavaIdentifierPart(x)) ptr++;
					else{
						state=State.START;
						String identifier=chars.substring(startptr,ptr);
						Kind type=trie.search(identifier);
						if(type==null) tokens.add(new Token(Kind.IDENT, startptr, ptr-startptr));
						else tokens.add(new Token(type, startptr, ptr-startptr));
					}
				}break;
				default:  assert false;
			}
			if(state==State.START) {
				ptr=skipWhiteSpace(ptr);
				startptr=ptr;
			}
		}
		switch(state){
			case START:break;
			case IDENTPART:{
				String identifier=chars.substring(startptr,ptr);
				Kind type=trie.search(identifier);
				if(type==null) tokens.add(new Token(Kind.IDENT, startptr, ptr-startptr));
				else tokens.add(new Token(type, startptr, ptr-startptr));
			}break;
			case DIGITS:{
				String number=chars.substring(startptr,ptr);
				if(checkNumber(number)){
					tokens.add(new Token(Kind.INT_LIT, startptr, ptr-startptr));
					state=State.START;
				}else throw new IllegalNumberException( "Number start at "+startptr+" is out of Integer range");
			}break;
			case NOT: tokens.add(new Token(Kind.NOT, ptr-1, 1));break; 
			case LT: tokens.add(new Token(Kind.LT, ptr-1,1 )); break;
			case GT: tokens.add(new Token(Kind.GT, ptr-1, 1)); break;
			case HALFEQUAL:throw new IllegalCharException( "illegal char start at pos " + chars.charAt(startptr) +" at pos "+startptr);
			case OR: tokens.add(new Token(Kind.OR, ptr-1, 1)); break;
			case HALFBARARROW:throw new IllegalCharException( "illegal char start at pos "+startptr);
			case DIV: tokens.add(new Token(Kind.DIV, ptr-1, 1)); break;
			case STARTCOMMENT:throw new IllegalCharException( "Unclosed comment starting at "+startptr);
			case ENDCOMMENT:throw new IllegalCharException( "unclosed comment starting at "+startptr);
			case MINUS: tokens.add(new Token(Kind.MINUS, ptr-1, 1));break;
		}
		tokens.add(new Token(Kind.EOF,ptr,0));
		return this;  
	}
	public boolean checkNumber(String number){
		if(number.length()>10) return false;
		else{
			long value=Long.parseLong(number);
			if(value>Integer.MAX_VALUE) return false;
			return true;
		}
	}
	public int skipWhiteSpace(int ptr){
		int len=chars.length();
		while(ptr<len){
			char x=chars.charAt(ptr);
			if(Character.isWhitespace(x)){
				if(x=='\n') newlineposition.add(ptr);
				ptr++;
			}else break;
		}
		return ptr;
	}

	/*
	 * Return the next token in the token list and update the state so that
	 * the next call will return the Token..  
	 */
	public Token nextToken() {
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum++);
	}
	
	/*
	 * Return the next token in the token list without updating the state.
	 * (So the following call to next will return the same token.)
	 */
	public Token peek(){
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum);		
	}
	

	/**
	 * Returns a LinePos object containing the line and position in line of the 
	 * given token.  
	 * 
	 * Line numbers start counting at 0
	 * 
	 * @param t
	 * @return
	 */
	public LinePos getLinePos(Token t) {
		//TODO IMPLEMENT THIS
		return t.getLinePos();
	}


}
