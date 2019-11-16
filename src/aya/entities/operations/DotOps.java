package aya.entities.operations;

import static aya.obj.Obj.BLOCK;
import static aya.obj.Obj.CHAR;
import static aya.obj.Obj.DICT;
import static aya.obj.Obj.LIST;
import static aya.obj.Obj.NUMBER;
import static aya.obj.Obj.NUMBERLIST;
import static aya.obj.Obj.STR;
import static aya.obj.Obj.SYMBOL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import aya.Aya;
import aya.AyaPrefs;
import aya.OperationDocs;
import aya.exceptions.AyaRuntimeException;
import aya.exceptions.AyaUserRuntimeException;
import aya.exceptions.SyntaxError;
import aya.exceptions.TypeError;
import aya.instruction.DataInstruction;
import aya.instruction.ListBuilder;
import aya.instruction.op.OpInstruction;
import aya.obj.Obj;
import aya.obj.block.Block;
import aya.obj.character.Char;
import aya.obj.dict.Dict;
import aya.obj.list.GenericList;
import aya.obj.list.List;
import aya.obj.list.ListRangeUtils;
import aya.obj.list.Str;
import aya.obj.list.numberlist.NumberList;
import aya.obj.number.Num;
import aya.obj.number.Number;
import aya.obj.number.NumberMath;
import aya.obj.symbol.Symbol;
import aya.parser.CharacterParser;
import aya.parser.Parser;
import aya.util.Pair;
import aya.util.QuickDialog;
import aya.variable.Variable;
import aya.variable.VariableSet;

public class DotOps {

	public static final char FIRST_OP = '!';


	/** A list of all valid single character operations.
	 *  Stored in final array for fast lookup.
	 *  Array indexes are always [(operator character) - FIRST_OP]
	 */
	public static OpInstruction[] DOT_OPS = {
		/* 33 !  */ new OP_Dot_Bang(),
		/* 34 "  */ null,
		/* 35 #  */ null, //Comment
		/* 36 $  */ new OP_Dot_Duplicate(),
		/* 37 %  */ new OP_Dot_Percent(),
		/* 38 &  */ new OP_Dot_And(),
		/* 39 '  */ new OP_Dot_CastChar(),
		/* 40 (  */ new OP_Dot_OParen(),
		/* 41 )  */ new OP_Dot_CParen(),
		/* 42 *  */ null,
		/* 43 +  */ new OP_Dot_Plus(),
		/* 44 ,  */ null,
		/* 45 -  */ new OP_Dot_Minus(),
		/* 46 .  */ null,
		/* 47 /  */ new OP_Dot_FwdSlash(),
		/* 48 0  */ null, //Number Literal
		/* 49 1  */ null, //Number Literal
		/* 50 2  */ null, //Number Literal
		/* 51 3  */ null, //Number Literal
		/* 52 4  */ null, //Number Literal
		/* 53 5  */ null, //Number Literal
		/* 54 6  */ null, //Number Literal
		/* 55 7  */ null, //Number Literal
		/* 56 8  */ null, //Number Literal
		/* 57 9  */ null, //Number Literal
		/* 58    */ null,
		/* 59 ;  */ new OP_Dot_ClearAll(),
		/* 60 <  */ new OP_Dot_LessThan(),
		/* 61 =  */ new OP_Dot_Equals(),
		/* 62 >  */ new OP_Dot_GreaterThan(),
		/* 63 ?  */ new OP_Dot_Conditional(),
		/* 64 @  */ new OP_Dot_At(),
		/* 65 A  */ new OP_Dot_ArrayAll(),
		/* 66 B  */ new OP_Dot_Append(),
		/* 67 C  */ new OP_Dot_SortUsing(),
		/* 68 D  */ new OP_Dot_Error(),
		/* 69 E  */ new OP_Dot_Len(),
		/* 70 F  */ new OP_Dot_Flatten(),
		/* 71 G  */ new OP_Dot_Write(),
		/* 72 H  */ null,
		/* 73 I  */ new OP_Dot_I(),
		/* 74 J  */ null,
		/* 75 K  */ new OP_Dot_TryCatch(),
		/* 76 L  */ null,
		/* 77 M  */ new OP_Dot_M(),
		/* 78 N  */ new OP_Dot_N(),
		/* 79 O  */ null,
		/* 80 P  */ new OP_Dot_Print(),
		/* 81 Q  */ new OP_Dot_Rand(),
		/* 82 R  */ new OP_Dot_R(),
		/* 83 S  */ new OP_Dot_Case(),
		/* 84 T  */ new OP_Dot_T(),
		/* 85 U  */ new OP_RequestString(),
		/* 86 V  */ new OP_Dot_AppendBack(),
		/* 87 W  */ new OP_Dot_W(),
		/* 88 X  */ null,
		/* 89 Y  */ null,
		/* 90 Z  */ new OP_Dot_Zed(),
		/* 91 [  */ null,
		/* 92 \  */ new OP_Dot_BackSlash(),
		/* 93 ]  */ new OP_Colon_Demote(),
		/* 94 ^  */ new OP_Dot_Pow(),
		/* 95 _  */ null, // Member Variable
		/* 96 `  */ null, // Block variable escape
		/* 97 a  */ null, // Member Variable
		/* 98 b  */ null, // Member Variable
		/* 99 c  */ null, // Member Variable
		/* 100 d */ null, // Member Variable
		/* 101 e */ null, // Member Variable
		/* 102 f */ null, // Member Variable
		/* 103 g */ null, // Member Variable
		/* 104 h */ null, // Member Variable
		/* 105 i */ null, // Member Variable
		/* 106 j */ null, // Member Variable
		/* 107 k */ null, // Member Variable
		/* 108 l */ null, // Member Variable
		/* 109 m */ null, // Member Variable
		/* 110 n */ null, // Member Variable
		/* 111 o */ null, // Member Variable
		/* 112 p */ null, // Member Variable
		/* 113 q */ null, // Member Variable
		/* 114 r */ null, // Member Variable
		/* 115 s */ null, // Member Variable
		/* 116 t */ null, // Member Variable
		/* 117 u */ null, // Member Variable
		/* 118 v */ null, // Member Variable
		/* 119 w */ null, // Member Variable
		/* 120 x */ null, // Member Variable
		/* 121 y */ null, // Member Variable
		/* 122 z */ null, // Member Variable
		/* 123 { */ null, // block comments
		/* 124 | */ new OP_Dot_Bar(),
		/* 125 } */ null, // block comments
		/* 126 ~ */ new OP_Dot_Tilde(),
	};


//	/** Returns a list of all the op descriptions **/
//	public static ArrayList<String> getAllOpDescriptions() {
//		ArrayList<String> out = new ArrayList<String>();
//		for (char i = 0; i <= 126-Ops.FIRST_OP; i++) {
//			if(DOT_OPS[i] != null) {
//				out.add(DOT_OPS[i].getDocStr() + "\n(extended operator)");
//			}
//		}
//		return out;
//	}


