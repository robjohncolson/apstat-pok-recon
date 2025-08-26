#!/usr/bin/env python3
"""
Clean up the question-panel function by removing duplicates and fixing brackets
"""

def clean_question_panel():
    with open('src/pok/views.cljs', 'r') as f:
        lines = f.readlines()
    
    new_lines = []
    inside_question_panel = False
    skip_until_next_function = False
    
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Start of question-panel function
        if '(defn question-panel' in line:
            inside_question_panel = True
            skip_until_next_function = False
            new_lines.append(line)
        
        # If we're inside question-panel and see duplicate loading state, skip it
        elif inside_question_panel and 'Loading state when no question is available' in line and skip_until_next_function:
            # Skip this duplicate section
            while i < len(lines) and not lines[i].strip().startswith('(defn '):
                i += 1
            i -= 1  # Back up one so we don't skip the next function
            inside_question_panel = False
            continue
        
        # First occurrence of loading state - mark to skip duplicates
        elif inside_question_panel and 'Loading state when no question is available' in line:
            new_lines.append(line)
            skip_until_next_function = True
            
        # End of question-panel function
        elif inside_question_panel and line.strip().startswith('(defn ') and 'question-panel' not in line:
            inside_question_panel = False
            skip_until_next_function = False
            new_lines.append(line)
            
        # Regular line
        else:
            if not skip_until_next_function:
                new_lines.append(line)
        
        i += 1
    
    # Write cleaned content
    with open('src/pok/views.cljs', 'w') as f:
        f.writelines(new_lines)
    
    print("Cleaned up question-panel function")

if __name__ == "__main__":
    clean_question_panel()