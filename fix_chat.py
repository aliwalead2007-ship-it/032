with open('app/src/main/java/com/example/RealServices.kt', 'r') as f:
    content = f.read()

bad_str = """            ""
        }

    suspend fun chatWithAssistant("""

good_str = """            ""
        }
    }

    suspend fun chatWithAssistant("""

content = content.replace(bad_str, good_str)
with open('app/src/main/java/com/example/RealServices.kt', 'w') as f:
    f.write(content)
print("done")