	/** Returns the operation bound to the character */
	public static OpInstruction getOp(char op) {
		if(op >= 33 && op <= 126) {
			return DOT_OPS[op-FIRST_OP];
		} else {
			throw new SyntaxError("Dot operator '." + op + "' does not exist");
		}
	}

}

// ! - 33
class OP_Dot_Bang extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".!");
		doc.desc("N", "signum");
		doc.desc("S", "parse if number");
		doc.ovrld(Ops.KEYVAR_SIGNUM.name());
		OperationDocs.add(doc);
	}

	public OP_Dot_Bang() {
		this.name = ".!";
	}
	@Override public void execute(final Block block) {
		Obj o = block.pop();

		if (o.isa(NUMBER)) {
			block.push(((Number)o).signnum());
		} else if (o.isa(STR)) {
			String numStr = o.str().trim();
			Block b;
			try {
				b = Parser.compile(numStr, Aya.getInstance());
			} catch (Exception e) {
				b = null;
			}

			if (b != null
					&& b.getInstructions().getInstrucionList().size() == 1
					&& DataInstruction.isa(b.getInstructions().peek(0), Obj.NUMBER)) {
				DataInstruction data = (DataInstruction)b.getInstructions().peek(0);
				block.push(data.getData());
			} else {
				block.push(o);
			}
		}
		else if (o.isa(DICT)) {
			block.callVariable((Dict)o, Ops.KEYVAR_SIGNUM);
		}
		else {
			throw new TypeError(this,o);
		}
	}
}

// $ - 36
class OP_Dot_Duplicate extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".$");
		doc.desc("..AN", "copies the Nth item on the stack to the top (not including N)");
		OperationDocs.add(doc);
	}

	public OP_Dot_Duplicate() {
		this.name = "._";
	}
	@Override public void execute (final Block block) {
		final Obj a = block.pop();

		if (a.isa(NUMBER)) {
			int size = block.getStack().size();
			int i = ((Number)a).toInt();

			if (i > size || i <= 0) {
				throw new AyaRuntimeException(i + " ._ stack index out of bounds");
			} else {
				final Obj cp = block.getStack().get(size - i);

				if(cp.isa(LIST)) {
					block.push(((List)cp).deepcopy());
				} else {
					block.push(cp);
				}
			}
		} else {
			throw new TypeError(this, a);
		}
	}
}


// % - 37
class OP_Dot_Percent extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".%");
		doc.desc("NN", "integer division");
		doc.ovrld(Ops.KEYVAR_IDIV.name(), Ops.KEYVAR_RIDIV.name());
		OperationDocs.add(doc);
	}

	public OP_Dot_Percent() {
		this.name = ".%";
	}
	@Override public void execute(final Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		if(a.isa(NUMBER) && b.isa(NUMBER)) {
			try {
				//b idiv a
				block.push(((Number)(b)).idiv((Number)(a)));
			} catch (ArithmeticException e) {
				throw new AyaRuntimeException("%: Divide by 0");
			}
		}
		else if (a.isa(NUMBERLIST) && b.isa(NUMBER)) {
			block.push( ((NumberList)a).idivFrom((Number)b) );
		}

		else if (a.isa(NUMBER) && b.isa(NUMBERLIST)) {
			block.push( ((NumberList)b).idiv((Number)a) );
		}
		else if (a.isa(NUMBERLIST) && b.isa(NUMBERLIST)) {
			block.push( ((NumberList)b).idiv((NumberList)a) );
		}

		else if (a.isa(DICT)) {
			block.push(b);
			block.callVariable((Dict)a, Ops.KEYVAR_IDIV);
		} else if (b.isa(DICT)) {
			block.callVariable((Dict)b, Ops.KEYVAR_RIDIV, a);
		}
		else {
			throw new TypeError(this, a,b);
		}
	}
}



