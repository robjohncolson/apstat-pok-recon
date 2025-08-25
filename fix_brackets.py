#!/usr/bin/env python3
"""
Bracket/Parentheses Fixer for ClojureScript Files
Helps fix mismatched brackets and parentheses based on clj-kondo output
"""

import re
import sys
from typing import List, Tuple

def read_file(filepath: str) -> List[str]:
    """Read file and return lines as list"""
    with open(filepath, 'r', encoding='utf-8') as f:
        return f.readlines()

def write_file(filepath: str, lines: List[str]) -> None:
    """Write lines back to file"""
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(lines)

def count_brackets(text: str) -> dict:
    """Count different types of brackets in text"""
    counts = {
        '(': text.count('('),
        ')': text.count(')'),
        '[': text.count('['),
        ']': text.count(']'),
        '{': text.count('{'),
        '}': text.count('}')
    }
    return counts

def analyze_bracket_balance(lines: List[str]) -> dict:
    """Analyze bracket balance across the entire file"""
    full_text = ''.join(lines)
    counts = count_brackets(full_text)
    
    return {
        'parens': counts['('] - counts[')'],
        'squares': counts['['] - counts[']'],
        'curlies': counts['{'] - counts['}'],
        'total_counts': counts
    }

def find_mismatched_brackets(lines: List[str], start_line: int, end_line: int) -> List[Tuple[int, str, str]]:
    """Find mismatched brackets between start_line and end_line"""
    issues = []
    
    # Track bracket stacks
    paren_stack = []
    square_stack = []
    curly_stack = []
    
    for line_num in range(start_line - 1, min(end_line, len(lines))):
        line = lines[line_num]
        
        # Skip strings and comments (simple approach)
        in_string = False
        in_comment = False
        i = 0
        
        while i < len(line):
            char = line[i]
            
            # Handle comments
            if char == ';' and not in_string:
                in_comment = True
                break
            
            # Handle strings
            if char == '"' and not in_comment:
                in_string = not in_string
                i += 1
                continue
            
            if in_string or in_comment:
                i += 1
                continue
            
            # Track brackets
            if char == '(':
                paren_stack.append((line_num + 1, i))
            elif char == ')':
                if paren_stack:
                    paren_stack.pop()
                else:
                    issues.append((line_num + 1, i, f"Unmatched closing parenthesis"))
            elif char == '[':
                square_stack.append((line_num + 1, i))
            elif char == ']':
                if square_stack:
                    square_stack.pop()
                else:
                    issues.append((line_num + 1, i, f"Unmatched closing square bracket"))
            elif char == '{':
                curly_stack.append((line_num + 1, i))
            elif char == '}':
                if curly_stack:
                    curly_stack.pop()
                else:
                    issues.append((line_num + 1, i, f"Unmatched closing curly bracket"))
            
            i += 1
    
    # Add unclosed brackets
    for line_num, pos in paren_stack:
        issues.append((line_num, pos, "Unclosed parenthesis"))
    for line_num, pos in square_stack:
        issues.append((line_num, pos, "Unclosed square bracket"))
    for line_num, pos in curly_stack:
        issues.append((line_num, pos, "Unclosed curly bracket"))
    
    return issues

