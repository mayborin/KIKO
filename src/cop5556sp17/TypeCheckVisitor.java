package cop5556sp17;

import cop5556sp17.Scanner;
import cop5556sp17.AST.ASTNode;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.Chain;
import cop5556sp17.AST.ChainElem;
import cop5556sp17.AST.ConstantExpression;
import cop5556sp17.AST.Dec;
import cop5556sp17.AST.Expression;
import cop5556sp17.AST.FilterOpChain;
import cop5556sp17.AST.FrameOpChain;
import cop5556sp17.AST.IdentChain;
import cop5556sp17.AST.IdentExpression;
import cop5556sp17.AST.IdentLValue;
import cop5556sp17.AST.IfStatement;
import cop5556sp17.AST.ImageOpChain;
import cop5556sp17.AST.IntLitExpression;
import cop5556sp17.AST.ParamDec;
import cop5556sp17.AST.Program;
import cop5556sp17.AST.SleepStatement;
import cop5556sp17.AST.Statement;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

import java.util.ArrayList;
import java.util.List;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.LinePos;
import cop5556sp17.Scanner.Token;
import static cop5556sp17.AST.Type.TypeName.*;
import static cop5556sp17.Scanner.Kind.ARROW;
import static cop5556sp17.Scanner.Kind.KW_HIDE;
import static cop5556sp17.Scanner.Kind.KW_MOVE;
import static cop5556sp17.Scanner.Kind.KW_SHOW;
import static cop5556sp17.Scanner.Kind.KW_XLOC;
import static cop5556sp17.Scanner.Kind.KW_YLOC;
import static cop5556sp17.Scanner.Kind.OP_BLUR;
import static cop5556sp17.Scanner.Kind.OP_CONVOLVE;
import static cop5556sp17.Scanner.Kind.OP_GRAY;
import static cop5556sp17.Scanner.Kind.OP_HEIGHT;
import static cop5556sp17.Scanner.Kind.OP_WIDTH;
import static cop5556sp17.Scanner.Kind.*;

public class TypeCheckVisitor implements ASTVisitor {

	@SuppressWarnings("serial")
	public static class TypeCheckException extends Exception {
		TypeCheckException(String message) {
			super(message);
		}
	}

