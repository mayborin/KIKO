# KIKO


Kiko is an JVM language specified to image manipulation and display and designed in an instructional process of Compiler design. The framework is contributed by Professor [Beverly Sanders](https://www.cise.ufl.edu/people/faculty/sanders) from University of Florida.

The whole design process includes 
1. Scanner
2. recursive descent parser
3. abstract syntax tree and LeBlanc-Cook symbol table
4. type checking using Vistor Pattern
5. code generation using ASM bytecode manipulation framework.

***

## Language Definition

### Lexical structure

```
comment ::=   /*   NOT(*/)*  */
token ::= ident  | keyword | frame_op_keyword | filter_op_keyword | image_op_keyword | boolean_literal
 	| int_literal  | separator  | operator
ident ::= ident_start  ident_part*    (but not reserved)
ident_start ::=  A .. Z | a .. z | $ | _
ident_part ::= ident_start | ( 0 .. 9 )
int_literal ::= 0  |  (1..9) (0..9)*
keyword ::= integer | boolean | image | url | file | frame | while | if | sleep | screenheight | screenwidth 
filter_op_keyword ∷= gray | convolve | blur | scale
image_op_keyword ∷= width | height 
frame_op_keyword ∷= xloc | yloc | hide | show | move
boolean_literal ::= true | false
separator ::= 	;  | ,  |  (  |  )  | { | }
operator ::=   	|  | &  |  ==  | !=  | < |  > | <= | >= | +  |  -  |  *   |  /   |  % | !  | -> |  |-> | <-
```

### Abstract Syntax

```
Program ∷= List<ParamDec> Block
ParamDec ∷= type ident
Block ∷= List<Dec>  List<Statement>
Dec ∷= type ident
Statement ∷= SleepStatement | WhileStatement | IfStatement | Chain
      	| AssignmentStatement
SleepStatement ∷= Expression
AssignmentStatement ∷= IdentLValue Expression
Chain ∷= ChainElem | BinaryChain
ChainElem ::= IdentChain | FilterOpChain | FrameOpChain | ImageOpChain
IdentChain ∷= ident
FilterOpChain ∷= filterOp Tuple
FrameOpChain ∷= frameOp Tuple
ImageOpChain ∷= imageOp Tuple
BinaryChain ∷= Chain (arrow | bararrow)  ChainElem
WhileStatement ∷= Expression Block
IfStatement ∷= Expression Block
Expression ∷= IdentExpression | IntLitExpression | BooleanLitExpression
  	| ConstantExpression | BinaryExpression
IdentExpression ∷= ident
IdentLValue ∷= ident
IntLitExpression ∷= intLit
BooleanLitExpression ∷= booleanLiteral
ConstantExpression ∷= screenWidth | screenHeight
BinaryExpression ∷= Expression op Expression
Tuple :≔ List<Expression>
	op ∷= relOp | weakOp | strongOp
type ∷= integer | image | frame | file | boolean | url
```
### Type checking rules

```
Program ∷= List<ParamDec> Block

ParamDec ∷= type ident  symtab.insert(ident.getText(), ParamDec);

Block ∷= symtab.enterScope()  List<Dec>  List<Statement>  symtab.leaveScope()

Dec ∷= type ident  symtab.insert(ident.getText(), Dec);
Statement ∷= SleepStatement | WhileStatement | IfStatement | Chain
      	| AssignmentStatement
        
SleepStatement ∷= Expression condition: Expression.type==INTEGER

AssignmentStatement ∷= IdentLValue Expression 
                      condition:  IdentLValue.type== Expression.type
                      
Chain ∷= ChainElem | BinaryChain

ChainElem ::= IdentChain | FilterOpChain | FrameOpChain | ImageOpChain

IdentChain ∷= ident  
	condition:  ident has been declared and is visible in the current scope
  
IdentChain.type <- ident.type
ident.type <- symtab.lookup(ident.getText()).getType()

FilterOpChain ∷= filterOp Tuple
	condition: Tuple.length == 0
	FilterOpChain.type <- IMAGE
  
FrameOpChain ∷= frameOp Tuple

  if (FrameOP.isKind(KW_SHOW, KW_HIDE) {
    condition: Tuple.length == 0
    FrameOpChain.type <- NONE
  }else if (FrameOp.isKind(KW_XLOC, KW_YLOC){
    condition: Tuple.length == 0
    FrameOpChain.type <- INTEGER
  }else if(FrameOp.isKind(KW_MOVE){
		condition: Tuple.length == 2
    FrameOpChain.type <- NONE
  }else there is a bug in your parser
                         
ImageOpChain ∷= imageOp Tuple

  if (imageOp.isKind(OP_WIDTH, OP_HEIGHT){
    condition:  Tuple.length == 0
    ImageOpChain.type <- INTEGER
  }else if (imageOP.isKind(KW_SCALE)){
    condition: Tuple.length==1
    ImageOpChain.type <- IMAGE
  }
  
BinaryChain ∷= Chain (arrow | bararrow)  ChainElem
```
|BinaryChain|Chain|op|ChainElem|
|---|---|---|---|
|type <-IMAGE|type =URL|arrow|type = IMAGE
|type <-IMAGE|type = FILE|arrow|type = IMAGE
|type <-INTEGER|type = FRAME|arrow|instanceof FrameOp & firstToken ∈ { KW_XLOC, KW_YLOC}
|type <-FRAME|type = FRAME|arrow|instanceof FrameOp &firstToken ∈ { KW_SHOW, KW_HIDE, KW_MOVE}
|type <-INTEGER|type = IMAGE|arrow|instanceof ImageOpChain) && firstToken ∈ { OP_WIDTH, OP_HEIGHT}
|type <-FRAME|type = IMAGE|arrow|type = FRAME
| type <-NONE|type = IMAGE|arrow|type = FILE
|type <-IMAGE|type = IMAGE|arrow,barrow|instanceof FilterOpChain &firstToken ∈ {OP_GRAY, OP_BLUR, OP_CONVOLVE}
|type <-IMAGE|type = IMAGE|arrow|instanceof ImageOpChain &firstToken ∈ {KW_SCALE}
|type <-IMAGE|type = IMAGE|arrow|instanceof IdentChain & IdentChain.type = INTEGER
|type <-INTEGER|type = INTEGER|arrow|instance of IdentChain & IdentChain.type = INTEGER

