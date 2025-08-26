#!/usr/bin/env python3
"""
Fix the question-panel bracket structure properly
"""

def fix_brackets():
    with open('src/pok/views.cljs', 'r') as f:
        content = f.read()
    
    # The structure should be:
    # (if current-question
    #   [:div.question-panel ... ]  ; close the div
    #   [:div.loading-question ...]) ; close the loading div
    # )  ; close the if
    # ))) ; close the fn, let, and defn
    
    # Find and replace the problematic line
    old_pattern = r'           "Loading question..."\]))\)'
    new_pattern = r'           "Loading question..."]))))'
    
    content = content.replace(old_pattern, new_pattern)
    
    with open('src/pok/views.cljs', 'w') as f:
        f.write(content)
    
    print("Fixed question-panel brackets")

if __name__ == "__main__":
    fix_brackets()