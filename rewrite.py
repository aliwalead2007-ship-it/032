import re

with open('app/src/main/java/com/example/RealServices.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'private fun buildDefaultIdeaAnalysis.*?^\s*\}\n\s*\}\n', re.MULTILINE | re.DOTALL)

new_content = """    private fun buildDefaultIdeaAnalysis(idea: String): IdeaAnalysis {
        val cleanIdea = idea.trim().ifBlank { "محتوى إسلامي هادف" }
        val parts = cleanIdea.split(Regex("[.،!؟\\n]")).map { it.trim() }.filter { it.length > 5 }
        val generatedScenes = mutableListOf<String>()
        if (parts.size >= 3) {
            generatedScenes.add("المشهد 1: ${parts[0]}")
            generatedScenes.add("المشهد 2: ${parts[1]}")
            generatedScenes.add("المشهد 3: ${parts[2]}")
        } else {
            val shortText = cleanIdea.take(30) + if (cleanIdea.length > 30) "..." else ""
            generatedScenes.add("المشهد 1: مقدمة حول (\$shortText)")
            generatedScenes.add("المشهد 2: عرض الفكرة الأساسية")
            generatedScenes.add("المشهد 3: الخاتمة والدعوة للتفكر")
        }
        return IdeaAnalysis(
            summary = cleanIdea.take(100),
            suggestedStyle = "سينمائي وقور (Gold & Deep Slate)",
            goal = "إيصال رسالة هادفة للمشاهدين",
            targetAudience = "الجمهور العام",
            tone = "خاشع وملهم",
            keywords = cleanIdea.split(" ").take(4).filter { it.length > 3 },
            proposedScenes = generatedScenes,
            viralityScore = 80,
            hookSuggestions = listOf("تعذّر التحليل — أكمل بالفكرة كما هي"),
            ctaSuggestions = listOf("شارك المقطع لتعم الفائدة", "اترك تعليقاً برأيك")
        )
    }
"""

if pattern.search(content):
    content = pattern.sub(new_content, content, count=1)
    with open('app/src/main/java/com/example/RealServices.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Pattern not found")

