package aya.ext.graphics.instruction;

import aya.ext.graphics.Canvas;
import aya.ext.graphics.CanvasTable;
import aya.ext.graphics.GraphicsInstruction;
import aya.obj.block.Block;

public class TextGraphicsInstruction extends GraphicsInstruction {

	public TextGraphicsInstruction(CanvasTable canvas_table) {
		super(canvas_table, "text", "NNNNN");
		_doc = "text x y canvas_id: draw text";
	}
	

	@Override
	protected void doCanvasCommand(Canvas cvs, Block block) {
		int y = _reader.popInt();
		int x = _reader.popInt();
		String text = _reader.popString();

		cvs.getG2D().drawString(text, x, y);	
		
		cvs.refresh();
	}
}



