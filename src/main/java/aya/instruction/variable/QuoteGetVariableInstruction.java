package aya.instruction.variable;

import aya.Aya;
import aya.ReprStream;
import aya.obj.Obj;
import aya.obj.block.Block;
import aya.obj.symbol.Symbol;

public class QuoteGetVariableInstruction extends VariableInstruction {

	public QuoteGetVariableInstruction(Symbol var) {
		super(var);
	}
	
	@Override
	public void execute(Block b) {
		Obj o = Aya.getInstance().getVars().getVar(variable_);
		b.push(o);
	}
	
	/**
	 * If o is a block, dump it's instructions. Otherwise, add it to the stack
	 * @param o
	 * @param b
	 */
	public static void addOrDumpVar(Obj o, Block b) {
		if (o.isa(Obj.BLOCK)) {
			b.getInstructions().addAll(((Block)o).getInstructions().getInstrucionList());
		} else {
			b.push(o);
		}
	}
	
	@Override
	public ReprStream repr(ReprStream stream) {
		stream.print(variable_.name() + ".`");
		return stream;
	}
}