// & - 38
class OP_Dot_And extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".&");
		doc.desc("SSS", "replace all occurances of the regex S1 with S2 in S3");
		doc.desc("LLB", "zip with");
		OperationDocs.add(doc);
	}

	public OP_Dot_And() {
		this.name = ".&";
	}
	@Override public void execute(final Block block) {
		Obj a = block.pop();  // str
		Obj b = block.pop();  // replace
		Obj c = block.pop();  // find

		if ( a.isa(STR) && (b.isa(STR) || b.isa(CHAR)) && (c.isa(STR) || c.isa(CHAR))) {
			block.push(new Str( a.str().replaceAll(c.str(), b.str()) ));
		} else if (a.isa(BLOCK) && b.isa(LIST) && c.isa(LIST)) {
			Block initial = new Block();
			initial.push(c);
			initial.push(b);
			ListBuilder lb = new ListBuilder(initial, (Block)a, null, 0);
			block.add(lb);

		} else {
			throw new TypeError(this,a,b,c);
		}
	}
}

// ' - 39
class OP_Dot_CastChar extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".'");
		doc.desc("N|S", "cast to char");
		doc.desc("L", "convert number list to string using UTF-8 encoding");
		OperationDocs.add(doc);
	}

	public OP_Dot_CastChar() {
		this.name = ".'";
	}
	@Override public void execute(final Block block) {
		Obj o = block.pop();

		if (o.isa(NUMBER)) {
			block.push( Char.valueOf(((Number)o).toInt()) );
		} else if (o.isa(STR)) {
			block.push( Char.valueOf(o.str().charAt(0)) );
		} else if (o.isa(LIST)) {
			block.push( Str.fromBytes(((List)o).toNumberList().toByteArray()) );
		}else if (o.isa(CHAR)) {
			block.push(o);
		} else {
			throw new TypeError(this,o);
		}
	}
}


// ( - 40
class OP_Dot_OParen extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".(");
		doc.desc("NN", "left bitwise shift");
		OperationDocs.add(doc);
	}

	public OP_Dot_OParen() {
		this.name = ".(";
	}
	@Override
	public void execute(Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		if (a.isa(NUMBER) && b.isa(NUMBER)) {
			// Reverse ops
			block.push( NumberMath.leftShift((Number)b, (Number)a) );
		} else {
			throw new TypeError(this, a, b);
		}
	}
}

// ) - 41
class OP_Dot_CParen extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".)");
		doc.desc("NN", "signed right bitwise shift");
		OperationDocs.add(doc);
	}

	public OP_Dot_CParen() {
		this.name = ".)";
	}
	@Override
	public void execute(Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		if (a.isa(NUMBER) && b.isa(NUMBER)) {
			// Reverse ops
			block.push( NumberMath.signedRightShift((Number)b, (Number)a) );
		} else {
			throw new TypeError(this, a, b);
		}
	}
}


// + - 43
class OP_Dot_Plus extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".+");
		doc.desc("NN", "gdc");
		doc.desc("BD", "swap vars in a copy of B for values defined in D");
		doc.desc("BJ", "constant capture variable from outer scope");
		doc.desc("BL<J>", "constant capture variables from outer scope");
		doc.desc("DD", "update D1 with the values from D2 (modify D1)");
		OperationDocs.add(doc);
	}

	public OP_Dot_Plus() {
		this.name = ".+";
	}
	private void capture(Block b, Symbol s) {
		Obj o = Aya.getInstance().getVars().getVar(s.id());
		b.getInstructions().assignVarValue(s.id(), o);
	}
	@Override
	public void execute(Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		// GCD
		if (a.isa(NUMBER) && b.isa(NUMBER)) {
			block.push(NumberMath.gcd((Number)a, (Number)b));
		}
		else if (b.isa(BLOCK)) {
			Block blk = (Block)(b.deepcopy());
			// Constant capture from dict
			if (a.isa(DICT)) {
				Dict.assignVarValues((Dict)a, blk);
			}
			// Constant capture from scope
			else if (a.isa(SYMBOL)) {
				capture(blk, (Symbol)a);
			}
			// Constant capture from scope (list)
			else if (a.isa(LIST)) {
				List l = (List)a;
				for (int i = 0; i < l.length(); i++) {
					final Obj s = l.get(i);
					if (s.isa(SYMBOL)) {
						capture(blk, (Symbol)s);
					} else {
						throw new AyaRuntimeException(".+ Expected list of symbols. Got:\n" + a.repr());
					}
				}
			}
			
			block.push(blk);
		}
		else if (a.isa(DICT) && b.isa(DICT)) {
			((Dict)b).update((Dict)a);
			block.push(b);
		}
		
		
		else {
			throw new TypeError(this, a, b);
		}
	}
}