```
WhileStatement ∷= Expression Block
condition:  Expression.type = Boolean
 
IfStatement ∷= Expression Block
condition:  Expression.type = Boolean
 
Expression ∷= IdentExpression | IntLitExpression | BooleanLitExpression| ConstantExpression | BinaryExpression

IdentExpression ∷= ident
	condition:  ident has been declared and is visible in the current scope
	IdentExpression.type <- ident.type
	IdentExpression.dec <- Dec of ident
  
IdentLValue ∷= ident
	condition:  ident has been declared and is visible in the current scope
	IdentLValue.dec <- Dec of ident
  
IntLitExpression ∷= intLit
	IntLitExpression.type <- INTEGER
  
BooleanLitExpression ∷= booleanLiteral
	BooleanLitExpression.type <- BOOLEAN
  
ConstantExpression ∷= screenWidth | screenHeight
	ConstantExpression.type <- INTEGER
  
BinaryExpression ∷= Expression op Expression
```
| BinaryExpression.type|Expression0.type|op|Expression1.type
|---|---|---|---|
|INTEGER|INTEGER|PLUS, MINUS|INTEGER
|IMAGE|IMAGE|PLUS, MINUS|IMAGE
|INTEGER|INTEGER|TIMES,DIV|INTEGER
|IMAGE|INTEGER|TIMES|IMAGE
|IMAGE|IMAGE|TIMES|INTEGER
|BOOLEAN|INTEGER|LT,GT,LE,GE|INTEGER
|BOOLEAN|BOOLEAN|LT,GT,LE,GE|BOOLEAN
|BOOLEAN ||EQUAL, NOTEQUAL|condition: Expression0.type = Expression1.type

```
Tuple ∷= List<Expression>
	condition:  for all expression in List<Expression>: Expression.type = INTEGER
	op ∷= relOp | weakOp | strongOp
  
type ∷= integer | image | frame | file | boolean | url
```

## How to Use
1. Compile source code
```
cd kiko
javac -cp .:lib/* src/cop5556sp17/*.java src/cop5556sp17/AST/*.java
```
2. Compile your kiko program into .class file
```
java -cp lib/*:src/  cop5556sp17.Compiler example.kiko
```
3. Run your program as a Java program
```
java -cp .:lib/*:src/ example [args]
```

## Example kiko program

```
example file a  {
    image x frame y integer z
    a->x->y; 
    z<-100;
    while(z>0){
        if(z%2==0){
            y->show;
        }
        if((z%2!=0 )| false){
            y->hide;
        }
        sleep 2000; 
        z<-z-1;
    }
}
```


















