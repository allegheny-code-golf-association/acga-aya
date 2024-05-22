package aya.parser;

import java.util.ArrayList;

import aya.Aya;
import aya.exceptions.ex.EndOfInputError;
import aya.exceptions.ex.ParserException;
import aya.exceptions.ex.SyntaxError;
import aya.instruction.BlockLiteralInstruction;
import aya.instruction.DataInstruction;
import aya.instruction.Instruction;
import aya.instruction.InstructionStack;
import aya.instruction.ListLiteralInstruction;
import aya.instruction.index.GetExprIndexInstruction;
import aya.instruction.index.GetNumberIndexInstruction;
import aya.instruction.index.GetObjIndexInstruction;
import aya.instruction.index.GetVarIndexInstruction;
import aya.instruction.index.SetExprIndexInstruction;
import aya.instruction.index.SetNumberIndexInstruction;
import aya.instruction.index.SetObjIndexInstruction;
import aya.instruction.index.SetVarIndexInstruction;
import aya.instruction.op.ColonOps;
import aya.instruction.op.OpInstruction;
import aya.instruction.op.Ops;
import aya.instruction.variable.GetKeyVariableInstruction;
import aya.instruction.variable.GetVariableInstruction;
import aya.instruction.variable.QuoteGetKeyVariableInstruction;
import aya.instruction.variable.QuoteGetVariableInstruction;
import aya.instruction.variable.SetKeyVariableInstruction;
import aya.instruction.variable.SetVariableInstruction;
import aya.obj.Obj;
import aya.obj.block.Block;
import aya.obj.list.List;
import aya.obj.number.Number;
import aya.obj.symbol.SymbolTable;
import aya.parser.token.TokenQueue;
import aya.parser.token.TokenStack;
import aya.parser.tokens.BlockToken;
import aya.parser.tokens.CDictToken;
import aya.parser.tokens.CharToken;
import aya.parser.tokens.KeyVarToken;
import aya.parser.tokens.LambdaToken;
import aya.parser.tokens.ListToken;
import aya.parser.tokens.NamedOpToken;
import aya.parser.tokens.NumberToken;
import aya.parser.tokens.OperatorToken;
import aya.parser.tokens.SpecialToken;
import aya.parser.tokens.StdToken;
import aya.parser.tokens.StringToken;
import aya.parser.tokens.SymbolToken;
import aya.parser.tokens.Token;
import aya.parser.tokens.VarToken;
import aya.util.CharUtils;

/**
 * 0. Input String 1. tokenize: Converts characters and character sets to tokens
 * - Parses string and character literals - Identifies Operators - detects dot
 * operators - Identifies opening and closing delimiters 2. assemble: Assembles
 * the tokens into token groups based on context - Assembles list and block
 * literals - parses decimal numbers 3. generate: Generate Aya code based on the
 * tokens
 * 
 * @author Nick
 *
 */
public class Parser {
	
	public static char CDICT_CHAR = (char)162; // cent