// - - 45
class OP_Dot_Minus extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".-");
		doc.desc("NN", "lcm");
		doc.desc("LN", "remove item at index N from L");
		doc.desc("LL", "remove items at indices L1 from L2");
		doc.desc("DJ", "remove key from dict");
		doc.desc("DS", "remove key from dict");
		OperationDocs.add(doc);
	}

	public OP_Dot_Minus() {
		this.name = ".-";
	}

	public boolean rmFromDict(Dict d, Obj idx) {
		if (idx.isa(SYMBOL)) {
			d.remove(((Symbol)idx).id());
		} else if (idx.isa(STR)) {
			d.remove(idx.str());
		} else if (idx.isa(LIST)) {
			List l = (List)idx;
			for (int i = 0; i < l.length(); i++) {
				if (!rmFromDict(d, l.get(i))) return false;
			}
		} else {
			return false;
		}
		
		return true;
	}

	@Override
	public void execute(Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		if (a.isa(NUMBER) && b.isa(NUMBER)) {
			block.push(NumberMath.lcm((Number)a, (Number)b));
		} else if (b.isa(LIST) && a.isa(NUMBER)) {
			((List)b).remove( ((Number)a).toInt() );
			block.push(b);
		} else if (a.isa(LIST) && b.isa(LIST)) {
			List l = (List)a;
			NumberList ns = l.toNumberList();
			((List)b).removeAll(ns.toIntegerArray());
			block.push(b);
		} else if (b.isa(DICT)) {
			Dict d = (Dict)b;
			if (!rmFromDict(d, a)) {
				throw new TypeError(this, a, b);
			}
			block.push(d);
		} else {
			throw new TypeError(this, a, b);
		}
	}
}

// / - 47
class OP_Dot_FwdSlash extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', "./");
		doc.desc("N", "ceiling");
		doc.ovrld(Ops.KEYVAR_CEIL.name());
		OperationDocs.add(doc);
	}

	public OP_Dot_FwdSlash() {
		this.name = "./";
	}
	@Override
	public void execute(Block block) {
		final Obj a = block.pop();

		if (a.isa(NUMBER)) {
			block.push(((Number)a).ceil());
		}

		else if (a.isa(NUMBERLIST)) {
			block.push( ((NumberList)a).ceil() );
		}

		else if (a.isa(DICT)) {
			block.callVariable((Dict)a, Ops.KEYVAR_CEIL);
		}

		else {
			throw new TypeError(this, a);
		}
	}
}

// ; - 59
class OP_Dot_ClearAll extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".;");
		doc.desc("..A", "clear the entire stack");
		OperationDocs.add(doc);
	}

	public OP_Dot_ClearAll() {
		this.name = ".;";
	}
	@Override
	public void execute(Block block) {
		block.clearStack();
	}
}

// < - 60
class OP_Dot_LessThan extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".<");
		doc.desc("LN", "head / pad 0");
		doc.desc("SN", "head / pad ' '");
		doc.desc("NN|SS|CC", "lesser of");
		doc.ovrld(Ops.KEYVAR_HEAD.name());
		OperationDocs.add(doc);
	}

	public OP_Dot_LessThan() {
		this.name = ".<";
	}
	@Override
	public void execute(Block block) {
		Obj b = block.pop();			// Popped in Reverse Order
		Obj a = block.pop();


		if (b.isa(NUMBER) && a.isa(LIST)) {
			block.push(((List)a).head(((Number)b).toInt()));
		}

		else if (a.isa(NUMBER) && b.isa(NUMBER)) {
			if ( ((Number)a).compareTo((Number)b) > 0) {
				block.push(a);
			} else {
				block.push(b);
			}
		}

		else if (a.isa(STR) && b.isa(STR)) {
			if ( ((Str)a).compareTo((Str)b) > 0) {
				block.push(a);
			} else {
				block.push(b);
			}
		}

		else if (a.isa(CHAR) && b.isa(CHAR)) {
			if ( ((Char)a).compareTo((Char)b) > 0) {
				block.push(a);
			} else {
				block.push(b);
			}
		}

		else if (a.isa(DICT)) {
			block.callVariable((Dict)a, Ops.KEYVAR_HEAD, b);
		}

		else {
			throw new TypeError(this, a, b);
		}
	}
}


// > - 62
class OP_Dot_GreaterThan extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".>");
		doc.desc("LN", "tail / pad 0");
		doc.desc("SN", "tail / pad ' '");
		doc.desc("NN|CC|SS", "greater of");
		doc.ovrld(Ops.KEYVAR_TAIL.name());
		OperationDocs.add(doc);
	}

	public OP_Dot_GreaterThan() {
		this.name = ".>";
	}
	@Override
	public void execute(Block block) {
		Obj b = block.pop();			// Popped in Reverse Order
		Obj a = block.pop();

		if (b.isa(NUMBER) && a.isa(LIST)) {
		block.push( ((List)a).tail(((Number)b).toInt()) );
		}

		else if (a.isa(NUMBER) && b.isa(NUMBER)) {
			if ( ((Number)a).compareTo((Number)b) < 0) {
				block.push(a);
			} else {
				block.push(b);
			}
		}

		else if (a.isa(STR) && b.isa(STR)) {
			if ( ((Str)a).compareTo((Str)b) < 0) {
				block.push(a);
			} else {
				block.push(b);
			}
		}

		else if (a.isa(CHAR) && b.isa(CHAR)) {
			if ( ((Char)a).compareTo((Char)b) < 0) {
				block.push(a);
			} else {
				block.push(b);
			}
		}

		else if (a.isa(DICT)) {
			block.callVariable((Dict)a, Ops.KEYVAR_TAIL, b);
		}

		else {
			throw new TypeError(this, a, b);
		}

	}
}