	SymbolTable symtab = new SymbolTable();

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Chain _e0 = binaryChain.getE0();
		Token _op = binaryChain.getArrow();
		ChainElem _e1 = binaryChain.getE1();
		_e0.visit(this, arg);
		_e1.visit(this, arg);
		TypeName _t0 = _e0.getTypeName();
		TypeName _t1 = _e1.getTypeName();
		if(_op.kind==BARARROW){
			if(_t0!=TypeName.IMAGE || !(_e1 instanceof FilterOpChain))
				throw new TypeCheckException(binaryChain.getFirstToken().getLinePos().toString());
			binaryChain.setTypeName(TypeName.IMAGE);
		}else{
			switch(_t0){
			case INTEGER:{
				if(!((_e1 instanceof IdentChain)&&(_t1==TypeName.INTEGER))){
					throw new TypeCheckException(binaryChain.getFirstToken().getLinePos().toString());
				}else{
					binaryChain.setTypeName(TypeName.INTEGER);
				}
			}break;
			case URL:
			case FILE:{
				if(_t1!=IMAGE) throw new TypeCheckException(binaryChain.getFirstToken().getLinePos().toString());
				binaryChain.setTypeName(TypeName.IMAGE);
			}break;
			case FRAME:{
				if(!(_e1 instanceof FrameOpChain)) throw new TypeCheckException(binaryChain.getFirstToken().getLinePos().toString());
				Kind tmpKind = _e1.getFirstToken().kind;
				if(tmpKind==KW_XLOC || tmpKind==KW_YLOC) binaryChain.setTypeName(TypeName.INTEGER);
				else binaryChain.setTypeName(TypeName.FRAME);
			}break;
			case IMAGE:{
				if(_e1 instanceof ImageOpChain){
					Kind tmpKind = _e1.getFirstToken().kind;
					if(tmpKind==OP_WIDTH || tmpKind==OP_HEIGHT){
						binaryChain.setTypeName(TypeName.INTEGER);
					}else{
						binaryChain.setTypeName(TypeName.IMAGE);
					}
				}else if(_t1 == FRAME){
					binaryChain.setTypeName(TypeName.FRAME);
				}else if(_t1==FILE){
					binaryChain.setTypeName(TypeName.NONE);
				}else if((_e1 instanceof FilterOpChain)||(_e1 instanceof IdentChain)){
					binaryChain.setTypeName(TypeName.IMAGE);
				}else{
					throw new TypeCheckException(binaryChain.getFirstToken().getLinePos().toString());
				}
			}break;
			default:
				throw new TypeCheckException(binaryChain.getFirstToken().getLinePos().toString());
			}
		}
		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		binaryExpression.getE0().visit(this, arg);
		binaryExpression.getE1().visit(this, arg);
		TypeName _e0 = binaryExpression.getE0().getTypeName();
		TypeName _e1 = binaryExpression.getE1().getTypeName();
		Kind _op = binaryExpression.getOp().kind;
		switch(_op){
		case PLUS:
		case MINUS:{
			if(_e0!=_e1) 
				throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
			binaryExpression.setTypeName(_e1);
		}break;
		case DIV:{
			if(_e0==TypeName.INTEGER && _e1==TypeName.INTEGER){
				binaryExpression.setTypeName(TypeName.INTEGER);
			}else if(_e0==TypeName.IMAGE&&_e1==TypeName.INTEGER){
				binaryExpression.setTypeName(TypeName.IMAGE);
			}else{
				throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
			}
		}break;
		case TIMES:{
			if(_e0==TypeName.INTEGER&&_e1==TypeName.INTEGER){
				binaryExpression.setTypeName(_e0);
			}else if((_e0==TypeName.INTEGER && _e1==TypeName.IMAGE) || (_e0==TypeName.IMAGE&&_e1==TypeName.INTEGER)){
				binaryExpression.setTypeName(TypeName.IMAGE);
			}else throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
		}break;
		case LT:
		case GT:
		case LE:
		case GE:{
			if((_e0==TypeName.INTEGER&&_e1==TypeName.INTEGER)||(_e0==TypeName.BOOLEAN&&_e1==TypeName.BOOLEAN)){
				binaryExpression.setTypeName(TypeName.BOOLEAN);
			}else throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
		}break;
		case EQUAL:
		case NOTEQUAL:{
			if(_e0!=_e1) throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
			binaryExpression.setTypeName(TypeName.BOOLEAN);
		}break;
		case MOD:{
			if(_e0==TypeName.INTEGER&&_e1==TypeName.INTEGER){
				binaryExpression.setTypeName(TypeName.INTEGER);
			}else if(_e0==TypeName.IMAGE&&_e1==TypeName.INTEGER){
				binaryExpression.setTypeName(TypeName.IMAGE);
			}else{
				throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
			}
		}break;
		case OR:
		case AND:{
			if(_e0==TypeName.BOOLEAN && _e1==TypeName.BOOLEAN){
				binaryExpression.setTypeName(TypeName.BOOLEAN);
			}else{
				throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
			}
		}break;
		default:
			throw new TypeCheckException(binaryExpression.getFirstToken().getLinePos().toString());
		}
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		// TODO Auto-generated method stub
		ArrayList<Dec> _dec = block.getDecs();
		ArrayList<Statement> _statement = block.getStatements();
		symtab.enterScope();
		for(Dec d : _dec){
			d.visit(this, arg);
		}
		for(Statement s:_statement){
			s.visit(this, arg);
		}
		symtab.leaveScope();
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		booleanLitExpression.setTypeName(TypeName.BOOLEAN);
		return booleanLitExpression.getValue();
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Tuple _arg = filterOpChain.getArg();
		//check Tuple.length==0
		if(_arg.getExprList().size()!=0){
			throw new TypeCheckException("Condition[FilterOpChain]: Tuple.length==0 Error at "+filterOpChain.getFirstToken().getLinePos().toString());
		}
		filterOpChain.setTypeName(TypeName.IMAGE);
		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Kind _kind = frameOpChain.getFirstToken().kind;
		Tuple _tuple = frameOpChain.getArg();
		switch(_kind){
		case KW_SHOW:
		case KW_HIDE:{
			if(_tuple.getExprList().size()!=0){
				throw new TypeCheckException("Condition[FrameOpChain]: Tuple.length==0 Error at "+frameOpChain.getFirstToken().getLinePos().toString());
			}
			frameOpChain.setTypeName(TypeName.NONE);
		}
		case KW_XLOC:
		case KW_YLOC:{
			if(_tuple.getExprList().size()!=0){
				throw new TypeCheckException("Condition[FrameOpChain]: Tuple.length==0 Error at "+frameOpChain.getFirstToken().getLinePos().toString());
			}
			frameOpChain.setTypeName(TypeName.INTEGER);
		}break;
		case KW_MOVE:{
			if(_tuple.getExprList().size()!=2){
				throw new TypeCheckException("Condition[FrameOpChain]: Tuple.length==2 Error at "+frameOpChain.getFirstToken().getLinePos().toString());
			}
			_tuple.visit(this, arg);
			frameOpChain.setTypeName(TypeName.NONE);
		}break;
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Dec _dec = symtab.lookup(identChain.getFirstToken().getText());
		if(_dec==null){
			throw new TypeCheckException("Ident "+identChain.getFirstToken()+" at "+identChain.getFirstToken().getLinePos().toString()+
					" not yet defined or not visible in the current scope!!");
		}
		identChain.setTypeName(_dec.getTypeName());
		identChain.setDec(_dec);
		return null;
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token _ident = identExpression.getFirstToken();
		Dec _dec = symtab.lookup(_ident.getText());
		if(_dec==null){
			throw new TypeCheckException("Ident "+_ident.getLinePos().toString()+" is not yet declared or not visiable in the current Scope");
		}
		identExpression.setDec(_dec);
		identExpression.setTypeName(_dec.getTypeName());
		return null;
	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression _expression = ifStatement.getE();
		Block _block = ifStatement.getB();
		_expression.visit(this, arg);
		_block.visit(this, arg);
		if(_expression.getTypeName()!=TypeName.BOOLEAN){
			throw new TypeCheckException("Condition[IfStatement]: Expression.type=Boolean Error at"
					+ ifStatement.getFirstToken().getLinePos().toString());
		}
		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		// TODO Auto-generated method stub
		intLitExpression.setTypeName(TypeName.INTEGER);
		return new Integer(intLitExpression.value);
	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression _expression = sleepStatement.getE();
		_expression.visit(this, arg);
		if(_expression.getTypeName()!=TypeName.INTEGER){
			throw new TypeCheckException("Condition[SleepStatment]: Expression.type==INTEGER Error at " + _expression.getFirstToken().getLinePos().toString());
		}
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Expression _expression = whileStatement.getE();
		Block _block = whileStatement.getB();
		_expression.visit(this, arg);
		_block.visit(this, arg);
		if(_expression.getTypeName()!=TypeName.BOOLEAN){
			throw new TypeCheckException("Condition[WhileStatement]: Expression.type=Boolean Error at"
					+ whileStatement.getFirstToken().getLinePos().toString());
		}
		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		// TODO Auto-generated method stub
		if(!symtab.insert(declaration.getIdent().getText(), declaration)){
			throw new TypeCheckException("Duplicate declaration for "+declaration.getIdent()+" at "+declaration.getFirstToken().getLinePos().toString());
		}
		declaration.setTypeName(Type.getTypeName(declaration.getType()));
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		// TODO Auto-generated method stub
		ArrayList<ParamDec> _paramDec = program.getParams();
		for(ParamDec p : _paramDec){
			p.visit(this, arg);
		}
		Block _block = program.getB();
		_block.visit(this, arg);
		return null;
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		// TODO Auto-generated method stub
		IdentLValue _ident = assignStatement.getVar();
		Expression _expression = assignStatement.getE();
		_ident.visit(this, arg);
		_expression.visit(this, arg);
		if(_ident.getTypeName()!=_expression.getTypeName()){
			throw new TypeCheckException("Condition[AssignmentStatement]: IdentLValue.type==Expression.type Error at "+assignStatement.getFirstToken().getLinePos().toString());
		}
		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token _ident = identX.getFirstToken();
		Dec _dec = symtab.lookup(_ident.getText());
		if(_dec==null){
			throw new TypeCheckException("Ident "+_ident.getLinePos().toString()+" is not yet declared or not visiable in the current Scope");
		}
		identX.setDec(_dec);
		identX.setTypeName(_dec.getTypeName());
		return null;
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		// TODO Auto-generated method stub
		paramDec.setTypeName(Type.getTypeName(paramDec.firstToken));
		if(!symtab.insert(paramDec.getIdent().getText(), paramDec)){
			throw new TypeCheckException("Duplicate declaration for "+paramDec.getIdent()+" at "+paramDec.getFirstToken().getLinePos().toString());
		}
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		// TODO Auto-generated method stub
		constantExpression.setTypeName(TypeName.INTEGER);
		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Kind _imageOpKind = imageOpChain.getFirstToken().kind;
		Tuple _tuple = imageOpChain.getArg();
		if(_imageOpKind==OP_WIDTH || _imageOpKind==OP_HEIGHT){
			if(_tuple.getExprList().size()!=0){
				throw new TypeCheckException("Condition[ImageOpChain, OP_WIDTH | OP_HEIGHT]: "
						+ "Tuple.length==0 Error at"+imageOpChain.getFirstToken().getLinePos().toString());
			}
			imageOpChain.setTypeName(TypeName.INTEGER);
		}else{
			if(_tuple.getExprList().size()!=1){
				_tuple.visit(this, arg);
				throw new TypeCheckException("Condition[ImageOpChain, KW_SCALE]: "
						+ "Tuple.length==1 Error at"+imageOpChain.getFirstToken().getLinePos().toString());
			}
			imageOpChain.setTypeName(TypeName.IMAGE);
		}
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		// TODO Auto-generated method stub
		List<Expression> _expression = tuple.getExprList();
		for(Expression e : _expression){
			e.visit(this, arg);
			if(e.getTypeName()!=TypeName.INTEGER){
				throw new TypeCheckException("Condition[Tuple]: all expreesion in List<Expression> should be of Type Integer Error at"
						+ tuple.getFirstToken().getLinePos().toString());
			}
		}
		return null;
	}


}
