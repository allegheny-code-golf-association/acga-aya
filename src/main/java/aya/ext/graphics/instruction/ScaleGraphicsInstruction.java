package aya.ext.graphics.instruction;

import aya.ext.graphics.Canvas;
import aya.ext.graphics.CanvasTable;
import aya.ext.graphics.GraphicsInstruction;
import aya.obj.block.Block;

public class ScaleGraphicsInstruction extends GraphicsInstruction {

	public ScaleGraphicsInstruction(CanvasTable canvas_table) {
		super(canvas_table, "scale", "NNN");
		_doc = "x y canvas_id: scale the canvas";
	}

	@Override
	protected void doCanvasCommand(Canvas cvs, Block block) {
		double y = _reader.popDouble();
		double x = _reader.popDouble();
	
		cvs.getG2D().scale(x, y);
	
		cvs.refresh();
	}
}