// = 61 new OP_Dot_Equals(),
class OP_Dot_Equals extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".=");
		doc.desc("LL|AL|LA", "element-wise equivalence");
		OperationDocs.add(doc);
	}

	public OP_Dot_Equals() {
		this.name = ".=";
	}

	@Override
	public void execute(final Block block) {
		final Obj a = block.pop();
		final Obj b = block.pop();

		if (a.isa(DICT) && b.isa(DICT)) {
			block.push(a.equiv(b) ? Num.ONE : Num.ZERO);
		}

		else if (a.isa(LIST) && b.isa(LIST)) {
			block.push( List.equalsElementwise((List)a, (List)b) );
		}

		else if ( a.isa(LIST) ) {
			block.push( List.equalsElementwise((List)a, b) );
		}

		else if ( b.isa(LIST) ) {
			block.push( List.equalsElementwise((List)b, a) );
		}

		else {
			throw new TypeError(this, a, b);
		}
	}

}


// ? - 63
class OP_Dot_Conditional extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".?");
		doc.desc("AAA", "if A1 then A2, else A3. If A2/A3 are blocks, execute");
		OperationDocs.add(doc);
	}

	public OP_Dot_Conditional() {
		this.name = ".?";
	}

	@Override
	public void execute(final Block block) {
		final Obj a = block.pop();
		final Obj b = block.pop();
		final Obj c = block.pop();
		//  c     b      a
		// cond {then} {else}

		if(c.bool()) {
			if(b.isa(BLOCK)) {
				block.addAll(((Block)b).duplicate().getInstructions().getInstrucionList());
			} else {
				block.push(b);
			}
		} else {
			if(a.isa(BLOCK)) {
				block.addAll(((Block)a).duplicate().getInstructions().getInstrucionList());
			} else {
				block.push(a);
			}
		}
	}

}

// @ - 64
class OP_Dot_At extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".@");
		doc.desc("..AN", "moves the Nth item on the stack (not including N) to the top");
		OperationDocs.add(doc);
	}

	public OP_Dot_At() {
		this.name = ".@";
	}
	@Override public void execute (final Block block) {
		final Obj a = block.pop();

		if (a.isa(NUMBER)) {
			int size = block.getStack().size();
			int i = ((Number)a).toInt();

			if (i > size || i <= 0) {
				throw new AyaRuntimeException(i + " .@ stack index out of bounds");
			} else {
				final Obj cp = block.getStack().get(size - i);
				block.getStack().remove(size - i);
				block.push(cp);
			}
		} else {
			throw new TypeError(this, a);
		}
	}
}


// A - 65
class OP_Dot_ArrayAll extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".A");
		doc.desc("..A", "wrap entire stack in a list");
		OperationDocs.add(doc);
	}

	public OP_Dot_ArrayAll() {
		this.name = ".A";
	}
	@SuppressWarnings("unchecked")
	@Override
	public void execute(Block block) {
		ArrayList<Obj> list = new ArrayList<Obj>();
		list.addAll((Stack<Obj>)block.getStack().clone());
		block.clearStack();
		block.push(new GenericList(list));
	}
}

// B - 66
class OP_Dot_Append extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".B");
		doc.desc("AL", "append item to the back of a list");
		OperationDocs.add(doc);
	}

	public OP_Dot_Append() {
		this.name = ".B";
	}
	@Override
	public void execute(Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		if (a.isa(LIST)) {
			((List)a).addItem(b);
			block.push(a);
		} else {
			throw new TypeError(this, a, b);
		}
	}
}

// C - 67
class OP_Dot_SortUsing extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".C");
		doc.desc("LB", "sort least to greatest by applying B to L");
		doc.desc("NN", "xor");
		OperationDocs.add(doc);
	}

	public OP_Dot_SortUsing() {
		this.name = ".C";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override public void execute(Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		if (a.isa(NUMBER) && b.isa(NUMBER)) {
			block.push(NumberMath.bxor((Number)a, (Number)b));
		}
		else if (a.isa(BLOCK) && b.isa(LIST)) {
			final Block blk = ((Block)a).duplicate();
			List objs = ((List)b);
			List key_obj = blk.mapTo(objs);

			//Convert keys to int array
			ArrayList<SUItem> items = new ArrayList<SUItem>(key_obj.length());
			try {

				for (int i = 0; i < objs.length(); i++) {
					items.add(new SUItem(objs.get(i), (Comparable) key_obj.get(i)));
				}
				Collections.sort(items);

			} catch (ClassCastException e) {
				throw new AyaRuntimeException(".C: all objects must be comparable to each other");
			}



			ArrayList<Obj> out = new ArrayList<Obj>(items.size());
			for (SUItem i : items) {
				out.add(i.o);
			}

			block.push(new GenericList(out).promote());

		}
		else {
			throw new TypeError(this, a);
		}
	}

	@SuppressWarnings("rawtypes")
	class SUItem<T extends Comparable> implements Comparable<SUItem<T>>{
		public Obj o;
		public T d;
		public SUItem(Obj o, T d) {
			this.o = o;
			this.d = d;
		}
		@SuppressWarnings("unchecked")
		public int compareTo(SUItem<T> i) {
			return d.compareTo(i.d);
		}
	}



}

