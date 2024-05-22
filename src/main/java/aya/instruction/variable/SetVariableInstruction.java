package aya.instruction.variable;

import aya.Aya;
import aya.ReprStream;
import aya.obj.block.Block;
import aya.obj.symbol.Symbol;

public class SetVariableInstruction extends VariableInstruction {
	
	public SetVariableInstruction(Symbol var) {
		super(var);
	}
	
	@Override
	public void execute(Block b) {
		Aya.getInstance().getVars().setVar(variable_, b.peek());
	}
	
	@Override
	public ReprStream repr(ReprStream stream) {
		stream.print(":" + variable_.name());
		return stream;
	}
}
