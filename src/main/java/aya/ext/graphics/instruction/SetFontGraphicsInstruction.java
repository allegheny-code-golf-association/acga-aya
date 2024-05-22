package aya.ext.graphics.instruction;

import java.awt.Font;

import aya.ext.graphics.Canvas;
import aya.ext.graphics.CanvasTable;
import aya.ext.graphics.GraphicsInstruction;
import aya.obj.block.Block;

public class SetFontGraphicsInstruction extends GraphicsInstruction {

	public SetFontGraphicsInstruction(CanvasTable canvas_table) {
		super(canvas_table, "set_font", "SSNN");
		_doc = "name style size canvas_id: set the font";
	}
	

	@Override
	protected void doCanvasCommand(Canvas cvs, Block block) {
		int size = _reader.popInt();
		int style =  strToStyle(_reader.popString());
		String name = _reader.popString();
		
        cvs.getG2D().setFont(new Font(name, style, size));
	}
	
	
	private int strToStyle(String s) {
		switch (s) {
		case "plain": return Font.PLAIN;
		case "bold": return Font.BOLD;
		case "italic": return Font.ITALIC;
		case "bolditalic": return Font.BOLD + Font.ITALIC;
		default: return -1;
		}
	}
	
}



