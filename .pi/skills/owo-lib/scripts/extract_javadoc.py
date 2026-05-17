#!/usr/bin/env python3
"""
Extract JavaDoc from owo-lib source code and organize into reference files.
Focuses on PUBLIC API only (public classes, methods, fields).
"""

import os
import re
from pathlib import Path
from collections import defaultdict, OrderedDict

SRC_DIR = Path(r"C:/Users/coper/Documents/AI-Workspace/Skills/skill_workshop/in_progress/owo-lib/repos/owo-lib/src/main/java")
OUT_DIR = Path(r"C:/Users/coper/Documents/AI-Workspace/Skills/skill_workshop/in_progress/owo-lib/references")
BASE_PKG = "io/wispforest/owo"

# Module mapping: subdirectory -> module name
MODULE_MAP = {
    "blockentity": "blockentity",
    "braid": "braid",
    "client": "client",
    "command": "command",
    "compat": "compat",
    "config": "config",
    "ext": "ext",
    "itemgroup": "itemgroup",
    "moddata": "moddata",
    "network": "networking",
    "ops": "ops",
    "particles": "particles",
    "registration": "registration",
    "renderdoc": "renderdoc",
    "serialization": "endec",
    "text": "text",
    "ui": "ui",
    "util": "util",
}


def get_module(filepath):
    """Determine module from filepath."""
    rel = os.path.relpath(filepath, SRC_DIR / BASE_PKG)
    parts = rel.replace("\\", "/").split("/")
    top = parts[0]
    return MODULE_MAP.get(top, "other")


def clean_javadoc(raw):
    """Clean a raw JavaDoc comment into readable markdown text."""
    lines = raw.split('\n')
    cleaned = []
    for line in lines:
        line = line.strip()
        line = re.sub(r'^/\*\*?\s*', '', line)
        line = re.sub(r'\s*\*/$', '', line)
        line = re.sub(r'^\*\s?', '', line)
        if line:
            cleaned.append(line)
    return ' '.join(cleaned).strip() if cleaned else None


def extract_javadoc_before(content, pos):
    """Extract JavaDoc comment immediately before position pos."""
    # Walk backward from pos, skip whitespace/newlines, find */
    i = pos - 1
    while i >= 0 and content[i] in ' \t\r\n':
        i -= 1
    if i < 0 or content[i:i+1] != '/' or content[i-1:i] != '*':
        return None
    
    # We found */, now find /**
    end = i + 1  # position after *
    j = i - 1  # position at /
    # j is at the / of */, go back to find /**
    start = content.rfind('/**', 0, j)
    if start == -1:
        return None
    
    # Check there's nothing but whitespace between start..end block and the target
    between = content[end:pos].strip()
    if between:
        return None
    
    raw = content[start:end]
    return clean_javadoc(raw)


def extract_package(content):
    m = re.search(r'package\s+([\w.]+)\s*;', content)
    return m.group(1) if m else "unknown"


def find_matching_brace(content, start):
    """Find matching } for { at position start."""
    depth = 0
    i = start
    in_string = False
    in_char = False
    in_line_comment = False
    in_block_comment = False
    
    while i < len(content):
        c = content[i]
        
        if in_line_comment:
            if c == '\n':
                in_line_comment = False
            i += 1
            continue
        
        if in_block_comment:
            if c == '*' and i + 1 < len(content) and content[i+1] == '/':
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        
        if in_string:
            if c == '\\':
                i += 2
                continue
            if c == '"':
                in_string = False
            i += 1
            continue
        
        if in_char:
            if c == '\\':
                i += 2
                continue
            if c == "'":
                in_char = False
            i += 1
            continue
        
        if c == '/' and i + 1 < len(content):
            if content[i+1] == '/':
                in_line_comment = True
                i += 2
                continue
            elif content[i+1] == '*':
                in_block_comment = True
                i += 2
                continue
        
        if c == '"':
            in_string = True
            i += 1
            continue
        if c == "'":
            in_char = True
            i += 1
            continue
        
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        
        i += 1
    return -1


def is_public_or_protected(text):
    """Check if a declaration text contains public or protected."""
    return bool(re.search(r'\bpublic\b', text) or re.search(r'\bprotected\b', text))