// D - 68
class OP_Dot_Error extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".D");
		doc.desc("S", "interrupts the program and throws an error message");
		OperationDocs.add(doc);
	}

	public OP_Dot_Error() {
		this.name = ".D";
	}
	@Override public void execute (Block block) {
		Obj a = block.pop();

		if(a.isa(STR)) {
			throw new AyaUserRuntimeException(a.str());
		} else {
			throw new TypeError(this, a);
		}
	}
}

//E - 69
class OP_Dot_Len extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".E");
		doc.desc("L", "length, keep list on stack");
		doc.ovrld(Ops.KEYVAR_LEN.name());
		OperationDocs.add(doc);
	}

	public OP_Dot_Len() {
		this.name = ".E";
	}
	@Override
	public void execute(Block block) {
		final Obj n = block.pop();

		if (n.isa(LIST)) {
			block.push(n);
			block.push( new Num(((List)n).length()) );
		}
		else if (n.isa(DICT)) {
			block.push(n);
			block.callVariable((Dict)n, Ops.KEYVAR_LEN);
		}
		else {
			throw new TypeError(this, n);
		}
	}
}



//F - 70
class OP_Dot_Flatten extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".F");
		doc.desc("L", "flatten nested list");
		OperationDocs.add(doc);
	}

	public OP_Dot_Flatten() {
		this.name = ".F";
	}
	@Override
	public void execute(Block block) {
		final Obj n = block.pop();

		if (n.isa(LIST)) {
			if (n.isa(STR) || n.isa(NUMBERLIST)) {
				block.push(n.deepcopy());
			} else {
				block.push(List.flatten((GenericList)n));
			}
		} else {
			throw new TypeError(this, n);
		}
	}
}




// G - 71
class OP_Dot_Write extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".G");
		doc.desc("ASN", "write A as a string to file located at S. N = 0, overwrite. N = 1, append");
		OperationDocs.add(doc);
	}

	public OP_Dot_Write() {
		this.name = ".G";
	}
	@Override
	public void execute(Block block) {
		final Obj n = block.pop();
		final Obj s = block.pop();
		final Obj a = block.pop();

		if (s.isa(STR) && n.isa(NUMBER)) {
			final int option = ((Number)n).toInt();
			final String filename = s.str();
			final String write = a.str();
			final String fstr = AyaPrefs.getWorkingDir()+filename;


			if(option == 0) {
				try {
				    Files.write(Paths.get(fstr), write.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				}catch (IOException e) {
				    throw new AyaRuntimeException("Cannot open file '" + fstr + "'");
				} catch (InvalidPathException ipe) {
					throw new AyaRuntimeException("Cannot open file '" + fstr + "'");
				}
			}

			else if (option == 1) {
				try {
				    Files.write(Paths.get(fstr), write.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				}catch (IOException e) {
				    throw new AyaRuntimeException("Cannot open file '" + fstr + "'");
				}  catch (InvalidPathException ipe) {
					throw new AyaRuntimeException("Cannot open file '" + fstr + "'");
				}
			}

			else {
			    throw new AyaRuntimeException(".U: Option " + option + "is not valid. Please use 0 for overwrite and 1 for append");
			}
			return;
		}
		throw new TypeError(this, n);
	}
}

// I - 73
//NOTE: If updating this operator, also update I
class OP_Dot_I extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".I");
		doc.desc("LN|LL", "index, keep list on stack");
		doc.desc("LB", "filter, keep list on stack");
		OperationDocs.add(doc);
	}

	public OP_Dot_I() {
		this.name = ".I";
	}
	@Override public void execute (final Block block) {
		Obj index = block.pop();
		final Obj list = block.pop();
		block.push(list); //.I keeps the list on the stack

		if(list.isa(LIST)) {		
			block.push(List.getIndex((List)list, index));
		}else if (list.isa(DICT)) {
			if (index.isa(LIST) && !index.isa(STR)) {
				List l = (List)index;
				if (l.length() == 1)
					index = l.get(0);
			}
			
			if ( ((Dict)list).hasMetaKey(Ops.KEYVAR_GETINDEX) ) {
				block.push(index);
				block.callVariable((Dict)list, Ops.KEYVAR_GETINDEX);
			} else {
				block.push(Dict.getIndex((Dict)list, index));
			}
		}
		else {
			throw new TypeError(this, index, list);
		}
	}
}

//K - 75
class OP_Dot_TryCatch extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".K");
		doc.desc("BB", "try B1, if error, execute B2. Neither block has access to the global stack");
		OperationDocs.add(doc);
	}

	public OP_Dot_TryCatch() {
		this.name = ".K";
	}
	@Override public void execute (Block block) {
		Obj catchBlock = block.pop();
		Obj tryBlock = block.pop();

		if(tryBlock.isa(BLOCK) && catchBlock.isa(BLOCK)) {
			try {
				Block b = ((Block)tryBlock).duplicate();
				b.eval();
				block.appendToStack(b.getStack());
			} catch (Exception e) {
				Block b = ((Block)catchBlock).duplicate();
				b.push(new Str(Aya.exToString(e)));
				b.eval();
				block.appendToStack(b.getStack());
			}
		}
	}
}


