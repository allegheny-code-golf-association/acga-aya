package aya.ext.graphics.instruction.keyboard;

import aya.ext.graphics.Canvas;
import aya.ext.graphics.CanvasTable;
import aya.ext.graphics.GraphicsInstruction;
import aya.obj.block.Block;
import aya.obj.list.List;
import java.util.stream.Collectors;

public class TypedCharsInstruction extends GraphicsInstruction {

	public TypedCharsInstruction(CanvasTable canvas_table) {
		super(canvas_table, "typed_chars", "N");
		_doc = "canvas_id: returns a list of unicode characters that were typed since the last time this instruction was executed.";
	}

	@Override
	protected void doCanvasCommand(Canvas cvs, Block block) {
		String typedString = cvs.getKeyListener().consumeTypedChars().stream()
				.map(Object::toString)
				.collect(Collectors.joining());

		block.push(List.fromString(typedString));
	}
}