def parse_class_body(body, class_name, pkg):
    """Parse methods and fields from a class body string."""
    methods = []
    fields = []
    
    # Find all member declarations
    # Strategy: iterate through body looking for patterns
    
    # Remove nested class bodies first to avoid false matches
    # Actually, let's just use a careful regex approach
    
    # Method: visibility modifiers + return type + name + (params)
    # Field: visibility modifiers + type + name + = value or ;
    
    # Find positions of all /** comment blocks to map JavaDoc
    javadoc_map = {}  # end_pos -> javadoc_text
    for m in re.finditer(r'/\*\*[\s\S]*?\*/', body):
        jd = clean_javadoc(m.group(0))
        if jd:
            javadoc_map[m.end()] = jd
    
    # Find all member-like declarations
    # We look for: optional annotations, then modifiers, then type, then name
    member_re = re.compile(
        r'(?:@\w+(?:\s*\([^)]*\))?\s+)*'  # annotations
        r'((?:(?:public|protected|private|static|final|abstract|synchronized|native|default|transient|volatile|strictfp)\s+)*)'  # modifiers
        r'(<[^>]+>\s+)?'  # optional generic type params
        r'([\w.]+(?:<[^>]+>)?(?:\[\])*)\s+'  # return type
        r'(\w+)\s*'  # name
        r'([({;=])',  # delimiter
    )
    
    for m in member_re.finditer(body):
        modifiers = m.group(1) or ''
        ret_type = (m.group(3) or '').strip()
        name = m.group(4)
        delim = m.group(5)
        
        # Only public/protected
        if not ('public' in modifiers or 'protected' in modifiers):
            continue
        
        # Skip common noise
        if name in ('toString', 'hashCode', 'equals', 'getClass', 'notify',
                    'notifyAll', 'wait', 'finalize', 'serialVersionUID'):
            continue
        
        # Find associated JavaDoc - look for a /** ending just before this match
        jd = None
        pre_start = m.start()
        # Look for javadoc ending right before this member
        for jd_end, jd_text in javadoc_map.items():
            # Check if the javadoc ends right before this member (allowing whitespace)
            between = body[jd_end:pre_start].strip()
            if not between and jd_end <= pre_start:
                jd = jd_text
                break
        
        if delim == '(':
            # Method or constructor
            # Extract params
            paren_start = body.index('(', m.start())
            depth = 0
            paren_end = paren_start
            for k in range(paren_start, len(body)):
                if body[k] == '(':
                    depth += 1
                elif body[k] == ')':
                    depth -= 1
                    if depth == 0:
                        paren_end = k
                        break
            
            params_raw = body[paren_start+1:paren_end]
            # Clean params - keep type and name
            params = clean_params(params_raw)
            
            is_constructor = (name == class_name)
            
            # Skip methods without javadoc that are simple overrides or getters/setters
            if not jd:
                if name.startswith('get') or name.startswith('set') or name.startswith('is'):
                    continue
                # Check for @Override annotation
                pre_text = body[max(0, m.start()-100):m.start()]
                if '@Override' in pre_text:
                    continue
            
            methods.append({
                'name': name,
                'return_type': ret_type,
                'params': params,
                'modifiers': modifiers.strip(),
                'javadoc': jd,
                'is_constructor': is_constructor
            })
        else:
            # Field
            if not jd and 'static final' in modifiers:
                continue
            
            fields.append({
                'name': name,
                'type': ret_type,
                'modifiers': modifiers.strip(),
                'javadoc': jd
            })
    
    return methods, fields


def clean_params(param_str):
    """Format parameter string."""
    if not param_str.strip():
        return ""
    params = []
    for p in param_str.split(','):
        p = p.strip()
        if p:
            params.append(p)
    return ', '.join(params)


