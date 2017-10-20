import java.io.StringReader;
import java.lang.StringBuilder;
import java.lang.Character;
import java.lang.String;
import java.util.HashMap;
import java.io.IOException;


public class Parser{
    
    private static StringReader input;
    
    static boolean parse(String s){
	input = new StringReader(s);
	lex();
	boolean value = program();
	expect(TokenType.EOL);
	return value;
    }

    private static enum TokenType{
	AND, OR, IMPLY, NOT, LPAREN, RPAREN,
	TRUE_LITERAL, FALSE_LITERAL, LET, QUERY,
	END_QUERY, END_LET,
	EQUALS, VARIABLE, VARIABLE_EQL,  EOL;
    }

    private static String LEXEME;
    private static TokenType TOKEN;

    private static void lex(){
	LEXEME = null;
	TOKEN = null;
	try{
	    int b;
	    do{ //Gets rid of the leading whitespaces
		b = input.read();
	    }while(Character.isWhitespace((char)b));
	    
	    if(b == -1){ //if the end of line is reached
		TOKEN = TokenType.EOL;
		return;
	    }
	    
	    if(Character.isLetter((char) b)){ //Process characters
		StringBuilder builder = new StringBuilder();
		do{
		    builder.append((char)b);
		    input.mark(1);
		    b = input.read();
		}while(b != -1 && Character.isLetter((char)b));
		input.reset();

		LEXEME = builder.toString(); //Turn the builder into a String
		LEXEME = LEXEME.toUpperCase(); //Goodbye case sensitive
		
		if(LEXEME.equals("LET")){
		    TOKEN = TokenType.LET;
		    return;
		}else if(LEXEME.equals("QUERY")){
		    TOKEN = TokenType.QUERY;
		    return;
		}else if(LEXEME.equals("TRUE")){
		    TOKEN = TokenType.TRUE_LITERAL;
		    return;
		}else if(LEXEME.equals("FALSE")){
		    TOKEN = TokenType.FALSE_LITERAL;
		    return;
		}else{ //Variable names
		    TOKEN = TokenType.VARIABLE;
		    return;
		}
	    }

	    char c = (char)b; //A non-letter character was found

	    //Single character functions
	    if(c == '|'){ //OR function
		TOKEN = TokenType.OR;
		return;
	    }
	    if(c == '&'){ //AND function
		TOKEN = TokenType.AND;
		return;
	    }
	    if(c == '~'){ // Negation
		TOKEN = TokenType.NOT;
		return;
	    }
	    if(c == '('){ //Left Paren
		TOKEN = TokenType.LPAREN;
		return;
	    }
	    if(c == ')'){ //Right Paren
		TOKEN = TokenType.RPAREN;
		return;
	    }
	    if(c == '='){
		TOKEN = TokenType.VARIABLE_EQL;
		return;
	    }
	    if(c == ';'){
		TOKEN = TokenType.END_LET;
		return;
	    }
	    if(c == '.'){
		TOKEN = TokenType.END_QUERY;
		return;
	    }

	    //Multiple Character Functions
	    if(c == '<'){ //Language's Equal Sign
	        b = (int)c;
		b = input.read();
		if((char)b == '='){ //Checks for the second Character
		   b = input.read();
		   if((char)b == '>'){
		       TOKEN = TokenType.EQUALS;
		       return;
		   }else{
		       throw new RuntimeException("Unexpected Character: " + (char)b);
		   }
		}else {
		    throw new RuntimeException("Unexpected Character: " + (char)b);
		}
	    }

	    if(c == '-'){ //Implies
		b = (int)c;
		b = input.read();
		if((char)b == '>'){
		    TOKEN = TokenType.IMPLY;
		    return;
		}else{
		    throw new RuntimeException("Unexpected Character: " + (char)b);
		}
	    }else{
		throw new RuntimeException("Unexpected Character: " + (char)b);
	    }
	}catch (IOException e){
	    throw new RuntimeException("Invalid Character Read");
	}
    }

    private static boolean accept(TokenType  type){
	if(TOKEN == type){
	    lex();
	    return true;
	}
	return false;
    }

    private static void expect(TokenType type){
	if(TOKEN == type){
	    lex();
	}else{
	    throw new RuntimeException("Wrong token was returned.");
	}
    }

    private static boolean peek(TokenType type){
	if(TOKEN == type){
	    return true;
	}
	return false;
    }

    //Global Variables
    private static HashMap<String, Boolean> lookup;
	
    //<Program>
    private static boolean program(){
	lookup = new HashMap<String, Boolean>();
	assignment();
	return query();
    }

    //<assignment>
    private static void assignment(){
	while(accept(TokenType.LET)){
	   String key = variable();
	   expect(TokenType.VARIABLE_EQL);
	   boolean value = proposition();
	   if(lookup.containsKey(key)){
		throw new RuntimeException("Variable has already been used");
    	   }else{
		lookup.put(key,value);
	   }
	   expect(TokenType.END_LET);
	}
    }

    //<query>
    private static boolean query(){
	boolean value = false;
	if(accept(TokenType.QUERY)){//If the Query Token is used
	     value = proposition(); //Return the value of the query
	     expect(TokenType.END_QUERY);
	}
	return value;
    }

    //<proposition>
    private static boolean proposition(){
	boolean value = implication();
	while(accept(TokenType.EQUALS)){
	    value = (value == implication());
	}
	return value;
    }

    //<implication>
    private static boolean implication(){
	boolean value = disjunction(); //moving down the heirarchy
	while(peek(TokenType.IMPLY)){
	    accept(TokenType.IMPLY);
	    value = !value | implication(); //Reduction of -> is !a|b
	}
	return value;
    }

    //<disjunction>
    private static boolean disjunction(){
	boolean value = conjunction();
	while(accept(TokenType.OR)){
	    value = conjunction() | value;
	}
	return value;
    }


    //<conjunction>
    private static boolean conjunction(){
	boolean value = negation();
	while(accept(TokenType.AND)){
	    value = negation() & value;
	}
	return value;
    }

    //<negation>
    private static boolean negation(){
	while(accept(TokenType.NOT)){
	    boolean value = expression();
	    value = !value;
	    return value;
	}
	return expression();
    }

    //<expression>
    private static boolean expression(){
	if(peek(TokenType.VARIABLE) || peek(TokenType.TRUE_LITERAL) || peek(TokenType.FALSE_LITERAL)){
		boolean value = bool();
		return value;
	}else{
		expect(TokenType.LPAREN);
		boolean value = proposition();
		expect(TokenType.RPAREN);
		return value;
	}
    }

    //<boolean>
    private static boolean bool(){
	String value = "";
	if(peek(TokenType.VARIABLE)){
	    value = variable();
	    if(value != null){
		 return lookup.get(value);
	    }else{
		throw new RuntimeException("Undeclared Variable Name");
	    }
	}else{
	    return literal();
	}
    }



    //<variable>
    private static String variable(){
	if(peek(TokenType.VARIABLE)){
		String value = LEXEME;
		expect(TokenType.VARIABLE);
		return value;
	} 
	return null;
    }

    //<literal>
    private static boolean literal(){
	if(accept(TokenType.TRUE_LITERAL)){
	    return true;
	}else{
	    expect(TokenType.FALSE_LITERAL);
	    return false;
	}
    }
}
