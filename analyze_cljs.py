#!/usr/bin/env python3
import re
import json

# Read the file
with open('/mnt/c/Users/rober/Downloads/apstat-pok-recon/src/pok/state.cljs', 'r', encoding='utf-8') as f:
    content = f.read()

# Check for various issues
issues = []

# 1. Check for unbalanced parens around problematic area (lines 30-70)
lines = content.split('\n')
problem_section = '\n'.join(lines[29:70])  # lines 30-70

open_parens = problem_section.count('(')
close_parens = problem_section.count(')')
if open_parens != close_parens:
    issues.append(f'Unbalanced parentheses in lines 30-70: {open_parens} open, {close_parens} close')

# 2. Check for invisible/non-printable characters
invisible_chars = []
for i, char in enumerate(content):
    if ord(char) > 127 or (ord(char) < 32 and char not in '\n\r\t'):
        line_num = content[:i].count('\n') + 1
        invisible_chars.append(f'Line {line_num}: char code {ord(char)} ({repr(char)})')

if invisible_chars:
    issues.append(f'Found {len(invisible_chars)} invisible/non-ASCII characters: {invisible_chars[:5]}')

# 3. Look for common problematic patterns
patterns = [
    (r'\(\s*comment\s', 'comment blocks'),
    (r'#_', 'reader discards'),  
    (r'#\?\s*:clj', 'JVM-only reader conditionals'),
    (r'\(\s*if\s+false', 'false conditionals'),
    (r'\(\s*when\s+false', 'false when blocks'),
]

for pattern, desc in patterns:
    matches = list(re.finditer(pattern, content, re.IGNORECASE | re.MULTILINE))
    if matches:
        for match in matches:
            line_num = content[:match.start()].count('\n') + 1
            issues.append(f'Found {desc} at line {line_num}: {match.group()}')

# 4. Check specific problematic lines around defns
defn_lines = []
for i, line in enumerate(lines):
    if 'defn ' in line:
        defn_lines.append((i+1, line.strip()))

if defn_lines:
    print(f'Found defn statements at lines: {[line_num for line_num, _ in defn_lines]}')

print('Issues found:')
for issue in issues:
    print(f'  - {issue}')

if not issues:
    print('  No obvious syntax issues detected')

# 5. Check for structural issues around the problem area
print('\nDetailed analysis of lines 30-70:')
for i, line in enumerate(lines[29:70], start=30):
    line_clean = line.strip()
    if line_clean:
        # Check for suspicious patterns
        suspicious = []
        if line_clean.startswith('(') and not line_clean.endswith(')') and ')' not in line_clean:
            suspicious.append('unclosed paren')
        if '"' in line_clean and line_clean.count('"') % 2 != 0:
            suspicious.append('unclosed string')
        if line_clean.startswith(';'):
            suspicious.append('comment line')
            
        status = f' [{", ".join(suspicious)}]' if suspicious else ''
        print(f'{i:3d}: {line_clean}{status}')

# 6. Paren balance check per line in problem area
print('\nParen balance check (lines 30-70):')
running_balance = 0
for i, line in enumerate(lines[29:70], start=30):
    open_count = line.count('(')
    close_count = line.count(')')
    running_balance += open_count - close_count
    if open_count > 0 or close_count > 0 or running_balance != 0:
        print(f'{i:3d}: +{open_count} -{close_count} = {running_balance} | {line.strip()[:50]}')