def parse_java_file(filepath):
    """Parse a Java file and extract public API with JavaDoc."""
    with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
        content = f.read()
    
    package = extract_package(content)
    
    # Skip mixin files
    if '/mixin/' in filepath.replace('\\', '/'):
        return package, []
    
    classes = []
    
    # Find all top-level class/interface/enum/record declarations
    # We need to find the declaration and check if it's public
    class_re = re.compile(
        r'\b(class|interface|enum|record)\s+(\w+)'
    )
    
    for m in class_re.finditer(content):
        class_type = m.group(1)
        class_name = m.group(2)
        
        # Get the line containing the declaration to check visibility
        line_start = content.rfind('\n', 0, m.start()) + 1
        line_end = content.find('\n', m.start())
        if line_end == -1:
            line_end = len(content)
        
        # Look at a window before the class keyword for visibility modifiers
        pre_window = content[max(0, m.start()-150):m.start()]
        if not is_public_or_protected(pre_window):
            continue
        
        # Extract JavaDoc before this declaration
        jd = extract_javadoc_before(content, line_start)
        
        # Find class body
        brace_pos = content.find('{', m.end())
        if brace_pos == -1:
            continue
        
        body_end = find_matching_brace(content, brace_pos)
        if body_end == -1:
            continue
        
        body = content[brace_pos+1:body_end]
        
        # Parse members
        methods, fields = parse_class_body(body, class_name, package)
        
        # Find nested public classes
        nested = []
        for nm in class_re.finditer(body):
            ntype = nm.group(1)
            nname = nm.group(2)
            npre = body[max(0, nm.start()-150):nm.start()]
            if is_public_or_protected(npre):
                njd = None
                # Find javadoc for nested class
                nline_start = body.rfind('\n', 0, nm.start()) + 1
                # Look for javadoc ending before this
                for jd_end_pos in range(nline_start-1, max(0, nline_start-1000), -1):
                    if body[jd_end_pos:jd_end_pos+2] == '*/':
                        # Found end of a comment, check if it's /**
                        comment_start = body.rfind('/**', 0, jd_end_pos)
                        if comment_start != -1:
                            njd = clean_javadoc(body[comment_start:jd_end_pos+2])
                        break
                
                nested.append({
                    'name': nname,
                    'type': ntype,
                    'javadoc': njd
                })
        
        classes.append({
            'name': class_name,
            'type': class_type,
            'javadoc': jd,
            'methods': methods,
            'fields': fields,
            'nested_classes': nested
        })
    
    return package, classes


def format_module(module_name, classes_by_package):
    """Format a module's documentation into markdown."""
    lines = []
    title = module_name.capitalize()
    lines.append(f"# JavaDoc Reference: {title}")
    lines.append("")
    
    # TOC
    toc = []
    for pkg in sorted(classes_by_package.keys()):
        for cls in classes_by_package[pkg]:
            anchor = f"{cls['name'].lower()}"
            toc.append(f"- [{cls['name']}](#{anchor}) — `{pkg}`")
    
    lines.append("## Table of Contents")
    lines.append("")
    lines.extend(toc)
    lines.append("")
    
    # Body
    for pkg in sorted(classes_by_package.keys()):
        pkg_classes = classes_by_package[pkg]
        if not pkg_classes:
            continue
        
        lines.append(f"## Package: `{pkg}`")
        lines.append("")
        
        for cls in pkg_classes:
            lines.append(f"### `{cls['type']}` {cls['name']}")
            if cls['javadoc']:
                lines.append("")
                for p in cls['javadoc'].split('<p>'):
                    p = p.strip()
                    if p:
                        lines.append(f"> {p}")
                        lines.append(">")
            lines.append("")
            
            # Nested classes
            for nc in cls.get('nested_classes', []):
                if nc.get('javadoc'):
                    lines.append(f"#### `{nc['type']}` {nc['name']}")
                    lines.append("")
                    for p in nc['javadoc'].split('<p>'):
                        p = p.strip()
                        if p:
                            lines.append(f"> {p}")
                            lines.append(">")
                    lines.append("")
            
            # Fields with javadoc
            doc_fields = [f for f in cls['fields'] if f['javadoc']]
            if doc_fields:
                lines.append("**Fields:**")
                lines.append("")
                for f in doc_fields:
                    lines.append(f"- `{f['type']} {f['name']}` — {f['javadoc']}")
                lines.append("")
            
            # Constructors first, then methods
            ctors = [m for m in cls['methods'] if m['is_constructor']]
            methods = [m for m in cls['methods'] if not m['is_constructor']]
            
            if ctors:
                lines.append("**Constructors:**")
                lines.append("")
                for m in ctors:
                    sig = f"{m['name']}({m['params']})"
                    jd = f"\n  > {m['javadoc']}" if m['javadoc'] else ""
                    lines.append(f"- `{sig}`{jd}")
                lines.append("")
            
            if methods:
                lines.append("**Methods:**")
                lines.append("")
                for m in methods:
                    sig = f"{m['return_type']} {m['name']}({m['params']})"
                    jd = f"\n  > {m['javadoc']}" if m['javadoc'] else ""
                    lines.append(f"- `{sig}`{jd}")
                lines.append("")
    
    return '\n'.join(lines)


