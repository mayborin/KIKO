package cop5556sp17;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTVisitor;
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
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

import static cop5556sp17.AST.Type.TypeName.FRAME;
import static cop5556sp17.AST.Type.TypeName.IMAGE;
import static cop5556sp17.AST.Type.TypeName.URL;
import static cop5556sp17.Scanner.Kind.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
		this.indexOfParam = 0;
		this.localSlot = 1;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;
	int indexOfParam;
	int localSlot;
	
	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.getName();
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(sourceFileName, null);

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.
		ArrayList<ParamDec> params = program.getParams();
		for (ParamDec dec : params)
			dec.visit(this, mv);
		mv.visitInsn(RETURN);
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		//end of main method

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		Label endRun = new Label();
		mv.visitLabel(endRun);
		mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
		//TODO  visit the local variables
		
		mv.visitMaxs(1, 1);
		mv.visitEnd(); // end of run method
		
		cw.visitEnd();//end of class
		
		//generate classfile and return it
		return cw.toByteArray();
	}



	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		assignStatement.getE().visit(this, arg);//calculate the value of expression, leave it on top of stack
		CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
		CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getTypeName());
		assignStatement.getVar().visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		int flag = (Integer) arg;
		Chain _e0 = binaryChain.getE0();
		Token _op = binaryChain.getArrow();
		_e0.visit(this, -1);
		if(_e0.getTypeName()==TypeName.FILE){
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromFile", PLPRuntimeImageIO.readFromFileDesc,false);
		}
		if(_e0.getTypeName()==TypeName.URL){
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromURL", PLPRuntimeImageIO.readFromURLSig,false);
		}
		ChainElem _e1 = binaryChain.getE1();
		_e1.visit(this, 1);
		if(flag==0){
			mv.visitInsn(POP);
		}
		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
      //TODO  Implement this
		binaryExpression.getE0().visit(this, arg);
		binaryExpression.getE1().visit(this, arg);
		boolean isIMG = binaryExpression.getE0().getTypeName()==TypeName.IMAGE || binaryExpression.getE1().getTypeName()==TypeName.IMAGE;
		switch(binaryExpression.getOp().kind){
		case OR:{
			mv.visitInsn(IOR);
		}break;
		case AND:{
			mv.visitInsn(IAND);
		}break;
		case PLUS:{
			if (isIMG) mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "add", PLPRuntimeImageOps.addSig, false);
			else mv.visitInsn(IADD);
		}break;
		case MINUS:{
			if(isIMG) mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "sub", PLPRuntimeImageOps.subSig, false);
			else mv.visitInsn(ISUB);
		}break;
		case TIMES:{
			if(isIMG) mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mul", PLPRuntimeImageOps.mulSig, false);
			else mv.visitInsn(IMUL);
		}break;
		case DIV:{
			if(isIMG) mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "div", PLPRuntimeImageOps.divSig, false);
			else mv.visitInsn(IDIV);
		}break;
		case MOD:{
			if(isIMG) mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mod", PLPRuntimeImageOps.modSig, false);
			else mv.visitInsn(IREM);
		}break;
		case LT:{
			Label l1 =  new Label();
			mv.visitJumpInsn(IF_ICMPGE, l1);
			mv.visitInsn(ICONST_1);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
		}break;
		case LE:{
			Label l1 = new Label();
			mv.visitJumpInsn(IF_ICMPGT, l1);
			mv.visitInsn(ICONST_1);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
		}break; 
		case GT:{
			Label l1 = new Label();
			mv.visitJumpInsn(IF_ICMPLE, l1);
			mv.visitInsn(ICONST_1);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
		}break;
		case GE:{
			Label l1 = new Label();
			mv.visitJumpInsn(IF_ICMPLT, l1);
			mv.visitInsn(ICONST_1);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
		}break;
		case EQUAL:{
			Label l1 = new Label();
			mv.visitJumpInsn(IF_ICMPNE, l1);
			mv.visitInsn(ICONST_1);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
		}break;
		case NOTEQUAL:{
			Label l1 = new Label();
			mv.visitJumpInsn(IF_ICMPEQ, l1);
			mv.visitInsn(ICONST_1);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
		}break;
		
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		//TODO  Implement this
		Label startBlock = new Label();
		mv.visitLabel(startBlock);
		ArrayList<Dec> _decs = block.getDecs();
		ArrayList<Statement> _statements = block.getStatements();
		for(Dec dec : _decs){
			dec.visit(this, arg);
		}
		for(Statement stat : _statements){
			stat.visit(this, 0);// Integer 0 used to reprensent top level of Chain
		}
		Label endBlock = new Label();
		mv.visitLabel(endBlock);
		//mv.visitLocalVariable("this", classDesc, null, startBlock, endBlock, 0);
		for( Dec dec : _decs){
			mv.visitLocalVariable(dec.getIdent().getText(), dec.getTypeName().getJVMTypeDesc(), null, startBlock, endBlock, dec.getSlot());
		}
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		//TODO Implement this
		boolean val = booleanLitExpression.getValue();
		if(val){
			mv.visitInsn(ICONST_1);
		}else{
			mv.visitInsn(ICONST_0);
		}
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		Kind _kind = constantExpression.getFirstToken().kind;
		if(_kind == KW_SCREENHEIGHT){
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenHeight", PLPRuntimeFrame.getScreenHeightSig, false);
		}else{
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenWidth", PLPRuntimeFrame.getScreenWidthSig, false);
		}
		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		//TODO Implement this
		declaration.setSlot(localSlot++);
		if(declaration.getTypeName()==IMAGE || declaration.getTypeName()==FRAME){
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, declaration.getSlot());
		}
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		//filterOpChain.getArg().visit(this, arg);
		Kind _kind = filterOpChain.getFirstToken().kind;
		mv.visitInsn(ACONST_NULL);
		if(_kind==OP_BLUR){
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "blurOp", PLPRuntimeFilterOps.opSig, false);
		}else if(_kind == OP_GRAY){
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "grayOp", PLPRuntimeFilterOps.opSig, false);
		}else{
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "convolveOp", PLPRuntimeFilterOps.opSig, false);
		}
		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		frameOpChain.getArg().visit(this, arg);
		Kind _kind = frameOpChain.getFirstToken().kind;
		switch(_kind){
			case KW_SHOW:{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "showImage", PLPRuntimeFrame.showImageDesc, false);
			}break;
			case KW_HIDE:{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "hideImage", PLPRuntimeFrame.hideImageDesc, false);
			}break;
			case KW_MOVE:{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "moveFrame", PLPRuntimeFrame.moveFrameDesc, false);
			}break;
			case KW_XLOC:{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getXVal", PLPRuntimeFrame.getXValDesc, false);
			}break;
			case KW_YLOC:{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getYVal", PLPRuntimeFrame.getYValDesc, false);
			}break;
			default:
				break;
		}
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		boolean isLeft = ((Integer) arg) == -1;
		Dec _dec = identChain.getDec();
		TypeName tp = _dec.getTypeName();
		if(isLeft){
			if(_dec.getSlot()<0){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, className, _dec.getIdent().getText(), tp.getJVMTypeDesc());
			}else{
				if(tp==TypeName.BOOLEAN || tp==TypeName.INTEGER){
					mv.visitVarInsn(ILOAD, _dec.getSlot());
				}else{
					mv.visitVarInsn(ALOAD, _dec.getSlot());
				}
			}
		}else{
			if(_dec.getSlot()<0){
				if(tp == TypeName.BOOLEAN || tp==TypeName.INTEGER || tp==TypeName.IMAGE){
					mv.visitInsn(DUP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, _dec.getIdent().getText(), tp.getJVMTypeDesc());
				}else if(tp==TypeName.FILE){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, _dec.getIdent().getText(), tp.getJVMTypeDesc());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "write", PLPRuntimeImageIO.writeImageDesc, false);
				}else if(tp==TypeName.FRAME){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, _dec.getIdent().getText(), tp.getJVMTypeDesc());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "createOrSetFrame", PLPRuntimeFrame.createOrSetFrameSig, false);
					mv.visitInsn(DUP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, _dec.getIdent().getText(), tp.getJVMTypeDesc());
				}
			}else{
				if(tp==TypeName.BOOLEAN || tp==TypeName.INTEGER){
					mv.visitInsn(DUP);
					mv.visitVarInsn(ISTORE, _dec.getSlot());
				}else if(tp==TypeName.IMAGE){
					mv.visitInsn(DUP);
					mv.visitVarInsn(ASTORE, _dec.getSlot());
				}else if(tp==TypeName.FILE){
					mv.visitVarInsn(ALOAD, _dec.getSlot());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "write", PLPRuntimeImageIO.writeImageDesc, false);
				}else if(tp==TypeName.FRAME){
					mv.visitVarInsn(ALOAD, _dec.getSlot());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "createOrSetFrame", PLPRuntimeFrame.createOrSetFrameSig, false);
					mv.visitInsn(DUP);
					mv.visitVarInsn(ASTORE, _dec.getSlot());
				}
			}
		}
		return null;
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		//TODO Implement this
		Dec _dec = identExpression.getDec();
		if(_dec.getSlot()<0){//this is a field variable
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, _dec.getIdent().getText(), _dec.getTypeName().getJVMTypeDesc());
		}else{
			if(_dec.getTypeName()==TypeName.BOOLEAN || _dec.getTypeName()==TypeName.INTEGER){
				mv.visitVarInsn(ILOAD, _dec.getSlot());
			}else{
				mv.visitVarInsn(ALOAD, _dec.getSlot());
			}
		}
		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		//TODO Implement this
		Dec _dec = identX.getDec();
		//if it's image on stack top, copy image
		if(_dec.getTypeName() == TypeName.IMAGE){
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "copyImage", PLPRuntimeImageOps.copyImageSig, false);
		}
		if(_dec.getSlot()<0){
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(SWAP);
			mv.visitFieldInsn(PUTFIELD, className, _dec.getIdent().getText(), Type.getTypeName(_dec.getType()).getJVMTypeDesc());
		}else{
			if(_dec.getTypeName()==TypeName.BOOLEAN || _dec.getTypeName()==TypeName.INTEGER){
				mv.visitVarInsn(ISTORE, _dec.getSlot());
			}else{
				mv.visitVarInsn(ASTORE, _dec.getSlot());
			}
		}
		return null;

	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		//TODO Implement this
		Label AFTER = new Label();
		Expression _expression = ifStatement.getE();
		_expression.visit(this, arg);
		mv.visitJumpInsn(IFEQ, AFTER);
		ifStatement.getB().visit(this, arg);
		mv.visitLabel(AFTER);
		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		imageOpChain.getArg().visit(this, arg);
		Kind _kind = imageOpChain.getFirstToken().kind;
		if(_kind == OP_WIDTH){
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getWidth", "()I", false);
		}else if(_kind == OP_HEIGHT){
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getWidth", "()I", false);
		}else{
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "scale", PLPRuntimeImageOps.scaleSig, false);
		}
		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		//TODO Implement this
		mv.visitLdcInsn(intLitExpression.value);
		return null;
	}


	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		//TODO Implement this
		//For assignment 5, only needs to handle integers and booleans
		String ident = paramDec.getIdent().getText();
		String identTypeName = paramDec.getTypeName().getJVMTypeDesc();
		FieldVisitor fv = cw.visitField(0, ident, identTypeName,null, null);
		fv.visitEnd();
		TypeName tp = paramDec.getTypeName();
		if(tp==TypeName.URL){
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(indexOfParam);
			indexOfParam++;
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "getURL", PLPRuntimeImageIO.getURLSig, false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(SWAP);
			mv.visitFieldInsn(PUTFIELD, className, ident, identTypeName);
			return null;
		}
		if(tp==TypeName.FILE){
			mv.visitTypeInsn(NEW, "java/io/File");
			mv.visitInsn(DUP);
		}
		// MethodVisitor mv = (MethodVisitor)arg;
		mv.visitVarInsn(ALOAD, 1);
		mv.visitLdcInsn(indexOfParam);
		indexOfParam++;
		mv.visitInsn(AALOAD);
		if(tp==TypeName.INTEGER){
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
		}else if(tp==TypeName.BOOLEAN){
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
		}else if(tp==TypeName.FILE){
			mv.visitMethodInsn(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
		}
		mv.visitVarInsn(ALOAD, 0);
		mv.visitInsn(SWAP);
		mv.visitFieldInsn(PUTFIELD, className, ident, identTypeName);
		return null;

	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		sleepStatement.getE().visit(this, arg);
		mv.visitInsn(I2L);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		List<Expression> _expression = tuple.getExprList();
		for(Expression e: _expression){
			e.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		//TODO Implement this
		Label GUARD = new Label();
		mv.visitJumpInsn(GOTO, GUARD);
		Label BODY = new Label();
		mv.visitLabel(BODY);
		whileStatement.getB().visit(this, arg);
		mv.visitLabel(GUARD);
		whileStatement.getE().visit(this, arg);
		mv.visitJumpInsn(IFNE, BODY);
		return null;
	}

}
