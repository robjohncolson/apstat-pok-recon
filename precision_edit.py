#!/usr/bin/env python3
"""
Precision edit tool for fixing ClojureScript bracket mismatches
"""

import re
import sys

def fix_question_panel_brackets(filename):
    """Fix the bracket mismatch in question-panel after adding nil check"""
    
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # We need exactly 2 more closing parentheses according to the analyzer
    # Find the end of question-panel function and add them
    
    for i, line in enumerate(lines):
        if 'Loading question...' in line and '])))' in line:
            # This line needs 2 more closing parens: ]))) -> ]))))
            lines[i] = line.replace(']))))', '])))))')
            print(f"Fixed line {i+1}: Added missing closing parenthesis")
            break
    
    # Write back
    with open(filename, 'w', encoding='utf-8') as f:
        f.writelines(lines)
    
    print("Applied precision bracket fix")

def analyze_brackets_around_line(filename, target_line):
    """Analyze bracket structure around a specific line"""
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    start = max(0, target_line - 5)
    end = min(len(lines), target_line + 5)
    
    print(f"Bracket analysis around line {target_line}:")
    for i in range(start, end):
        line = lines[i].rstrip()
        parens = line.count('(') - line.count(')')
        squares = line.count('[') - line.count(']')
        curlies = line.count('{') - line.count('}')
        
        marker = ">>>" if i == target_line - 1 else "   "
        print(f"{marker} {i+1:3}: {parens:+2}() {squares:+2}[] {curlies:+2}{{}}: {line}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python precision_edit.py <action> [args]")
        print("Actions:")
        print("  fix-question-panel <file>")
        print("  analyze <file> <line_number>")
        sys.exit(1)
    
    action = sys.argv[1]
    
    if action == "fix-question-panel":
        filename = sys.argv[2]
        fix_question_panel_brackets(filename)
    elif action == "analyze":
        filename = sys.argv[2]
        line_num = int(sys.argv[3])
        analyze_brackets_around_line(filename, line_num)
    else:
        print(f"Unknown action: {action}")

if __name__ == "__main__":
    main()