def fix_views_cljs_main_panel(lines: List[str]) -> List[str]:
    """Specific fix for the main-panel function bracket issue"""
    # Find the main-panel function
    main_panel_start = None
    for i, line in enumerate(lines):
        if "(defn main-panel" in line:
            main_panel_start = i
            break
    
    if main_panel_start is None:
        print("Could not find main-panel function")
        return lines
    
    # Find the problematic line around 468
    problem_line = None
    for i in range(len(lines) - 1, -1, -1):
        if "[test-controls]" in lines[i]:
            problem_line = i
            break
    
    if problem_line is None:
        print("Could not find test-controls line")
        return lines
    
    print(f"Found main-panel at line {main_panel_start + 1}")
    print(f"Found test-controls at line {problem_line + 1}")
    print(f"Current line: {repr(lines[problem_line])}")
    
    # The structure should be:
    # (defn main-panel []
    #   (let [...]
    #     [:div.main-panel
    #      ...
    #      [:div.content-area
    #       [:div.question-section ...]
    #       [:div.sidebar-section
    #        [reputation-panel]
    #        [test-controls]]]]))  ; close sidebar, content-area, main-panel, let, defn
    
    # Replace the problematic line
    old_line = lines[problem_line]
    if "[test-controls]]]])" in old_line:
        new_line = old_line.replace("[test-controls]]]])", "[test-controls]]]))")
        lines[problem_line] = new_line
        print(f"Fixed: {repr(old_line)} -> {repr(new_line)}")
    elif "[test-controls]]]]" in old_line:
        new_line = old_line.replace("[test-controls]]]]", "[test-controls]]])")
        lines[problem_line] = new_line
        print(f"Fixed: {repr(old_line)} -> {repr(new_line)}")
    elif "[test-controls]]])" in old_line:
        new_line = old_line.replace("[test-controls]]])", "[test-controls]]]))")
        lines[problem_line] = new_line  
        print(f"Fixed: {repr(old_line)} -> {repr(new_line)}")
    else:
        print(f"Unexpected format: {repr(old_line)}")
        print("Manual intervention needed")
    
    return lines

def fix_phase5_tests(lines: List[str]) -> List[str]:
    """Fix missing closing parenthesis in phase5-tests.cljs"""
    # Add missing closing parenthesis at the end
    if lines and not lines[-1].strip().endswith(')'):
        # Find the last line with content
        for i in range(len(lines) - 1, -1, -1):
            if lines[i].strip():
                lines[i] = lines[i].rstrip() + ')\n'
                print(f"Added closing parenthesis to line {i + 1}")
                break
    return lines

def main():
    if len(sys.argv) < 2:
        print("Usage: python fix_brackets.py <filepath> [--analyze|--fix-views|--fix-phase5]")
        sys.exit(1)
    
    filepath = sys.argv[1]
    mode = sys.argv[2] if len(sys.argv) > 2 else "--analyze"
    
    try:
        lines = read_file(filepath)
        print(f"Read {len(lines)} lines from {filepath}")
        
        if mode == "--analyze":
            balance = analyze_bracket_balance(lines)
            print("\nBracket Analysis:")
            print(f"Parentheses balance: {balance['parens']} (need {-balance['parens']} more closing)" if balance['parens'] > 0 else f"Parentheses balance: {balance['parens']} (too many closing)")
            print(f"Square brackets balance: {balance['squares']}")
            print(f"Curly brackets balance: {balance['curlies']}")
            print(f"\nTotal counts: {balance['total_counts']}")
            
            # Look for specific issues around reported lines
            issues = find_mismatched_brackets(lines, 430, 470)
            if issues:
                print(f"\nFound {len(issues)} bracket issues:")
                for line_num, pos, desc in issues:
                    print(f"  Line {line_num}, position {pos}: {desc}")
        
        elif mode == "--fix-views":
            print("Attempting to fix views.cljs main-panel function...")
            fixed_lines = fix_views_cljs_main_panel(lines)
            write_file(filepath, fixed_lines)
            print(f"Fixed file written to {filepath}")
            
            # Re-analyze
            balance = analyze_bracket_balance(fixed_lines)
            print(f"New balance - Parens: {balance['parens']}, Squares: {balance['squares']}")
        
        elif mode == "--fix-phase5":
            print("Attempting to fix phase5-tests.cljs...")
            fixed_lines = fix_phase5_tests(lines)
            write_file(filepath, fixed_lines)
            print(f"Fixed file written to {filepath}")
            
            # Re-analyze
            balance = analyze_bracket_balance(fixed_lines)
            print(f"New balance - Parens: {balance['parens']}, Squares: {balance['squares']}")
        
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()