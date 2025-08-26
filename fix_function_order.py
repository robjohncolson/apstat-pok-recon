#!/usr/bin/env python3
"""
Fix function declaration order in state.cljs
"""

import re

def fix_function_order():
    """Move function definitions before they are used"""
    
    # Read the state.cljs file
    with open('src/pok/state.cljs', 'r') as f:
        content = f.read()
    
    # Find the load-from-local function
    load_from_local_pattern = r'(\(defn load-from-local\s+"Load data from localStorage"\s+\[key\](?:\s|\S)*?catch js/Error _ nil\)\)\))'
    load_from_local_match = re.search(load_from_local_pattern, content, re.MULTILINE | re.DOTALL)
    
    if load_from_local_match:
        load_from_local_func = load_from_local_match.group(1)
        # Remove it from its current location
        content = re.sub(load_from_local_pattern, '', content, flags=re.MULTILINE | re.DOTALL)
        
        # Insert it after the save-to-local function
        save_to_local_pattern = r'(\(defn save-to-local\s+"Save data to localStorage"\s+\[key data\](?:\s|\S)*?clj->js data\)\)\))'
        content = re.sub(save_to_local_pattern, r'\1\n\n' + load_from_local_func, content, flags=re.MULTILINE | re.DOTALL)
    
    # Find the derive-pubkey-map-from-chain function
    derive_pubkey_pattern = r'(\(defn derive-pubkey-map-from-chain\s+"Derives pubkey->username mapping from create-user transactions in chain"(?:\s|\S)*?\{\} chain\)\))'
    derive_pubkey_match = re.search(derive_pubkey_pattern, content, re.MULTILINE | re.DOTALL)
    
    if derive_pubkey_match:
        derive_pubkey_func = derive_pubkey_match.group(1)
        # Remove it from its current location
        content = re.sub(derive_pubkey_pattern, '', content, flags=re.MULTILINE | re.DOTALL)
        
        # Insert it after the Profile record definition (around line 60)
        archetype_constants_pattern = r'(\(def \^:const ARCHETYPES(?:\s|\S)*?\{:emoji "🤝" :description "Collaborative and helpful"\}\})\)'
        content = re.sub(archetype_constants_pattern, r'\1\n\n;; Derive pubkey-to-username mapping from chain\n' + derive_pubkey_func, content, flags=re.MULTILINE | re.DOTALL)
    
    # Write the updated file
    with open('src/pok/state.cljs', 'w') as f:
        f.write(content)
    
    print("Fixed function declaration order")

if __name__ == '__main__':
    fix_function_order()