	public static TokenQueue tokenize(Aya aya, String s) throws ParserException {
		TokenQueue tokens = new TokenQueue();
		ParserString in = new ParserString(s);

		while (!in.isEmpty()) {
			char current = in.next();

			// Line Comment
			if (current == '.' && in.hasNext() && in.peek() == '#') {
				in.next(); // skip the '#'

				// Help Text
				if (in.hasNext() && in.peek() == '?') {
					in.next(); // Skip the '?'
					StringBuilder sb = new StringBuilder();
					while (in.hasNext() && in.peek() != '\n') {
						sb.append(in.next());
					}
					// Append the last character
					if (in.hasNext()) {
						sb.append(in.next());
					}
					String doc = formatString(sb.toString()).trim();
					aya.addHelpText(doc);
				}

				else {
					while (in.hasNext() && in.peek() != '\n') {
						in.next();
					}
					// Skip the last character
					if (in.hasNext()) {
						in.next();
					}
				}
				continue;
			}

			// Block Comment
			if (current == '.' && in.hasNext() && in.peek() == '{') {
				in.next(); // Skip the '{'

				// Determine if the block is documentation
				boolean isDocCode = false;
				StringBuilder docs = null;
				if (in.hasNext() && in.peek() == '?') {
					in.next(); // Skip the '?'
					isDocCode = true;
					docs = new StringBuilder();

				}

				// Skip or collect the block
				boolean complete = false;
				while (in.hasNext(1)) {
					if (in.peek(0) == '.' && in.peek(1) == '}') {
						in.next();
						in.next(); // Skip the ".}"
						complete = true;
						break;
					}

					if (isDocCode) {
						docs.append(in.next());
					} else {
						in.next();
					}
				}

				// Early input termination
				if (!complete) {
					while (in.hasNext()) {
						in.next();
					}
				}

				// Add the documentation to Aya
				if (isDocCode) {
					String doc = formatString(docs.toString()).trim();
					aya.addHelpText(doc);
				}
			}

			// Number Literals
			else if (Character.isDigit(current)) {
				in.backup();
				tokens.add(parseNumber(in));
			}
			
			// Negative Number Literal
			else if (current == '-' && in.hasNext() && Character.isDigit(in.peek())) {
				in.backup();
				tokens.add(parseNumber(in));
			}
			
			// Negative Decimal Literal (starting with -.)
			else if (current == '-' && in.hasNext()  && in.peek() == '.'
									&& in.hasNext(1) && Character.isDigit(in.peek(1))) {
				in.backup();
				tokens.add(parseNumber(in));
			}

			// Dot (operator/decimal)
			else if (current == '.') {
				if (in.hasNext()) {
					// Decimal
					if (Character.isDigit(in.peek())) {
						in.backup();
						tokens.add(parseNumber(in));
					}

					// Key Variable
					else if (SymbolTable.isBasicSymbolChar(in.peek())) {
						String varname = "" + in.next();
						while (in.hasNext() && SymbolTable.isBasicSymbolChar(in.peek())) {
							varname += in.next();
						}
						tokens.add(new KeyVarToken(varname));
					}

					else if (in.peek() == '"') {
						in.next(); // Skip opening '
						String varname = parseString(in, '"');
						tokens.add(new KeyVarToken(varname));
					}


					// Quote a function (.`)
					else if (in.peek() == '`') {
						tokens.add(SpecialToken.FN_QUOTE);
						in.next(); // Skip the '`'
					}

					// Dot Colon
					else if (in.peek() == ':') {
						tokens.add(SpecialToken.DOT_COLON);
						in.next(); // Skip the ':'
						
						// Quoted variable
						if (in.peek() == '"') {
							in.next(); // Skip open '
							String str = parseString(in, '"');
							tokens.add(new VarToken(str));
						}
					}

					// Plain Dot
					else if (in.peek() == '[') {
						tokens.add(SpecialToken.DOT);
					}

					// Dot operator
					else if (in.peek() <= Ops.MAX_OP){
						tokens.add(new OperatorToken("" + in.next(), OperatorToken.DOT_OP));
					}
					
					else {
						tokens.add(new KeyVarToken(""+in.next()));
					}

				} else {
					throw new SyntaxError("Unexpected end of input after '.'" + in.toString());
				}
			}

			// Math Operators
			else if (current == 'M') {
				try {
					tokens.add(new OperatorToken("" + in.next(), OperatorToken.MATH_OP));
				} catch (EndOfInputError e) {
					throw new SyntaxError("Expected op name after 'M'" + in.toString());
				}
			}
			
			else if (current == Parser.CDICT_CHAR) { // cent
				tokens.add(new CDictToken(""+in.next()));
			}

			// Long String Literals
			else if (current == '"' && in.hasNext(1) && in.peek(0) == '"' && in.peek(1) == '"') {
				StringBuilder str = new StringBuilder();

				// Skip other quote chars
				in.next();
				in.next();

				while (true) {
					// String closed
					if (in.hasNext(2) && in.peek(0) == '"' && in.peek(1) == '"' && in.peek(2) == '"') {

						// false = do not interpolate
						tokens.add(new StringToken(str.toString(), false));

						// Skip closing quotes
						in.next();
						in.next();
						in.next();

						// Exit loop
						break;
					}
					// If there exists a character, add it
					else if (in.hasNext()) {
						str.append(in.next());

					}
					// Incomplete
					else {
						throw new SyntaxError("Incomplete long string literal: " + str.toString());
					}
				}
			}

			// String Literals
			else if (current == '"') {
				String str = parseString(in);
				tokens.add(new StringToken(str));
			}

			// Character Literals
			else if (current == '\'') {
				if (!in.hasNext()) {
					throw new SyntaxError("Expected character name after '''" + in.toString());
				}
				// Special Character
				if (in.peek() == '\\') {
					in.next(); // Skip the \ character
					StringBuilder sb = new StringBuilder();
					boolean complete = false;
					while (in.hasNext()) {
						if (in.peek() == '\'') {
							in.next(); // Skip the closing quote
							complete = true;
							break;
						}
						sb.append("" + in.next());
					}
					if (!complete) {
						throw new SyntaxError("Expected closing quote after character literal '\\" + sb.toString());
					}

					char specialChar;
					if (sb.length() == 0) {
						specialChar = '\\';
					} else {
						specialChar = CharacterParser.parse(sb.toString());
					}

					if (specialChar == CharacterParser.INVALID) {
						throw new SyntaxError("'\\" + sb.toString() + "' is not a valid special character");
					}

					tokens.add(new CharToken("" + specialChar));

				}

				// Normal Character
				else {
					tokens.add(new CharToken("" + in.next()));
				}
			}

			// Variable Name
			else if (SymbolTable.isBasicSymbolChar(current)) {
				StringBuilder sb = new StringBuilder("" + current);
				while (in.hasNext() && SymbolTable.isBasicSymbolChar(in.peek())) {
					sb.append(in.next());
				}
				tokens.add(new VarToken(sb.toString()));
			}

			// Normal Operators
			else if (Ops.isOpChar(current)) {
				tokens.add(new OperatorToken("" + current, OperatorToken.STD_OP));
			}

			// Colon
			else if (current == ':') {
				
				if (in.hasNext()) {

					// Symbol
					if (in.peek() == ':') {
						in.next(); // Move to the next colon
						String sym = "";
						if (in.hasNext() && in.peek() == '"') {
							// Quoted symbol
							in.next(); // Skip '
							sym = parseString(in, '"');
						} else {

							// Fist, try to parse as simple variable
							while (in.hasNext() && SymbolTable.isBasicSymbolChar(in.peek())) {
								sym += in.next();
							}

							// If empty, check for operator or special character
							if (sym.equals("")) {
								if (in.hasNext()) {
									
									// Multi-char operator
									if (isMultiCharOpPrefix(in.peek())) {
										sym = ""+in.next();  // prefix
										sym += ""+in.next(); // op char
									} else {
										// Anything else: (single op, unicode symbol, etc.) single character
										sym = "" + in.next();
									}
									
								} else {
									throw new SyntaxError("Expected symbol name");
								}
							}
						}

						tokens.add(new SymbolToken(sym));
					}
					
					// Named Operator
					else if (in.peek() == '{') {
						in.next(); // Skip '{'
						StringBuilder sb = new StringBuilder();
						boolean done = false;
						while (in.hasNext()) {
							char c = in.next();
							if (c == '}') {
								done = true;
								break;
							} else {
								sb.append(c);
							}
						}
						if (done) {
							tokens.add(new NamedOpToken(sb.toString()));
						} else {
							throw new SyntaxError("Expected '}' after :{" + sb.toString());
						}
					}

					// Colon Pound
					else if (in.peek() == '#') {
						tokens.add(SpecialToken.COLON_POUND);
						in.next(); // Skip the #
					}

					// Quoted variable
					else if (in.peek() == '"') {
						tokens.add(SpecialToken.COLON);
						in.next(); // Skip open '
						String varname = parseString(in, '"');
						tokens.add(new VarToken(varname));
					}

					// Colon Operator
					else if (ColonOps.isColonOpChar(in.peek()) && in.peek() != '{' && in.peek() != '[') {
						tokens.add(new OperatorToken("" + in.next(), OperatorToken.COLON_OP));
					}

					// Special number
					else if (CharUtils.isDigit(in.peek()) || in.peek() == '-') {
						in.backup();
						tokens.add(parseNumber(in));
					}
					
					// Plain Colon
					else {
						tokens.add(SpecialToken.COLON);
					}
				}

				// !hasNext()
				else {
					tokens.add(SpecialToken.COLON);
				}

			} // end colon

			else {
				// Single Character Special Tokens
				SpecialToken tmp = SpecialToken.get(current);
				if (tmp != null) {
					tokens.add(tmp);
				} else if (!Character.isWhitespace(current)){
					// Single character variable
					tokens.add(new VarToken(""+current));
				}
			}
		}

		return tokens;
	}

