package aya.ext.json;

import aya.exceptions.runtime.TypeError;
import aya.instruction.named.NamedInstruction;
import aya.obj.Obj;
import aya.obj.block.Block;
import aya.obj.list.List;

public class ToJSONInstruction extends NamedInstruction {
	
	public ToJSONInstruction() {
		super("json.dumps");
		_doc = "Serialize a dict to a json string";
	}

	@Override
	public void execute(Block block) {
		final Obj a = block.pop();
		
		if (!a.isa(Obj.DICT)) {
			throw new TypeError(this, "::dict", a);
		}
		
		block.push(List.fromString(JSONUtils.encodeJSON(a, JSONUtils.JSONParams.getDefaultEncode())));
	}

}
