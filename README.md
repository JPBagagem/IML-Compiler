# IML - Image Manipulation Language
---

**Authors:**
- Ana Margarida Castro
- Filipe Marques
- Gonçalo Almeida
- João Barreira
- José Pedro Bagagem
- Marta Contente

## Introduction

This project implements **IML** (*Image Manipulation Language*), a domain-specific language designed for grayscale image manipulation. It features a compiled core language (IML) and an interpreted secondary language (**IIML**) for declarative image generation.

---

## Key Features

### Core IML (Compiled)
- **Advanced Image Processing:** Supports pixel-wise arithmetic operations between images, lists, and constants.
- **Dynamic List Handling:** 
    - Bidimensional indexing support (e.g., `j[0][1]`).
    - Modern list operations like the `pop` method to remove specific sublists.
- **Saturation Safety:** Automatic handling of pixel intensity to prevent overflows, ensuring values stay within the valid [0, 1] range.
    - *Example:* [example03.iml](file:///home/bagagem/C/comp2425-iml-b03/examples/example03.iml) demonstrates progressive illumination without saturation artifacts.

![Progressive Illumination](file:///home/bagagem/C/comp2425-iml-b03/examples/newTests/iluminado.gif)

### IIML (Interpreted)
- **Declarative Graphics:** Easily place shapes and lines on a canvas.
- **Extended Geometry:** Includes additional shapes like **Lines** and **Stars** beyond standard requirements.
- **Architectural Separation:** Decouples semantic analysis from the interpreter logic for better maintainability and error reporting.

---

## Getting Started

### Prerequisites
- Python 3.x
- ANTLR4
- Java Runtime Environment (for ANTLR)

### Building the Project
Navigate to the `src` directory and run the build script:
```bash
cd src
./build.sh
```
This script runs `antlr4-build` for the main IML project and its Python-based IIML extension.

### Running Programs
To compile and run an IML script:
```bash
./compile.sh ../examples/your_script.iml
./run.sh ../examples/generated_output.py
```

To run an IIML script (Interpreted):
```bash
python3 run_iiml.py
```

### Cleaning Up
To remove generated ANTLR4 and compiled files:
```bash
./clean.sh
# Use -d to target a specific directory
./clean.sh -d IIML
```

---

## Project Structure

- `src/`: Source code, including Lexer and Parser definitions (`.g4` files).
    - `IIML/`: Secondary interpreted language implementation.
- `examples/`: Collection of test scripts demonstrating language features.
- `doc/`: Project documentation and reports.
- `LICENSE`: [MIT License](file:///home/bagagem/C/comp2425-iml-b03/LICENSE).

---

## Technical Details

### Semantic Analysis
Our analyzer supports complex pixel-wise operations across multiple types:
- Images (PGM format).
- Iterables (lists of lists of numbers or percentages).
- Mixed operations between iterables and constants.

*Note: Operations on lists containing booleans or strings are intentionally restricted.*

### Example Output
One of the most powerful features is the blending and processing of visual data:
![Blend Result](file:///home/bagagem/C/comp2425-iml-b03/examples/images/blend.gif)
