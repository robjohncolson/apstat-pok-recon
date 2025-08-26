#!/usr/bin/env python3
"""
Fix subscription namespace mismatches between views.cljs and state.cljs
"""

import re

def fix_subscription_namespaces():
    """Fix ::state/ subscription references in views.cljs"""
    
    # Read the views.cljs file
    with open('src/pok/views.cljs', 'r') as f:
        content = f.read()
    
    # Replace ::state/subscription with ::subscription
    replacements = [
        (r'\[::state/mempool-count\]', '[::mempool-count]'),
        (r'\[::state/chain\]', '[::chain]'),
        (r'\[::state/curriculum\]', '[::curriculum]'),
        (r'\[::state/qr-modal-visible\]', '[::qr-modal-visible]')
    ]
    
    for old_pattern, new_pattern in replacements:
        count = len(re.findall(old_pattern, content))
        content = re.sub(old_pattern, new_pattern, content)
        if count > 0:
            print(f"Replaced {count} instances of {old_pattern} with {new_pattern}")
    
    # Write the updated file
    with open('src/pok/views.cljs', 'w') as f:
        f.write(content)
    
    print("Fixed subscription namespace mismatches in views.cljs")

if __name__ == '__main__':
    fix_subscription_namespaces()