	public static NumberToken parseNumber(ParserString in) throws EndOfInputError, SyntaxError {
		if (!in.hasNext()) {
			throw new SyntaxError("Attempted to parse empty number string");
		}
		char start = in.next();
		if (start == ':') {
			if (in.hasNext() && (CharUtils.isDigit(in.peek()) || in.peek() == '-')) {
				// Collect the special number
				StringBuilder specNum = new StringBuilder();
				while (in.hasNext()
						&& (CharUtils.isDigit(in.peek()) || CharUtils.isLowerAlpha(in.peek()) || in.peek() == '-' || in.peek() == '.')) {
					if (in.peek() == '.' && in.hasNext() && !CharUtils.isDigit(in.peek(1)))
						break;
					specNum.append(in.next());
				}
				return new NumberToken(specNum.toString(), true);
			} else {
				throw new SyntaxError(in.toString() + " is not a valid number");
			}
		} else if (CharUtils.isDigit(start) || start == '.' || start == '-') {
			StringBuilder num = new StringBuilder("" + start);

			while (in.hasNext() && Character.isDigit(in.peek())) {
				num.append(in.next());
			}

			if (start != '.') { // The start of the number was not a decimal, there may be one now
				// Decimal
				if (in.hasNext() && in.peek(0) == '.' && in.hasNext(1) && Character.isDigit(in.peek(1))) {
					num.append('.');
					in.next(); // Skip the '.'
					while (in.hasNext() && Character.isDigit(in.peek())) {
						num.append(in.next());
					}
				}
			}
			return new NumberToken(num.toString());
		} else {
			throw new SyntaxError(in.toString() + " is not a valid number");
		}
	}

