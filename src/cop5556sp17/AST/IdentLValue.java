package cop5556sp17.AST;

import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.Type.TypeName;

public class IdentLValue extends ASTNode {
	private TypeName typeName;
	private Dec dec;
	
	public IdentLValue(Token firstToken) {
		super(firstToken);
		this.typeName=null;
		this.dec = null;
	}
	
	@Override
	public String toString() {
		return "IdentLValue [firstToken=" + firstToken + "]";
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitIdentLValue(this,arg);
	}

	public String getText() {
		return firstToken.getText();
	}
	
	public TypeName getTypeName(){
		return typeName;
	}
	
	public void setTypeName(TypeName typeName){
		this.typeName=typeName;
	}
	
	public void setDec(Dec dec){
		this.dec = dec;
	}
	
	public Dec getDec(){
		return dec;
	}

}
