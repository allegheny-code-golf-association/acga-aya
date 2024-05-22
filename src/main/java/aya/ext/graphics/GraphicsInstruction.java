package aya.ext.graphics;

import aya.exceptions.runtime.InvalidReferenceError;
import aya.exceptions.runtime.TypeError;
import aya.exceptions.runtime.ValueError;
import aya.instruction.named.NamedInstruction;
import aya.obj.Obj;
import aya.obj.block.Block;
import aya.util.BlockReader;
import aya.util.Casting;

public abstract class GraphicsInstruction extends NamedInstruction {

	protected CanvasTable _canvas_table;
	private String _arg_type_string;
	protected BlockReader _reader;
	
	public GraphicsInstruction(CanvasTable canvas_table, String name, String arg_type_str) {
		super("graphics." + name);
		_arg_type_string = arg_type_str;
		_canvas_table = canvas_table;
		_reader = new BlockReader(this);
	}
	

	@Override
	public void execute(Block block) {
		_reader.setBlock(block);

		final Obj o_id = block.pop();
		int canvas_id = -1;
		if (o_id.isa(Obj.NUMBER)) {
			canvas_id = Casting.asNumber(o_id).toInt();
		} else {
			throw new TypeError(this, _arg_type_string + " " + _name + " first invalid canvas id ", o_id);
		}

		Canvas cvs = _canvas_table.getCanvas(canvas_id);

		if (cvs == null || !cvs.isOpen()) {
			throw new InvalidReferenceError(this._name, canvas_id);
		}
		
		try {
			doCanvasCommand(cvs, block);
		} catch (IllegalArgumentException e) {
			throw new ValueError(e.getMessage());
		}
		
	}
	
	
	protected abstract void doCanvasCommand(Canvas cvs, Block block);
}