def main():
    print("Scanning Java source files...")
    
    java_files = []
    for root, dirs, files in os.walk(SRC_DIR):
        for f in files:
            if f.endswith('.java'):
                java_files.append(os.path.join(root, f))
    
    print(f"Found {len(java_files)} Java files")
    
    # Collect data by module -> package -> classes
    module_data = defaultdict(lambda: defaultdict(list))
    
    total_classes = 0
    total_methods = 0
    total_fields = 0
    total_packages = set()
    classes_with_javadoc = 0
    methods_with_javadoc = 0
    
    for filepath in java_files:
        pkg, classes = parse_java_file(filepath)
        if not classes:
            continue
        
        module = get_module(filepath)
        total_packages.add(pkg)
        
        for cls in classes:
            total_classes += 1
            if cls['javadoc']:
                classes_with_javadoc += 1
            total_methods += len(cls['methods'])
            for m in cls['methods']:
                if m['javadoc']:
                    methods_with_javadoc += 1
            total_fields += len(cls['fields'])
            
            # Only add to output if there's ANY javadoc in the class
            has_jd = bool(cls['javadoc'])
            if not has_jd:
                for m in cls['methods']:
                    if m['javadoc']:
                        has_jd = True
                        break
            if not has_jd:
                for f in cls['fields']:
                    if f['javadoc']:
                        has_jd = True
                        break
            if not has_jd:
                for nc in cls.get('nested_classes', []):
                    if nc.get('javadoc'):
                        has_jd = True
                        break
            
            if has_jd:
                module_data[module][pkg].append(cls)
    
    print(f"Classes with JavaDoc: {classes_with_javadoc}")
    print(f"Methods with JavaDoc: {methods_with_javadoc}")
    print(f"Total classes processed: {total_classes}")
    print(f"Total methods documented: {total_methods}")
    print(f"Total fields documented: {total_fields}")
    print(f"Total packages found: {len(total_packages)}")
    
    # Generate output
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    output_files = []
    
    for module in sorted(module_data.keys()):
        if not any(module_data[module].values()):
            continue
        
        filename = f"javadoc-{module}.md"
        filepath = OUT_DIR / filename
        content = format_module(module, module_data[module])
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        output_files.append(filename)
        
        mod_classes = sum(len(v) for v in module_data[module].values())
        print(f"  {module}: {mod_classes} classes -> {filename}")
    
    # Summary
    summary = []
    summary.append("# JavaDoc Extraction Summary")
    summary.append("")
    summary.append("## Statistics")
    summary.append("")
    summary.append(f"- **Total Java files scanned:** {len(java_files)}")
    summary.append(f"- **Total classes processed:** {total_classes}")
    summary.append(f"- **Classes with JavaDoc:** {classes_with_javadoc}")
    summary.append(f"- **Total methods documented:** {total_methods}")
    summary.append(f"- **Methods with JavaDoc:** {methods_with_javadoc}")
    summary.append(f"- **Total fields documented:** {total_fields}")
    summary.append(f"- **Total packages found:** {len(total_packages)}")
    summary.append("")
    
    summary.append("## Modules Documented")
    summary.append("")
    for module in sorted(module_data.keys()):
        pkgs = sorted(module_data[module].keys())
        cls_list = []
        for p in pkgs:
            for c in module_data[module][p]:
                cls_list.append((p, c['name'], c['type'], len(c['methods']), len(c['fields'])))
        
        summary.append(f"### {module.capitalize()} (`javadoc-{module}.md`)")
        summary.append(f"- Packages: {len(pkgs)}")
        summary.append(f"- Classes: {len(cls_list)}")
        summary.append("")
        summary.append("| Package | Class | Type | Methods | Fields |")
        summary.append("|---------|-------|------|---------|--------|")
        for p, n, ct, mc, fc in cls_list:
            summary.append(f"| `{p}` | `{n}` | {ct} | {mc} | {fc} |")
        summary.append("")
    
    summary.append("## All Packages")
    summary.append("")
    for p in sorted(total_packages):
        summary.append(f"- `{p}`")
    summary.append("")
    
    summary.append("## Output Files")
    summary.append("")
    for f in output_files:
        summary.append(f"- `{f}`")
    
    with open(OUT_DIR / "javadoc-summary.md", 'w', encoding='utf-8') as f:
        f.write('\n'.join(summary))
    output_files.append("javadoc-summary.md")
    
    print(f"\nGenerated {len(output_files)} output files in {OUT_DIR}")


if __name__ == "__main__":
    main()
