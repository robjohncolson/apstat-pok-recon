#!/usr/bin/env python3
"""
Fix keyword mismatch between qualified (::) and unqualified (:) keywords
Standardize everything to unqualified keywords
"""

import re

def fix_keyword_mismatch():
    """Fix qualified/unqualified keyword mismatches"""
    
    print("Fixing keyword mismatches...")
    
    # Fix state.cljs - change all ::subscription to :subscription
    with open('src/pok/state.cljs', 'r') as f:
        state_content = f.read()
    
    # Replace qualified subscription registrations
    replacements = [
        (r'::curriculum', ':curriculum'),
        (r'::mempool', ':mempool'), 
        (r'::chain', ':chain'),
        (r'::distributions', ':distributions'),
        (r'::convergence', ':convergence'),
        (r'::mempool-count', ':mempool-count'),
        (r'::qr-modal-visible', ':qr-modal-visible'),
        (r'::save-local', ':save-local'),
        (r'::load-local', ':load-local')
    ]
    
    for old, new in replacements:
        count = len(re.findall(old, state_content))
        state_content = re.sub(old, new, state_content)
        if count > 0:
            print(f"  state.cljs: Replaced {count} instances of {old} with {new}")
    
    with open('src/pok/state.cljs', 'w') as f:
        f.write(state_content)
    
    # Fix views.cljs - change all ::subscription to :subscription  
    with open('src/pok/views.cljs', 'r') as f:
        views_content = f.read()
    
    view_replacements = [
        (r'::curriculum', ':curriculum'),
        (r'::mempool', ':mempool'),
        (r'::chain', ':chain'), 
        (r'::mempool-count', ':mempool-count'),
        (r'::qr-modal-visible', ':qr-modal-visible')
    ]
    
    for old, new in view_replacements:
        count = len(re.findall(old, views_content))
        views_content = re.sub(old, new, views_content)
        if count > 0:
            print(f"  views.cljs: Replaced {count} instances of {old} with {new}")
    
    with open('src/pok/views.cljs', 'w') as f:
        f.write(views_content)
    
    print("Keyword mismatch fixes applied!")

if __name__ == '__main__':
    fix_keyword_mismatch()