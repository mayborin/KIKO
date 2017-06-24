package cop5556sp17;

import cop5556sp17.Scanner.Kind;
import static cop5556sp17.Scanner.Kind.*;
import cop5556sp17.Scanner.Token;
import java.util.*;
import cop5556sp17.AST.*;

public class Parser {

	/**
	 * Exception to be thrown if a syntax error is detected in the input.
	 * You will want to provide a useful error message.
	 *
	 */
	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		public SyntaxException(String message) {
			super(message);
		}
	}

	Scanner scanner;
	Token t;
	Set<Kind> deckind;
	Set<Kind> statementkind;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
		deckind=new HashSet<Kind>();
		deckind.add(KW_INTEGER); 
		deckind.add(KW_BOOLEAN); 
		deckind.add(KW_IMAGE); 
		deckind.add(KW_FRAME);
		Kind[] statementkinds={OP_SLEEP, KW_WHILE, KW_IF, IDENT, OP_BLUR,OP_GRAY,OP_CONVOLVE,
			KW_SHOW ,KW_HIDE,KW_MOVE, KW_XLOC,KW_YLOC, OP_WIDTH,OP_HEIGHT,KW_SCALE};
		statementkind=new HashSet<Kind>(Arrays.asList(statementkinds));
	}

	/**
	 * parse the input using tokens from the scanner.
	 * Check for EOF (i.e. no trailing junk) when finished
	 * 
	 * @throws SyntaxException
	 */
	ASTNode parse() throws SyntaxException {
	    Program prog=program();
		matchEOF();
		return prog;
	}

	Program program() throws SyntaxException {
		ArrayList<ParamDec> _paramDec=new ArrayList<>();
		Block _block=null;
		Token _firstToken = match(IDENT);
		Kind kind=t.kind;
		switch(kind){
			case LBRACE:{
				_block=block();
			}break;
			case KW_URL:
			case KW_FILE: 
			case KW_INTEGER: 
			case KW_BOOLEAN:{
				_paramDec.add(paramDec());
				while(t.isKind(COMMA)){
					consume();
					_paramDec.add(paramDec());
				}
				_block=block();
			}break;
			default:
			        throw new SyntaxException("illegal factor");
		}
		return new Program(_firstToken, _paramDec, _block);
	}

	ParamDec paramDec() throws SyntaxException {
		Token _type = match(KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN);
		Token _ident = match(IDENT);
		return new ParamDec(_type, _ident);
	}


	Dec dec() throws SyntaxException {
		Token _type = match( KW_INTEGER, KW_BOOLEAN , KW_IMAGE , KW_FRAME);
		Token _ident = match(IDENT);
		return new Dec(_type, _ident);
	}

	Statement statement() throws SyntaxException {
		Kind kind=t.kind;
		Token _firstToken = t;
		switch(kind){
			case OP_SLEEP:{
			    consume();
				Expression _expression=expression();
				match(SEMI);
				return new SleepStatement(_firstToken, _expression);
			}
			case KW_WHILE:{
				consume();
				match(LPAREN);
				Expression _expression = expression();
				match(RPAREN);
				Block _block = block();
				return new WhileStatement(_firstToken, _expression, _block);
			}
			case KW_IF:{
				consume();
				match(LPAREN);
				Expression _expression = expression();
				match(RPAREN);
				Block _block = block();
				return new IfStatement(_firstToken, _expression, _block);
			}
			case IDENT:{
				Kind following=scanner.peek().kind;//use the following token to determine 
				if(following==ASSIGN){
					AssignmentStatement _assign = assign();
					match(SEMI);
					return _assign;
				}else{
					Chain _chain = chain();
					match(SEMI);
					return _chain;
				}
				
			}
			default:{
				Chain _chain = chain();
				match(SEMI);
				return _chain;
			}
		}
	}

	AssignmentStatement assign() throws SyntaxException{
		Token _firstToken = match(IDENT);
		match(ASSIGN);
		Expression _expression = expression();
		return new AssignmentStatement(_firstToken, new IdentLValue(_firstToken), _expression);
	}

	Block block() throws SyntaxException {
		Token _firstToken = t;
		ArrayList<Dec> _decs=new ArrayList<>();
		ArrayList<Statement> _statements=new ArrayList<>();
		match(LBRACE);
		Kind k=t.kind;
		boolean isDec=deckind.contains(k);
		boolean isState=statementkind.contains(k);
		while(isDec||isState){
			if(isDec) _decs.add(dec());
			else _statements.add(statement());
			k=t.kind;
			isDec=deckind.contains(k);
			isState=statementkind.contains(k);
		}
		match(RBRACE);
		return new Block(_firstToken, _decs, _statements);
	}

	Expression expression() throws SyntaxException {
		Expression _expression = term();
		Kind k = t.kind;
		Token _op = null;
		while(k==LT||k==LE|| k==GT || k==GE || k==EQUAL || k==NOTEQUAL){
			_op = consume();
			Expression _secondExpression = term();
			_expression=new BinaryExpression(_expression.firstToken, _expression, _op, _secondExpression);
			k=t.kind;
		}
		return _expression;
	}

	Expression term() throws SyntaxException {
		Expression _expression = elem();
		Kind k=t.kind;
		Token _op = null;
		while(k==PLUS || k==MINUS || k==OR){
			_op = consume();
			Expression _secondExpression = elem();
			k=t.kind;
			_expression = new BinaryExpression(_expression.firstToken, _expression, _op, _secondExpression);
		}
		return _expression;
	}

	Expression elem() throws SyntaxException {
		Expression _expression = factor();
		Kind k=t.kind;
		Token _op=null;
		while(k==TIMES || k==DIV || k==AND || k==MOD){
			_op = consume();
			Expression _secondExpression = factor();
			k=t.kind;
			_expression = new BinaryExpression(_expression.firstToken, _expression, _op, _secondExpression);
		}
		return _expression;
	}

	Expression factor() throws SyntaxException {
		Kind kind = t.kind;
		switch (kind) {
			case IDENT: {
				return new IdentExpression(consume());
			}
			
			case INT_LIT: {
				return new IntLitExpression(consume());
			}
			
			case KW_TRUE:
			case KW_FALSE: {
				return new BooleanLitExpression(consume());
			}
			
			case KW_SCREENWIDTH:
			case KW_SCREENHEIGHT: {
				return new ConstantExpression(consume());
			}
				
			case LPAREN: {
				consume();
				Expression _expression = expression();
				match(RPAREN);
				return _expression;
			}
				
			default:
				//you will want to provide a more useful error message
				throw new SyntaxException("illegal factor");
		}
	}


	Chain chain() throws SyntaxException {
		Token _firstToken = t;
		Chain _e0 = chainElem();
		Token _arrow = match(ARROW, BARARROW);
		ChainElem _e1 = chainElem();
		BinaryChain _binaryChain = new BinaryChain(_firstToken, _e0, _arrow, _e1);
		while(t.kind==ARROW||t.kind==BARARROW){
			_arrow = consume();
			_e1 = chainElem();
			_binaryChain = new BinaryChain(_firstToken, _binaryChain, _arrow, _e1);
		}
		return _binaryChain;
	}

	ChainElem chainElem() throws SyntaxException {
		if(t.isKind(IDENT)){
			return new IdentChain(consume());
		}else{
			Token _firstToken = match(OP_BLUR, OP_GRAY, OP_CONVOLVE, KW_SHOW,KW_HIDE,KW_MOVE,KW_XLOC,KW_YLOC,
				OP_WIDTH, OP_HEIGHT, KW_SCALE);
			Tuple _tuple = arg();
			switch(_firstToken.kind){
				case OP_BLUR:
				case OP_GRAY:
				case OP_CONVOLVE:{
					return new FilterOpChain(_firstToken, _tuple);
				}
				case OP_WIDTH:
				case OP_HEIGHT:
				case KW_SCALE:{
					return new ImageOpChain(_firstToken, _tuple);
				}
				default:{
					return new FrameOpChain(_firstToken, _tuple);
				}
			}
		}
	}

	Tuple arg() throws SyntaxException {
		Token _firstToken = t;
		List<Expression> _expression=new ArrayList<>();
		if(t.isKind(LPAREN)){
			consume();
			_expression.add(expression());
			while(t.isKind(COMMA)){
				consume();
				_expression.add(expression());
			}
			match(RPAREN);
			return new Tuple(_firstToken, _expression);
		}else{
			return new Tuple(_firstToken, _expression);
		}
	}

	/**
	 * Checks whether the current token is the EOF token. If not, a
	 * SyntaxException is thrown.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.isKind(EOF)) {
			return t;
		}
		throw new SyntaxException("expected EOF");
	}

	/**
	 * Checks if the current token has the given kind. If so, the current token
	 * is consumed and returned. If not, a SyntaxException is thrown.
	 * 
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		if (t.isKind(kind)) {
			return consume();
		}
		throw new SyntaxException("saw " + t.kind + " expected " + kind);
	}

	/**
	 * Checks if the current token has one of the given kinds. If so, the
	 * current token is consumed and returned. If not, a SyntaxException is
	 * thrown.
	 * 
	 * * Precondition: for all given kinds, kind != EOF
	 * 
	 * @param kinds
	 *            list of kinds, matches any one
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind... kinds) throws SyntaxException {
		// TODO. Optional but handy
		for(Kind kind:kinds){
			if(t.isKind(kind)){
				return consume();
			}
		}
		throw new SyntaxException("One of "+Arrays.toString(kinds)+" expected");
	}

	/**
	 * Gets the next token and returns the consumed token.
	 * 
	 * Precondition: t.kind != EOF
	 * 
	 * @return
	 * 
	 */
	private Token consume() throws SyntaxException {
		Token tmp = t;
		t = scanner.nextToken();
		return tmp;
	}

}
