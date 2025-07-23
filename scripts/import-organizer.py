#!/usr/bin/env python3
import os


def scan_for_files(root_dir):
    java_files = []
    for root, dirs, files in os.walk(root_dir):
        # we do a little bit of recursion
        for nested_dir in dirs:
            if nested_dir == "remappedSrc":
                continue
            for nested_file in scan_for_files(nested_dir):
                java_files.append(nested_file)

        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))

    return java_files


def sort_imports(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        lines = file.readlines()

    other_lines = []
    import_block = []
    package_statement = []
    inside_import_block = False
    found_import_statement = False
    found_package_statement = False

    for line in lines:
        if line.strip().startswith("package "):
            found_package_statement = True
            package_statement.append(line)
        elif line.strip().startswith("import "):
            # Start walking the import block
            inside_import_block = True
            found_import_statement = True
            import_block.append(line.strip())
        elif inside_import_block:
            if len(line.strip()) == 0:
                # Remove empty lines from the import block
                continue
            else:
                # Non-empty non-import line marks the end of the import block
                inside_import_block = False
                other_lines.append(line)
        elif len(line.strip()) == 0 and not found_import_statement:
            # Empty line between package statement and import block
            package_statement.append(line)
        elif not found_package_statement:
            # Sometimes comments go above a package statement
            package_statement.append(line)
        else:
            # All other lines including empty lines
            other_lines.append(line)

    # Check if imports were already sorted
    if all(len(import_block[i]) <= len(import_block[i + 1]) for i in range(len(import_block) - 1)):
        return False
    else:
        # Sort imports by length
        import_block.sort(key=len)
        sorted_content = "".join(package_statement) + "\n".join(import_block) + "\n\n" + "".join(other_lines)

        with open(file_path, 'w',  encoding='utf-8') as file:
            file.write(sorted_content)

        return True


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, os.pardir))

    print(f'Scanning project root: "{project_root}"')

    source_files = scan_for_files(project_root)
    print(f'Attempting import sort in {len(source_files)} Java source files...')

    sorted_imports = []
    for src in source_files:
        if sort_imports(src):
            sorted_imports.append(os.path.basename(src))

    print(
        f'\nSuccessfully sorted imports '
        f'in {len(sorted_imports)} out of {len(source_files)} '
        f'total Java files in the {os.path.basename(project_root)} project.'
    )


if __name__ == "__main__":
    main()
