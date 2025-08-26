#!/usr/bin/env python3
"""
Fix nil rendering issue in views.cljs by properly handling the question-panel conditional rendering
"""

import re
import sys
from typing import List

def read_file(filepath: str) -> List[str]:
    """Read file and return lines as list"""
    with open(filepath, 'r', encoding='utf-8') as f:
        return f.readlines()

def write_file(filepath: str, lines: List[str]) -> None:
    """Write lines back to file"""
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(lines)

def fix_question_panel_nil_handling(lines: List[str]) -> List[str]:
    """Fix the question-panel function to properly handle nil current-question"""
    
    # Find the question-panel function
    question_panel_start = None
    for i, line in enumerate(lines):
        if "(defn question-panel" in line:
            question_panel_start = i
            break
    
    if question_panel_start is None:
        print("Could not find question-panel function")
        return lines
    
    print(f"Found question-panel function at line {question_panel_start + 1}")
    
    # The current broken structure needs to be fixed
    # We need to properly structure the conditional rendering
    
    # Find the current problematic if statement and fix the bracket structure
    fixed_lines = []
    inside_question_panel = False
    found_if_statement = False
    bracket_depth = 0
    
    for i, line in enumerate(lines):
        if i == question_panel_start:
            inside_question_panel = True
            bracket_depth = 0
            
        if inside_question_panel:
            # Track bracket depth to know when we've exited the function
            bracket_depth += line.count('(') - line.count(')')
            
            # Replace the problematic conditional rendering with a proper structure
            if "if current-question" in line and not found_if_statement:
                found_if_statement = True
                # Start the proper conditional structure
                fixed_lines.append("        (if current-question\n")
                continue
            elif found_if_statement and "Please wait while we load the curriculum." in line:
                # This is the loading message line - fix the closing
                fixed_lines.append(line.replace("]]))))))", "]])\n"))
                # Add the proper closing for the if statement and function
                fixed_lines.append("          ;; Loading state when no question is available\n")
                fixed_lines.append("          [:div.loading-panel\n")
                fixed_lines.append("           {:style {:background-color \"#f8f9fa\"\n")
                fixed_lines.append("                    :border-radius \"12px\"\n")
                fixed_lines.append("                    :padding \"30px\"\n")
                fixed_lines.append("                    :margin \"20px 0\"\n")
                fixed_lines.append("                    :text-align \"center\"\n")
                fixed_lines.append("                    :box-shadow \"0 4px 12px rgba(0,0,0,0.1)\"}}\n")
                fixed_lines.append("           [:h3 {:style {:color \"#2c3e50\"}}\n")
                fixed_lines.append("            \"Loading questions...\"]\n")
                fixed_lines.append("           [:p {:style {:color \"#666\"}}\n")
                fixed_lines.append("            \"Please wait while we load the curriculum.\"]]))))\n")
                continue
            
            # Check if we've exited the function
            if bracket_depth <= 0 and i > question_panel_start + 5:
                inside_question_panel = False
                found_if_statement = False
        
        fixed_lines.append(line)
    
    print("Fixed question-panel conditional rendering structure")
    return fixed_lines

def main():
    if len(sys.argv) != 2:
        print("Usage: python fix_nil_render.py <views.cljs>")
        sys.exit(1)
    
    filepath = sys.argv[1]
    
    try:
        lines = read_file(filepath)
        print(f"Read {len(lines)} lines from {filepath}")
        
        fixed_lines = fix_question_panel_nil_handling(lines)
        write_file(filepath, fixed_lines)
        print(f"Fixed file written to {filepath}")
        
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()