//M- 77
class OP_Dot_M extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".M");
		doc.desc("D", "get D's meta, create one if it does not exist");
		OperationDocs.add(doc);
	}

	public OP_Dot_M() {
		this.name = ".M";
	}
	
	public static final long ARGS = Variable.encodeString("args");
	public static final long ARGTYPES = Variable.encodeString("argtypes");
	public static final long LOCALS = Variable.encodeString("locals");
	
	@Override public void execute (final Block block) {
		Obj a = block.pop();
		 if (a.isa(DICT)) {
			block.push(((Dict)a).getMetaDict());
		} else if (a.isa(BLOCK)) {
			block.push(getBlockMeta((Block)a));
		} else {
			throw new TypeError(this, a);
		}
	}
	private Dict getBlockMeta(Block b) {
		Dict d = new Dict();
		// Arg Names
		final ArrayList<Pair<Symbol, Symbol>> args_and_types = b.getArgsAndTypes();
		ArrayList<Obj> argnames = new ArrayList<>(args_and_types.size());
		Dict argtypes = new Dict();
		
		for (Pair<Symbol, Symbol> p : args_and_types) {
			argnames.add(p.first());
			argtypes.set(p.first().id(), p.second());
		}
		d.set(ARGS, new GenericList(argnames));
		d.set(ARGTYPES, argtypes);
		final VariableSet vars = b.getLocals();
		if (vars != null) {
			d.set(LOCALS, new Dict(vars));
		}
				
		return d;
	}
}

// N - 78
class OP_Dot_N extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".N");
		doc.desc("LB", "return the index of the first element of L that satifies E");
		OperationDocs.add(doc);
	}

	public OP_Dot_N() {
		this.name = ".N";
	}
	@Override public void execute (final Block block) {
		final Obj a = block.pop(); //Block
		final Obj b = block.pop(); //List

		int index = 0;
		if(b.isa(LIST) && a.isa(BLOCK)) {

			block.push(b); //Push the list

			final Block blk = (Block)a;
			List l = (List)b;
			for (int i = 0; i < l.length(); i++) {
				Block cond = blk.duplicate();
				cond.push(l.get(i));
				cond.eval();
				Obj result = cond.pop();
				if (result.bool()) {
					block.push(new Num(index)); // ..and the index
					return;
				}
				index++;
			}
			block.push(new Num(-1));
		} else {
			throw new TypeError(this, a, b);
		}
	}
}


// P - 80
class OP_Dot_Print extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".P");
		doc.desc("A", "print to stdout");
		OperationDocs.add(doc);
	}

	public OP_Dot_Print() {
		this.name = ".P";
	}
	@Override public void execute (Block block) {
		Aya.getInstance().print(block.pop().str());
	}
}

// Q - 81
class OP_Dot_Rand extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".Q");
		doc.desc("-", "return a random decimal from 0 to 1");
		OperationDocs.add(doc);
	}

	public OP_Dot_Rand() {
		this.name = ".Q";
	}
	@Override public void execute (Block block) {
		block.push(new Num(Ops.RAND.nextDouble()));
	}
}

// R - 82
class OP_Dot_R extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".R");
		doc.desc("N", "range [0, 1, .., N-1]");
		OperationDocs.add(doc);
	}

	public OP_Dot_R() {
		this.name = ".R";
	}
	@Override public void execute (Block block) {
		final Obj a = block.pop();

		if (a.isa(NUMBER)) {
			final Number n = (Number)a;
			if (n.compareTo(Num.ZERO) == 0) {
				// 0 .R => [ ]
				block.push(new GenericList(new ArrayList<Obj>()));
			} else if (n.compareTo(Num.ZERO) > 0) {
				// +N .R => [0 1 2 ... N-1]
				block.push( ListRangeUtils.buildRange(Num.ZERO, n.dec()) );
			} else {
				// -N .R => [N+1 ... -1 0]
				block.push( ListRangeUtils.buildRange(n.inc(), Num.ZERO) );
			}
		} else {
			throw new TypeError(this, a);
		}
	}
}


// S - 83
class OP_Dot_Case extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".S");
		doc.desc(".S", "returns the first element of a list. if the first element is a block, evaluate");
		OperationDocs.add(doc);
	}

	public OP_Dot_Case() {
		this.name = ".S";
	}
	@Override public void execute (Block block) {
		Obj a = block.pop();

		//Return the first element of a list
		//If block, dump it
		if(a.isa(LIST)) {
			List l = (List)a;
			if(l.length() == 0)
				throw new AyaRuntimeException(this.name + ": list contains no elements");

			Obj item = l.get(0);
			if(item.isa(BLOCK)) {
				block.addAll(((Block)item).getInstructions().getInstrucionList());
			} else {
				block.push(item);
			}
		} else {
			throw new TypeError(this, a);
		}
	}
}

//T - 84
class OP_Dot_T extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".T");
		doc.desc("L", "transpose a 2d list");
		OperationDocs.add(doc);
	}

	public OP_Dot_T() {
		this.name = ".T";
	}
	@Override public void execute (Block block) {
		final Obj a = block.pop();

		if (a.isa(LIST)) {
			block.push( List.transpose((List)a) );
		}

		else {
			throw new TypeError(this, a);
		}
	}
}

// U - 85
class OP_RequestString extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".U");
		doc.desc("S", "requests a string using a ui dialog, S is the prompt text");
		OperationDocs.add(doc);
	}

	public OP_RequestString() {
		this.name = ".U";
	}
	@Override
	public void execute(Block block) {

		block.push(new Str(QuickDialog.requestString(block.pop().str())));

	}
}