	public static TokenQueue assemble(TokenQueue in) throws EndOfInputError, SyntaxError {
		TokenQueue out = new TokenQueue();

		while (in.hasNext()) {
			Token current = in.next();

			switch (current.getType()) {
			case Token.OPEN_CURLY:
				closeDelim(Token.OPEN_CURLY, Token.CLOSE_CURLY, Token.BLOCK, in, out);
				break;
			case Token.OPEN_SQBRACKET:
				closeDelim(Token.OPEN_SQBRACKET, Token.CLOSE_SQBRACKET, Token.LIST, in, out);
				break;
			case Token.OPEN_PAREN:
				closeDelim(Token.OPEN_PAREN, Token.CLOSE_PAREN, Token.LAMBDA, in, out);
				break;

			// At this point, all delims should be balanced
			// If they aren't, throw an error
			case Token.CLOSE_CURLY:
				throw new SyntaxError("Unexpected token '}'");
			case Token.CLOSE_PAREN:
				throw new SyntaxError("Unexpected token ')'");
			case Token.CLOSE_SQBRACKET:
				throw new SyntaxError("Unexpected token ']'");

			default:
				out.add(current);
			}
		}

		return out;
	}

	/**
	 * Assumes the first delim has been removed input = 1 2 3] output = out.add(new
	 * Token(Token.LIST, data))
	 * @throws EndOfInputError 
	 * @throws SyntaxError 
	 */
	public static void closeDelim(int open, int close, int type, TokenQueue in, TokenQueue out) throws EndOfInputError, SyntaxError {
		TokenQueue innerTokens = new TokenQueue();
		StringBuilder debugStr = new StringBuilder();
		boolean complete = false;
		int brackets = 0;
		while (in.hasNext()) {
			int currentType = in.peek().getType();

			if (currentType == open) {
				brackets++;
			}

			else if (currentType == close) {
				if (brackets == 0) {
					in.next(); // skip the closing delim
					complete = true;
					break;
				} else {
					brackets--;
				}
			}
			debugStr.append(in.peek().getData()).append(" ");
			innerTokens.add(in.next());
		}

		if (!complete) {
			throw new SyntaxError(
					"Expected closing " + SpecialToken.quickString(type) + " delimiter after " + debugStr.toString());
		}

		innerTokens = assemble(innerTokens);

		switch (type) {
		case Token.BLOCK:
			out.add(new BlockToken(debugStr.toString(), innerTokens.getArrayList()));
			break;
		case Token.LIST:
			out.add(new ListToken(debugStr.toString(), innerTokens.getArrayList()));
			break;
		case Token.LAMBDA:
			out.add(new LambdaToken(debugStr.toString(), innerTokens.getArrayList()));
		}

	}

