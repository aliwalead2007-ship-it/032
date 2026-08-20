with open('app/src/main/java/com/example/Models.kt', 'r') as f:
    content = f.read()

old_str = "val ctaSuggestions: List<String> = emptyList()"
new_str = "val ctaSuggestions: List<String> = emptyList(),\n    val confidence: Double = 1.0,\n    val themes: List<String> = emptyList()"
content = content.replace(old_str, new_str)

with open('app/src/main/java/com/example/Models.kt', 'w') as f:
    f.write(content)
print("Updated Models.kt")
