import os
import glob

directory = 'MMO_Market/MMO_Market (3)/MMO_Market/database/sql_scripts/migration'
sql_files = glob.glob(os.path.join(directory, '*.sql'))

header = "USE MMO_System_Schema;\nGO\n\n"

updated = 0
for file_path in sql_files:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if 'USE MMO_System_Schema' not in content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(header + content)
            print(f"Updated {file_path}")
            updated += 1
        else:
            print(f"Skipped {file_path}")
    except Exception as e:
        print(f"Failed {file_path}: {e}")

print(f"Done. Updated {updated} files.")