	public static InstructionStack generate(TokenQueue tokens_in) throws ParserException {
		InstructionStack is = new InstructionStack();
		TokenStack stk = new TokenStack(tokens_in);

		while (stk.hasNext()) {
			Token current = stk.pop();

			// COLON
			if (current.isa(Token.COLON)) {
				if (is.isEmpty()) {
					throw new SyntaxError("Expected token after ':' in:\n\t" + tokens_in.toString());
				}
				Instruction next = is.pop();
				// Variable Assignment
				if (next instanceof GetVariableInstruction) {
					GetVariableInstruction v = ((GetVariableInstruction) next);
					is.push(new SetVariableInstruction(v.getSymbol()));
				} else {
					throw new SyntaxError("':' not followed by operator in:\n\t" + tokens_in.toString());
				}
			}

			else if (current.isa(Token.DOT)) {
				if (is.isEmpty()) {
					throw new SyntaxError("Expected token after '.' in:\n\t" + tokens_in.toString());
				}
				Instruction next = is.pop();

				if (next instanceof ListLiteralInstruction) {
					List l = ((ListLiteralInstruction) next).toList();
					if (l != null) {
						if (l.length() == 1) {
							Obj first = l.getExact(0);
							if (first.isa(Obj.NUMBER)) {
								is.push(new GetNumberIndexInstruction(((Number)first).toInt()));
							} else {
								is.push(new GetObjIndexInstruction(first));
							}
						} else {
							throw new SyntaxError(
									"Invalid index: " + l.repr() + ": Index must contain exactly one element");
						}
					} else {
						ListLiteralInstruction lli = (ListLiteralInstruction) next;
						ArrayList<Instruction> instructions = lli.getInstructions().getInstrucionList();
						if (instructions.size() == 1 && instructions.get(0) instanceof GetVariableInstruction) {
							// Small optimization for single variable indices
							is.push(new GetVarIndexInstruction(((GetVariableInstruction) instructions.get(0)).getSymbol()));
						} else {
							is.push(new GetExprIndexInstruction(new Block(lli.getInstructions())));
						}
					}
				}
			}

			else if (current.isa(Token.DOT_COLON)) {
				if (is.isEmpty()) {
					throw new SyntaxError("Expected token after '.:' in:\n\t" + tokens_in.toString());
				}
				Instruction next = is.pop();
				// Key Variable Assignment
				if (next instanceof GetVariableInstruction) {
					GetVariableInstruction v = ((GetVariableInstruction) next);
					is.push(new SetKeyVariableInstruction(v.getSymbol()));
				}

				// Index assignment
				else if (next instanceof ListLiteralInstruction) {
					List l = ((ListLiteralInstruction) next).toList();
					if (l != null) {
						if (l.length() == 1) {
							Obj first = l.getExact(0);
							if (first.isa(Obj.NUMBER)) {
								is.push(new SetNumberIndexInstruction(((Number)first).toInt()));
							} else {
								is.push(new SetObjIndexInstruction(first));
							}
						} else {
							throw new SyntaxError(
									"Invalid index: " + l.repr() + ": Index must contain exactly one element");
						}
					} else {
						ListLiteralInstruction lli = (ListLiteralInstruction) next;
						ArrayList<Instruction> instructions = lli.getInstructions().getInstrucionList();
						if (instructions.size() == 1 && instructions.get(0) instanceof GetVariableInstruction) {
							// Small optimization for single variable indices
							is.push(new SetVarIndexInstruction(((GetVariableInstruction) instructions.get(0)).getSymbol()));
						} else {
							is.push(new SetExprIndexInstruction(new Block(lli.getInstructions())));
						}
					}
				}
			}

			// POUND
			else if (current.isa(Token.POUND)) {
				BlockLiteralInstruction blk_ins = captureUntilOp(is, tokens_in);
				is.push(Ops.getOp('#'));
				is.push(blk_ins);
			}

			else if (current.isa(Token.TICK)) {
				BlockLiteralInstruction blk_ins = captureUntilOp(is, tokens_in);
				is.push(blk_ins);
			}

			// COLON POUND
			else if (current.isa(Token.COLON_POUND)) {
				if (is.isEmpty()) {
					throw new SyntaxError("Expected token after ':#' in:\n\t" + tokens_in.toString());
				}
				Instruction next = is.pop();
				// Apply a block to a list or dict
				is.push(ColonOps.getOp('#'));
				is.push(next);
			}

			else if (current.isa(Token.FN_QUOTE)) {
				if (stk.hasNext()) {
					if (stk.peek().isa(Token.VAR)) {
						VarToken t = (VarToken) stk.pop();
						is.push(new QuoteGetVariableInstruction(t.getSymbol()));
					} else if (stk.peek().isa(Token.KEY_VAR)) {
						KeyVarToken t = (KeyVarToken) stk.pop();
						is.push(new QuoteGetKeyVariableInstruction(t.getSymbol()));
					} else {
						throw new SyntaxError("Expected var or keyvar before quote (.`) token");
					}
				} else {
					throw new SyntaxError("Expected var or keyvar before quote (.`) token");
				}
			}

			else if (current.typeString().equals("special")) {
				throw new SyntaxError("Unexpected token in:\n\t" + tokens_in.toString());
			}

			// Std Token
			else {
				is.push(((StdToken) current).getInstruction());
			}

		}

		return is;

	}
	
