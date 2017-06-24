package cop5556sp17.AST;

import cop5556sp17.Scanner.Token;

public class IdentChain extends ChainElem {

	Dec _dec;
	public IdentChain(Token firstToken) {
		super(firstToken);
		_dec = null;
	}

	public void setDec(Dec d){
		_dec = d;
	}
	
	public Dec getDec(){
		return _dec;
	}
	
	@Override
	public String toString() {
		return "IdentChain [firstToken=" + firstToken + "]";
	}


	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitIdentChain(this, arg);
	}

}
