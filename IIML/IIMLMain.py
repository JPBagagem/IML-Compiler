import sys
from antlr4 import *
from IIMLLexer import IIMLLexer
from IIMLParser import IIMLParser
from IIMLSemanticAnalyser import IIMLSemanticAnalyser
from IIMLInterpreter import IIMLInterpreter

def main(code):
   visitor0 = IIMLSemanticAnalyser()
   input_stream = InputStream(code)
   lexer = IIMLLexer(input_stream)
   stream = CommonTokenStream(lexer)
   parser = IIMLParser(stream)
   tree = parser.program()

   if parser.getNumberOfSyntaxErrors() == 0:
      
      err = visitor0.visit(tree)
      if err:
         return err
      
      visitor1 = IIMLInterpreter()
      return  visitor1.visit(tree)