	private static BlockLiteralInstruction captureUntilOp(InstructionStack is, TokenQueue tokens_in) throws SyntaxError {
		if (is.isEmpty()) {
			throw new SyntaxError("Expected token when assembling block in:\n\t" + tokens_in.toString());
		} else {
			Instruction next = is.pop();

			// Apply a block to a list
			if (next instanceof DataInstruction && ((DataInstruction) next).objIsa(Obj.BLOCK)) {
				throw new SyntaxError("Assertion Failed!!");
			} else if (next instanceof BlockLiteralInstruction) {
				return (BlockLiteralInstruction)next;
			} else {
				// Create a block and apply it to a list
				Block colonBlock = new Block();
				is.push(next); // Add next back in

				while (!is.isEmpty()) {
					Instruction o = is.pop();
					colonBlock.getInstructions().insert(0, o);
					if (o instanceof OpInstruction || o instanceof GetVariableInstruction
							|| o instanceof GetKeyVariableInstruction) {
						break;
					}
				}
				return new BlockLiteralInstruction(colonBlock);
			}
		}
	}
	
	private static String formatString(String input) throws EndOfInputError, SyntaxError {
		ParserString in = new ParserString(input + "\"");
		return parseString(in);
	}

	private static String parseString(ParserString in) throws EndOfInputError, SyntaxError {
		return parseString(in, '"');
	}

	private static String parseString(ParserString in, char termination) throws EndOfInputError, SyntaxError {
		boolean complete = false;
		StringBuilder str = new StringBuilder();
		while (in.hasNext()) {
			char c = in.next();
			if (c == '\\') {
				char escape = in.next();
				switch (escape) {
				case '$':
					str.append("\\$");
					break;
				case '}':
					str.append("}"); // For escaping documented comments
					break;
				case 'n':
					str.append('\n');
					break;
				case 't':
					str.append('\t');
					break;
				case 'r':
					str.append('\r');
					break;
				case 'b':
					str.append('\b');
					break;
				case 'f':
					str.append('\f');
					break;
				case '"':
					str.append('"');
					break;
				case '\\':
					str.append('\\');
					break;
				case '{':
					StringBuilder sc = new StringBuilder(); // Special Char
					boolean specialComplete = false;

					while (in.hasNext()) {
						if (in.peek() == '}') {
							specialComplete = true;
							in.next(); // Skip the closing '}'
							break;
						}
						sc.append(in.next());
					}

					if (!specialComplete) {
						// throw new SyntaxError("Early termination of special character in string
						// literal: " + str.toString());
						// Always return a valid result
						str.append("\\{").append(sc);
					} else {

						// Parse the character
						char specChar = CharacterParser.parse(sc.toString());
						if (specChar == CharacterParser.INVALID) {
							// throw new SyntaxError("'\\" + sc.toString() + "' is not a valid special
							// character");
							// Always return a valid result
							str.append("\\{").append(sc).append("}");
						}

						str.append(specChar);
					}
					break;

				default:
					// throw new SyntaxError("'" + escape + "' is not a valid escape character....
					// Always return a valid result
					str.append('\\').append(escape);
				}
			} else if (c == termination) {
				complete = true;
				break;
			} else {
				str.append(c);
			}
		}
		if (complete) {
			return str.toString();
		} else {
			throw new SyntaxError("Expected closing quote after string \"" + str.toString());
		}
	}



	/**
	 * Compiles a string into a code block using input => tokenize => assemble =>
	 * generate
	 * @throws ParserException 
	 * @throws SyntaxError 
	 * @throws EndOfInputError 
	 */
	public static Block compile(String s, Aya aya) throws EndOfInputError, SyntaxError, ParserException {
		return new Block(generate(assemble(tokenize(aya, s))));
	}

	/**
	 * Compiles a string into instruction stack using input => tokenize => assemble
	 * => generate
	 * @throws ParserException 
	 * @throws SyntaxError 
	 * @throws EndOfInputError 
	 */
	public static InstructionStack compileIS(String s, Aya aya) throws EndOfInputError, SyntaxError, ParserException {
		return generate(assemble(tokenize(aya, s)));
	}

	private static boolean isMultiCharOpPrefix(char c) {
		return c == ':' || c == '.' || c == 'M';
	}

}
