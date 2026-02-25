#!/usr/bin/env python3

import sys
import numpy as np
import cv2
from antlr4 import InputStream, CommonTokenStream
from IIMLLexer import IIMLLexer
from IIMLParser import IIMLParser
from IIMLInterpreter import IIMLInterpreter

def main():
    args = sys.argv[1:]
    iiml_filename = args[0] if args else "example.iiml"

    with open(iiml_filename, "r") as f:
        code = f.read()

    # Generate AST
    input_stream = InputStream(code)
    lexer = IIMLLexer(input_stream)
    tokens = CommonTokenStream(lexer)
    parser = IIMLParser(tokens)
    tree = parser.program()

    # Interpret the AST
    interpreter = IIMLInterpreter()
    _ = interpreter.visit(tree)

    if interpreter.image is not None:
        img_float = interpreter.image
        img_uint8 = np.clip(img_float, 0, 255).astype(np.uint8)

        # Write directly to “output.pgm” using OpenCV
        success = cv2.imwrite("output.pgm", img_uint8)
        if not success:
            print("Erro ao salvar output.pgm")
        else:
            print("Imagem guardada como output.pgm")
    else:
        print("Nenhuma imagem foi gerada.")

if __name__ == "__main__":
    main()
