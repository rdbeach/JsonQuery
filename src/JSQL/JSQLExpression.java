package src.JSQL;

/*
 * Copyright 2012 Udo Klimaschewski
 *
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

/*
 * THIS FILE HAS BEEN MODIFIED FROM ITS ORIGINAL FORM
 *
 * 8/17/2014
 * RObert Beach
 *
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSQLExpression {
	
	
	private static final String KEYWORD_REGEX = "==|<>|!=|<=|>=|&&|\\|\\||=|>|<|\\+|-|\\*|\\^|%|,|/|\\)|\\(|(?<!\\S)and|or(?!\\S)|"
			+ "(\\b)SQRT(\\b)|(\\b)IF(\\b)|(\\b)RANDOM(\\b)|(\\b)MIN(\\b)|(\\b)MAX(\\b)|(\\b)ABS(\\b)|(\\b)ROUND(\\b)|(\\b)FLOOR(\\b)|"
			+ "(\\b)CEILING(\\b)|(\\b)LOG(\\b)|(\\b)SQRT(\\b)|(\\b)SIN(\\b)|(\\b)COS(\\b)|(\\b)TAN(\\b)|(\\b)SINH(\\b)|(\\b)COSH(\\b)|"
			+ "(\\b)TANH(\\b)|(\\b)RAD(\\b)|(\\b)DEG(\\b)";
	
	/**
	 * Definition of PI as a constant, can be used in expressions as variable.
	 */
	public static final BigDecimal PI = new BigDecimal(
			"3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");
	
	public static final int PATH = 1;
	public static final int NUMERIC = 2;
	
	/**
	 * The {@link MathContext} to use for calculations.
	 */
	private MathContext mc = MathContext.DECIMAL32;
	
	/**
	 * The input expression in tokenized form.
	 */
	List<String> tokens;
	/**
	 * The RPN (Reverse Polish Notation) of the expression.
	 */
	private List<String> rpn = null;
	/**
	 * All defined operators with name and implementation.
	 */
	private Map<String, Operator> operators = new HashMap<String, JSQLExpression.Operator>();
	
	
	private  List<String> binaryOperators = new ArrayList<String>();
	
	/**
	 * All defined functions with name and implementation.
	 */
	private Map<String, Function> functions = new HashMap<String, JSQLExpression.Function>();
	
	/**
	 * All with name and value.
	 */
	private Map<String, BigDecimal> variables = new HashMap<String, BigDecimal>();
	
	/**
	 * All non string defined variables with name and value.
	 */
	private Map<String, BigDecimal> numeric_variables = new HashMap<String, BigDecimal>();
	
	/**
	 * All string defined variables with name and value.
	 */
	private Map<String, String> string_variables = new HashMap<String, String>();
	
	/**
	 * What character to use for decimal separators.
	 */
	private final char decimalSeparator = '.';
	/**
	 * What character to use for minus sign (negative values).
	 */
	private final char minusSign = '-';
	/**
	 * The expression evaluators exception class.
	 */
	public class ExpressionException extends RuntimeException {
		private static final long serialVersionUID = 1118142866870779047L;
		public ExpressionException(String message) {
			super(message);
		}
	}
	/**
	 * Abstract definition of a supported expression function. A function is
	 * defined by a name, the number of parameters and the actual processing
	 * implementation.
	 */
	public abstract class Function {
		/**
		 * Name of this function.
		 */
		private String name;
		/**
		 * Number of parameters expected for this function.
		 */
		private int numParams;
		/**
		 * Creates a new function with given name and parameter count.
		 *
		 * @param name
		 * The name of the function.
		 * @param numParams
		 * The number of parameters for this function.
		 */
		public Function(String name, int numParams) {
			this.name = name.toUpperCase();
			this.numParams = numParams;
		}
		public String getName() {
			return name;
		}
		public int getNumParams() {
			return numParams;
		}
		/**
		 * Implementation for this function.
		 *
		 * @param parameters
		 * Parameters will be passed by the expression evaluator as a
		 * {@link List} of {@link BigDecimal} values.
		 * @return The function must return a new {@link BigDecimal} value as a
		 * computing result.
		 */
		public abstract BigDecimal eval(List<Token<?>> parameters);
	}
	/**
	 * Abstract definition of a supported operator. An operator is defined by
	 * its name (pattern), precedence and if it is left- or right associative.
	 */
	public abstract class Operator {
		/**
		 * This operators name (pattern).
		 */
		private String oper;
		/**
		 * Operators precedence.
		 */
		private int precedence;
		/**
		 * Operator is left associative.
		 */
		private boolean leftAssoc;
		/**
		 * Creates a new operator.
		 *
		 * @param oper
		 * The operator name (pattern).
		 * @param precedence
		 * The operators precedence.
		 * @param leftAssoc
		 * <code>true</code> if the operator is left associative,
		 * else <code>false</code>.
		 */
		public Operator(String oper, int precedence, boolean leftAssoc) {
			this.oper = oper;
			this.precedence = precedence;
			this.leftAssoc = leftAssoc;
		}
		public String getOper() {
			return oper;
		}
		public int getPrecedence() {
			return precedence;
		}
		public boolean isLeftAssoc() {
			return leftAssoc;
		}
		/**
		 * Implementation for this operator.
		 *
		 * @param v1
		 * Operand 1.
		 * @param v2
		 * Operand 2.
		 * @return The result of the operation.
		 */
		public abstract BigDecimal eval(Token<?> v1, Token<?> v2);
	}
	/**
	 * Expression tokenizer that allows to iterate over a {@link String}
	 * expression token by token. Blank characters will be skipped.
	 */
	private class Tokenizer implements Iterator<String> {
		/**
		 * Actual position in expression string.
		 */
		private int pos = 0;
		/**
		 * The original input expression.
		 */
		private String input;
		/**
		 * The previous token or <code>null</code> if none.
		 */

		/**
		 * Creates a new tokenizer for an expression.
		 *
		 * @param input
		 * The expression string.
		 */
		public Tokenizer(String input) {
			this.input = input;
		}
		
		@Override
		public String next() {
			return null;
		}
		
		@Override
		public boolean hasNext() {
			return (pos < input.length());
		}
		
		@Override
		public void remove() {
			throw new ExpressionException("remove() not supported");
		}
		
		private List<String> tokenizeSimplifiedExpression(JSQLTokenMap<Integer,Object> variablesMap,int count){
			
			Pattern pattern = Pattern.compile(KEYWORD_REGEX, Pattern.CASE_INSENSITIVE);
		    Matcher matcher = pattern.matcher(input);
		    
		    List<String> tokenList = new ArrayList<String>();
		    
		    int beginClause=0;
    		String clause = "";
		    while (matcher.find()) {
		    	
		    	String key = input.substring(matcher.start(),matcher.end()).trim();
		    	
		    	// Is function?
		    	
		    	if(functions.containsKey(key.toUpperCase())){
		    		if(!input.substring(matcher.end(),input.length()).trim().startsWith("(")){
		    			continue;
		    		}
		    	}
		    	
		    	// Is all operator
		    	
		    	if(key.equals("*")){
		    		if(input.substring(matcher.end(),input.length()).trim().startsWith("=")){
		    			continue;
		    		}
		    	}
		    		
		    	// Add the previous token
	
		    	if(beginClause!=0){
		    		clause = input.substring(beginClause,matcher.start());
		    	}else{
		    		clause = input.substring(0,matcher.start());
		    	}
		    	if(!clause.trim().equals("")){
		    		if(!clause.trim().startsWith("ARG")){
		    			variables.put(dummyVar(count),null);
		    			tokenList.add(dummyVar(count++));
		    			putByType(variablesMap,clause.trim());
		    		}else{
		    			variables.put(clause.trim(),null);
		    			tokenList.add(clause.trim());
		    		}
		    		
		    	}
		    	// Add the operator
		    
	    		tokenList.add(key);
		    	
		    	beginClause =  matcher.end();
		    	
		    }
		    if(beginClause<input.length()){
		    	clause = input.substring(beginClause,input.length());
		    	if(!clause.trim().startsWith("ARG")){
		    		variables.put(dummyVar(count),null);
	    			tokenList.add(dummyVar(count++));
	    			putByType(variablesMap,clause.trim());
	    		}else{
	    			variables.put(clause.trim(),null);
	    			tokenList.add(clause.trim());
	    		}
			}
		    listIt(tokenList,"\nCompleted Simplified Expression:Tokenlist: ","token");
			return tokenList;
		}
		
	}
	
	private class Token<T>{
		String type;
		T value;
		public Token(T value){
			this.value = value;
			if(value instanceof String){
				type="String";
			}else{
				type="Number";
			}
		}
		public BigDecimal dec(){
			if(type.equals("Number"))
				return (BigDecimal)value;
			else if(isNumber(value.toString()))
				return new BigDecimal(value.toString());
			else return BigDecimal.ZERO;
		}
		public String str(){
			if(type.equals("String"))
			return (String)value;
			else throw new RuntimeException("Illegal operation with type string.");
		}
		public T getValue(){
			return value;
		}
	}
	
	/**
	 * Creates a new expression instance from an expression string.
	 *
	 * @param expression
	 * The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
	 * <code>"sin(y)>0 & max(z, 3)>3"</code>
	 */
	public JSQLExpression() {
		//this.expression = expression;
		addOperator(new Operator("+", 20, true) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().add(v2.dec(), mc);
			}
		});
		addOperator(new Operator("-", 20, true) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().subtract(v2.dec(), mc);
			}
		});
		addOperator(new Operator("*", 30, true) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().multiply(v2.dec(), mc);
			}
		});
		addOperator(new Operator("/", 30, true) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().divide(v2.dec(), mc);
			}
		});
		addOperator(new Operator("%", 30, true) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().remainder(v2.dec(), mc);
			}
		});
		addOperator(new Operator("^", 40, false) {
			@Override
			public BigDecimal eval(Token<?> arg1, Token<?> arg2) {
				/*-
				 * Thanks to Gene Marin:
				 * http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java
				 */
				BigDecimal v1 = arg1.dec(), v2 = arg2.dec();
				int signOf2 = v2.signum();
				double dn1 = v1.doubleValue();
				v2 = v2.multiply(new BigDecimal(signOf2)); // n2 is now positive
				BigDecimal remainderOf2 = v2.remainder(BigDecimal.ONE);
				BigDecimal n2IntPart = v2.subtract(remainderOf2);
				BigDecimal intPow = v1.pow(n2IntPart.intValueExact(), mc);
				BigDecimal doublePow = new BigDecimal(Math.pow(dn1,
						remainderOf2.doubleValue()));
				BigDecimal result = intPow.multiply(doublePow, mc);
				if (signOf2 == -1) {
					result = BigDecimal.ONE.divide(result, mc.getPrecision(),
							RoundingMode.HALF_UP);
				}
				return result;
			}
		});
		addOperator(new Operator("&&", 4, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				boolean b1 = !v1.dec().equals(BigDecimal.ZERO);
				boolean b2 = !v2.dec().equals(BigDecimal.ZERO);
				return b1 && b2 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("and", 4, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				boolean b1 = !v1.dec().equals(BigDecimal.ZERO);
				boolean b2 = !v2.dec().equals(BigDecimal.ZERO);
				return b1 && b2 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("||", 2, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				boolean b1 = !v1.dec().equals(BigDecimal.ZERO);
				boolean b2 = !v2.dec().equals(BigDecimal.ZERO);
				return b1 || b2 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("or", 2, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				boolean b1 = !v1.dec().equals(BigDecimal.ZERO);
				boolean b2 = !v2.dec().equals(BigDecimal.ZERO);
				return b1 || b2 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator(">", 10, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().compareTo(v2.dec()) == 1 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator(">=", 10, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().compareTo(v2.dec()) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("<", 10, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().compareTo(v2.dec()) == -1 ? BigDecimal.ONE
						: BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("<=", 10, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return v1.dec().compareTo(v2.dec()) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("=", 7, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				if(v1.type.equals("String")&&v2.type.equals("String")){
					return v1.str().equals(v2.str())? BigDecimal.ONE : BigDecimal.ZERO;
				}
				return v1.dec().compareTo(v2.dec()) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("==", 7, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return operators.get("=").eval(v1, v2);
			}
		});
		addOperator(new Operator("!=", 7, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				if(v1.type.equals("String")&&v2.type.equals("String")){
					return !v1.str().equals(v2.str())? BigDecimal.ONE : BigDecimal.ZERO;
				}
				return (v1.dec()).compareTo(v2.dec()) != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addOperator(new Operator("<>", 7, false) {
			@Override
			public BigDecimal eval(Token<?> v1, Token<?> v2) {
				return operators.get("!=").eval(v1, v2);
			}
		});
		addFunction(new Function("NOT", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				boolean zero = parameters.get(0).dec().compareTo(BigDecimal.ZERO) == 0;
				return zero ? BigDecimal.ONE : BigDecimal.ZERO;
			}
		});
		addFunction(new Function("IF", 3) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				boolean isTrue = !parameters.get(0).equals(BigDecimal.ZERO);
				return isTrue ? parameters.get(1).dec() : parameters.get(2).dec();
			}
		});
		addFunction(new Function("RANDOM", 0) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.random();
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("SIN", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.sin(Math.toRadians(parameters.get(0).dec()
						.doubleValue()));
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("COS", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.cos(Math.toRadians(parameters.get(0).dec()
						.doubleValue()));
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("TAN", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.tan(Math.toRadians(parameters.get(0).dec()
						.doubleValue()));
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("SINH", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.sinh(parameters.get(0).dec().doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("COSH", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.cosh(parameters.get(0).dec().doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("TANH", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.tanh(parameters.get(0).dec().doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("RAD", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.toRadians(parameters.get(0).dec().doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("DEG", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.toDegrees(parameters.get(0).dec().doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("MAX", 2) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				BigDecimal v1 = parameters.get(0).dec();
				BigDecimal v2 = parameters.get(1).dec();
				return v1.compareTo(v2) > 0 ? v1 : v2;
			}
		});
		addFunction(new Function("MIN", 2) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				BigDecimal v1 = parameters.get(0).dec();
				BigDecimal v2 = parameters.get(1).dec();
				return v1.compareTo(v2) < 0 ? v1 : v2;
			}
		});
		addFunction(new Function("ABS", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				return parameters.get(0).dec().abs(mc);
			}
		});
		addFunction(new Function("LOG", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				double d = Math.log10(parameters.get(0).dec().doubleValue());
				return new BigDecimal(d, mc);
			}
		});
		addFunction(new Function("ROUND", 2) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				BigDecimal toRound = parameters.get(0).dec();
				int precision = parameters.get(1).dec().intValue();
				return toRound.setScale(precision, mc.getRoundingMode());
			}
		});
		addFunction(new Function("FLOOR", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				BigDecimal toRound = parameters.get(0).dec();
				return toRound.setScale(0, RoundingMode.FLOOR);
			}
		});
		addFunction(new Function("CEILING", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				BigDecimal toRound = parameters.get(0).dec();
				return toRound.setScale(0, RoundingMode.CEILING);
			}
		});
		addFunction(new Function("SQRT", 1) {
			@Override
			public BigDecimal eval(List<Token<?>> parameters) {
				/*
				 * From The Java Programmers Guide To numerical Computing
				 * (Ronald Mak, 2003)
				 */
				BigDecimal x = parameters.get(0).dec();
				if (x.compareTo(BigDecimal.ZERO) == 0) {
					return new BigDecimal(0);
				}
				if (x.signum() < 0) {
					throw new ExpressionException(
							"Argument to SQRT() function must not be negative");
				}
				BigInteger n = x.movePointRight(mc.getPrecision() << 1)
						.toBigInteger();
				int bits = (n.bitLength() + 1) >> 1;
				BigInteger ix = n.shiftRight(bits);
				BigInteger ixPrev;
				do {
					ixPrev = ix;
					ix = ix.add(n.divide(ix)).shiftRight(1);
					// Give other threads a chance to work;
					Thread.yield();
				} while (ix.compareTo(ixPrev) != 0);
				return new BigDecimal(ix, mc.getPrecision());
			}
		});
		numeric_variables.put("PI", PI);
		numeric_variables.put("TRUE", BigDecimal.ONE);
		numeric_variables.put("FALSE", BigDecimal.ZERO);
	}
	/**
	 * Is the string a number?
	 *
	 * @param st
	 * The string.
	 * @return <code>true</code>, if the input string is a number.
	 */
	private boolean isNumber(String st) {
		if (st.charAt(0) == minusSign && st.length() == 1)
			return false;
		for (char ch : st.toCharArray()) {
			if (!Character.isDigit(ch) && ch != minusSign
					&& ch != decimalSeparator)
				return false;
		}
		return true;
	}
	
	/**
	 * Implementation of the <i>Shunting Yard</i> algorithm to transform an
	 * infix expression to a RPN expression.
	 *
	 * @param expression
	 * The input expression in infx.
	 * @return A RPN representation of the expression, with each token as a list
	 * member.
	 */
	private List<String> shuntingYard() {
		List<String> outputQueue = new ArrayList<String>();
		Stack<String> stack = new Stack<String>();
		
		out("shunting yard input:");
		out("--------------------");
		
		for (String token:tokens) {
			out2(" | " + token);
		}
		out("");
		out("--------------------");
		String lastFunction = null;
		String previousToken = null;
		for (String token:tokens) {
			
		//while (tokenizer.hasNext()) {
		//String token = tokenizer.next();
			if (isNumber(token)) {
				outputQueue.add(token);
			} else if (variables.containsKey(token)) {
				outputQueue.add(token);
			} else if (numeric_variables.containsKey(token)) {
				outputQueue.add(token);
			} else if (string_variables.containsKey(token)) {
				outputQueue.add(token);
			} else if (functions.containsKey(token.toUpperCase())) {
				stack.push(token);
				lastFunction = token;
			} else if (",".equals(token)) {
				while (!stack.isEmpty() && !"(".equals(stack.peek())) {
					outputQueue.add(stack.pop());
				}
				if (stack.isEmpty()) {
					throw new ExpressionException("Parse error for function '"
							+ lastFunction + "'");
				}
			} else if (operators.containsKey(token)) {
				Operator o1 = operators.get(token);
				String token2 = stack.isEmpty() ? null : stack.peek();
				while (operators.containsKey(token2)
						&& ((o1.isLeftAssoc() && o1.getPrecedence() <= operators
						.get(token2).getPrecedence()) || (o1
								.getPrecedence() < operators.get(token2)
								.getPrecedence()))) {
					outputQueue.add(stack.pop());
					token2 = stack.isEmpty() ? null : stack.peek();
				}
				stack.push(token);
			} else if (Character.isLetter(token.charAt(0))) {
				stack.push(token);
			} else if ("(".equals(token)) {
				if (previousToken != null) {
					if (isNumber(previousToken)) {
						throw new ExpressionException("Missing operator near " + token);
					}
				}
				stack.push(token);
			} else if (")".equals(token)) {
				while (!stack.isEmpty() && !"(".equals(stack.peek())) {
					outputQueue.add(stack.pop());
				}
				if (stack.isEmpty()) {
					throw new RuntimeException("Mismatched parentheses");
				}
				stack.pop();
				if (!stack.isEmpty()
						&& functions.containsKey(stack.peek().toUpperCase())) {
					outputQueue.add(stack.pop());
				}
			}
			previousToken = token;
		}
		while (!stack.isEmpty()) {
			String element = stack.pop();
			if ("(".equals(element) || ")".equals(element)) {
				throw new RuntimeException("Mismatched parentheses");
			}
			if (!operators.containsKey(element)) {
				throw new RuntimeException("Unknown operator or function: "
						+ element);
			}
			outputQueue.add(element);
		}
		return outputQueue;
	}
	
	/**
	 * Parses the expression, returning the variable list.
	 *
	 * @return The result of the expression.
	 */
	public void tokenize(String expression,JSQLTokenMap<Integer,Object> variablesMap,int count) {
		clear();
		Tokenizer tokenizer = new Tokenizer(expression);
		tokens = tokenizer.tokenizeSimplifiedExpression(variablesMap,count);
		getRPN();
	}
	
	public List<String> getContext(){
		return tokens;
	}
	
	public void setContext(List<String> tk){
		clear();
		tokens=tk;
		for(String token:tokens){
			if(token.startsWith("ARG")){
				variables.put(token, null);
			}
		}
		getRPN();
	}
	
	public BigDecimal eval(Map<Integer,Object> valuesMap){
		 for (Entry<Integer, Object> entry : valuesMap.entrySet()) {
			setByType(entry.getValue(),entry.getKey());
		}
		return eval();
	}
	
	/**
	 * Evaluates the expression.
	 *
	 * @return The result of the expression.
	 */
	public BigDecimal eval() {
		try{
			Stack<Token<?>> stack = new Stack<Token<?>>();
			out("Reverse polish notation:");
			out("--------------------");
			for (String token : getRPN()) {
				out2(" | " + token);
				out("");
				if (operators.containsKey(token)) {
					Token<?> arg2 = stack.pop();
					Token<?> arg1 = stack.pop();
					stack.push(new Token<BigDecimal>(operators.get(token).eval(arg1, arg2)));
				} else if (numeric_variables.containsKey(token)) {
					stack.push(new Token<BigDecimal>(numeric_variables.get(token).round(mc)));
				} else if (string_variables.containsKey(token)) {
					stack.push(new Token<String>(string_variables.get(token)));
				} else if (functions.containsKey(token.toUpperCase())) {
					Function f = functions.get(token.toUpperCase());
					ArrayList<Token<?>> p = new ArrayList<Token<?>>(
							f.getNumParams());
					for (int i = 0; i < f.numParams; i++) {
						p.add(0,stack.pop());
					}
					Token<BigDecimal> fResult = new Token<BigDecimal>(f.eval(p));
					stack.push(fResult);
				} else {
					stack.push(new Token<BigDecimal>(new BigDecimal(token, mc)));
				}
			}
			out("--------------------");
			return ((BigDecimal)stack.pop().getValue()).stripTrailingZeros();
		}catch(Exception e){
			System.out.println(e);
			return BigDecimal.ZERO;
		}
	}
	/**
	 * Sets the precision for expression evaluation.
	 *
	 * @param precision
	 * The new precision.
	 *
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression setPrecision(int precision) {
		this.mc = new MathContext(precision);
		return this;
	}
	/**
	 * Sets the rounding mode for expression evaluation.
	 *
	 * @param roundingMode
	 * The new rounding mode.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression setRoundingMode(RoundingMode roundingMode) {
		this.mc = new MathContext(mc.getPrecision(), roundingMode);
		return this;
	}
	/**
	 * Adds an operator to the list of supported operators.
	 *
	 * @param operator
	 * The operator to add.
	 * @return The previous operator with that name, or <code>null</code> if
	 * there was none.
	 */
	public Operator addOperator(Operator operator) {
		return operators.put(operator.getOper(), operator);
	}
	/**
	 * Adds a function to the list of supported functions
	 *
	 * @param function
	 * The function to add.
	 * @return The previous operator with that name, or <code>null</code> if
	 * there was none.
	 */
	public Function addFunction(Function function) {
		return functions.put(function.getName(), function);
	}
	
	public void setByType(Object val,int i){
		if(val instanceof Number){
			out(val+" is a number");
			set(dummyVar(i),(Number)val);
		}else if(val instanceof String){
			out(val+" is a string");
			set(dummyVar(i),(String)val);
		}else if(val instanceof Boolean){
			out(val+" is a boolean");
			set(dummyVar(i),(Boolean)val);
		}
	}
	
	private void putByType(JSQLTokenMap<Integer,Object> variablesMap, String variable){
		if(isNumber(variable)){
			variablesMap.tokens.add(new BigDecimal(variable));
			variablesMap.type.add(NUMERIC); // Numeric
		}else if(variable.equalsIgnoreCase("true")||variable.equalsIgnoreCase("false")){
			variablesMap.tokens.add(new Boolean(variable));
			variablesMap.type.add(NUMERIC); // Numeric
		}else{
			variablesMap.tokens.add(variable);
			variablesMap.type.add(PATH); // Path
		}
	}
	
	public String dummyVar(int count){
		return "ARG"+count;
	}
	
	/**
	 * Sets a variable value.
	 *
	 * @param variable
	 * The variable name.
	 * @param value
	 * The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression setVariable(String variable, BigDecimal value) {
		string_variables.remove(variable);
		numeric_variables.put(variable, value);
		return this;
	}
	/**
	 * Sets a variable value.
	 *
	 * @param variable
	 * The variable to set.
	 * @param value
	 * The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression setVariable(String variable, String value) {
		numeric_variables.remove(variable);
		string_variables.put(variable, value);
		//expression = expression.replaceAll("\\b" + variable + "\\b", "(" + value + ")");
		return this;
	}

	/**
	 * Sets a variable value.
	 *
	 * @param variable
	 * The variable to set.
	 * @param value
	 * The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression set(String variable, int value) {
		return setVariable(variable,new BigDecimal(value));
	}
	
	/**
	 * Sets a variable value.
	 *
	 * @param variable
	 * The variable to set.
	 * @param value
	 * The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression set(String variable, Number value) {
		return setVariable(variable,new BigDecimal(value.doubleValue()));
	}

	/**
	 * Sets a variable value.
	 *
	 * @param variable
	 * The variable to set.
	 * @param value
	 * The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression set(String variable, boolean value) {
		return setVariable(variable,new BigDecimal((value?1:0)));
	}

	/**
	 * Sets a variable value.
	 *
	 * @param variable
	 * The variable to set.
	 * @param value
	 * The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression set(String variable, BigDecimal value) {
		return setVariable(variable, value);
	}

	/**
	 * Sets a variable value.
	 *
	 * @param variable
	 * The variable to set.
	 * @param value
	 * The variable value.
	 * @return The expression, allows to chain methods.
	 */
	public JSQLExpression set(String variable, String value) {
		return setVariable(variable, value);
	}

	/**
	 * Get an iterator for this expression, allows iterating over an expression
	 * token by token.
	 *
	 * @return A new iterator instance for this expression.
	 */
	//public Iterator<String> getJSQLEvaluateTokenizer() {
	//	return new Tokenizer(String expression);
	//}
	
	private void clear(){
		variables.clear();
		numeric_variables.clear();
		string_variables.clear();
		rpn=null;
	}
	
	/**
	 * Cached access to the RPN notation of this expression, ensures only one
	 * calculation of the RPN per expression instance. If no cached instance
	 * exists, a new one will be created and put to the cache.
	 *
	 * @return The cached RPN instance.
	 */
	public List<String> getRPN() {
		if (rpn == null) {
			rpn = shuntingYard();
		}
		return rpn;
	}
	/**
	 * Get a string representation of the RPN (Reverse Polish Notation) for this
	 * expression.
	 *
	 * @return A string with the RPN representation for this expression.
	 */
	public String toRPN() {
		String result = new String();
		for (String st : getRPN()) {
			result = result.isEmpty() ? result : result + " ";
			result += st;
		}
		return result;
	}
	public void out(Object msg){
		//System.out.println(msg);
	}
	public void out2(Object msg){
		//System.out.print(msg);
	}
	private void listIt(List list,String start,String loopStr){
		out(start);
		for (Object elem : list) {
			out(loopStr+ " "+ elem.toString());
		}
		out("");
	}
}