// V - 86
class OP_Dot_AppendBack extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".V");
		doc.desc("AL", "append item to back of list");
		OperationDocs.add(doc);
	}

	public OP_Dot_AppendBack() {
		this.name = ".V";
	}
	@Override
	public void execute(Block block) {
		Obj a = block.pop();
		Obj b = block.pop();

		if (a.isa(LIST)) {
			((List)a).addItem(0, b);
			block.push(a);
		} else {
			throw new TypeError(this, a, b);
		}
	}
}

// W - 87
class OP_Dot_W extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".W");
		doc.desc("D", "export variables only if they exist in the most local scope");
		OperationDocs.add(doc);
	}

	public OP_Dot_W() {
		this.name = ".W";
	}
	@Override
	public void execute(Block block) {
		final Obj a = block.pop();

		if (a.isa(DICT)) {
			final Dict d = (Dict)a;
			Aya.getInstance().getVars().peek().mergeDefined(d.getVarSet());
		} else {
			throw new TypeError(this, a);
		}
	}
}


// Z - 90
class OP_Dot_Zed extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".Z");
		doc.desc("S|C|J", "dereference variable");
		OperationDocs.add(doc);
	}

	public OP_Dot_Zed() {
		this.name = ".Z";
	}
	@Override public void execute (Block block) {
		Obj s = block.pop();

		if(s.isa(STR) || s.isa(CHAR)) {
			String str = s.str();
			if(str.contains(".")) {
				throw new AyaRuntimeException(".Z: Cannot look up dictionary variables");
			}
			Variable v = new Variable(str);
			block.push(Aya.getInstance().getVars().getVar(v));
			return;
		} else if (s.isa(SYMBOL)) {
			block.push(Aya.getInstance().getVars().getVar( ((Symbol)s).id() ));
		} else {
			throw new TypeError(this, s);
		}
	}
}



// \ - 92
class OP_Dot_BackSlash extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".\\");
		doc.desc("N", "floor");
		doc.ovrld(Ops.KEYVAR_FLOOR.name());
		OperationDocs.add(doc);
	}

	public OP_Dot_BackSlash() {
		this.name = ".\\";
	}
	@Override
	public void execute(Block block) {
		final Obj a = block.pop();

		if (a.isa(NUMBER)) {
			block.push(((Number)a).floor());
		}

		else if (a.isa(NUMBERLIST)) {
			block.push( ((NumberList)a).floor() );
		}

		else {
			throw new TypeError(this, a);
		}
	}
}

// ^ - 94
class OP_Dot_Pow extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".^");
		doc.desc("L", "compile");
		OperationDocs.add(doc);
	}

	public OP_Dot_Pow() {
		this.name = ".^";
	}
	
	@Override
	public void execute(Block block) {
		final Obj a = block.pop();

		if (a.isa(LIST)) {
			ArrayList<Obj> l = ((List)a).getObjAL();
			Block b = new Block();
			for (int i = 0; i < l.size(); i++) {
				final Obj k = l.get(i);
				if (k.isa(BLOCK)) {
					b.addBlockBack((Block)k);
				} else {
					b.addBack(l.get(i));
				}
			}
			block.push(b);
		}
		else {
			throw new TypeError(this, a);
		}
	}
}



// _ - 95


// | - 124
class OP_Dot_Bar extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".|");
		doc.desc("N", "absolute value");
		doc.ovrld(Ops.KEYVAR_ABS.name());
		doc.vect();
		OperationDocs.add(doc);
	}

	public OP_Dot_Bar() {
		this.name = ".|";
	}
	@Override
	public void execute(final Block block) {
		final Obj a = block.pop();

		if (a.isa(NUMBER)) {
			block.push( ((Number)a).abs() );
		} else if (a.isa(NUMBERLIST)) {
			block.push( ((NumberList)a).abs() );
		} else if (a.isa(DICT)) {
			block.callVariable((Dict)a, Ops.KEYVAR_ABS);
		} else {
			throw new TypeError(this, a);
		}
	}
}




// ~ - 126
class OP_Dot_Tilde extends OpInstruction {

	static {
		OpDoc doc = new OpDoc('.', ".~");
		doc.desc("S", "parse contents to a block");
		doc.desc("J|C", "deref variable; if not a block, put contents in block");
		OperationDocs.add(doc);
	}

	public OP_Dot_Tilde() {
		this.name = ".~";
	}
	@Override
	public void execute(final Block block) {
		final Obj a = block.pop();

		if (a.isa(STR)) {
			block.push( Parser.compile(a.str(), Aya.getInstance()) );
			return;
		} else if (a.isa(CHAR)) {
			final char c = ((Char)a).charValue();
			final String varname = CharacterParser.getName(c);
			if(varname == null) {
				throw new AyaRuntimeException("Character '" + c + " is not a valid variable");
			}

			Obj e = Aya.getInstance().getVars().getVar(Variable.encodeString(varname));
			if (!e.isa(BLOCK)) {
				Block b = new Block();
				b.add(e);
				block.push(b);
			} else {
				block.push(e);
			}
		} else if (a.isa(SYMBOL)) {
			Obj e = Aya.getInstance().getVars().getVar(((Symbol)a).id());
			if (!e.isa(BLOCK)) {
				Block b = new Block();
				b.add(e);
				block.push(b);
			} else {
				block.push(e);
			}
		} else {
			throw new TypeError(this, a);
		}
	}
}
