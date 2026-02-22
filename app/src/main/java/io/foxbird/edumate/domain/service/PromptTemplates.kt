package io.foxbird.edumate.domain.service

object PromptTemplates {

    fun qaPrompt(context: String, query: String, conversationHistory: String = ""): String {
        val historySection = if (conversationHistory.isNotBlank()) {
            "\n\nConversation so far:\n$conversationHistory\n"
        } else ""

        return buildString {
            append("System: You are EduMate, a friendly and knowledgeable educational tutor ")
            append("for students in grades 5 through 10. Answer questions using ONLY the ")
            append("provided study material. If the answer isn't in the material, say so ")
            append("honestly. Keep explanations clear and age-appropriate. Use examples when helpful.\n\n")
            append("Study Material:\n$context\n")
            append(historySection)
            append("\nUser: $query\n\nAssistant:")
        }
    }

    fun summaryPrompt(context: String): String {
        return buildString {
            append("System: Summarize the following study material in a clear, concise way ")
            append("suitable for a student. Include key points and important concepts.\n\n")
            append("Material:\n$context\n\nSummary:")
        }
    }

    fun quizPrompt(context: String, numQuestions: Int = 5): String {
        return buildString {
            append("System: Generate $numQuestions quiz questions based on the following study material. ")
            append("Include a mix of multiple choice, true/false, and short answer questions. ")
            append("Provide the correct answers after each question.\n\n")
            append("Material:\n$context\n\nQuiz:")
        }
    }

    fun explainPrompt(concept: String, context: String): String {
        return buildString {
            append("System: Explain the concept of \"$concept\" using the provided study material. ")
            append("Make it simple and easy to understand for a middle school student. ")
            append("Use analogies and examples when possible.\n\n")
            append("Material:\n$context\n\nExplanation:")
        }
    }

    fun titlePrompt(firstMessage: String): String {
        return buildString {
            append("System: Generate a short, descriptive title (max 6 words) for a conversation ")
            append("that starts with the following message. Return only the title, nothing else.\n\n")
            append("Message: $firstMessage\n\nTitle:")
        }
    }

    fun conceptExtractionPrompt(text: String): String {
        return buildString {
            append("System: Extract key concepts from the following text. Return a JSON array of objects ")
            append("with fields: name, type (one of: term, person, formula, theorem, concept, event, place, definition), ")
            append("and definition (brief). Return ONLY valid JSON, no explanation.\n\n")
            append("Text:\n$text\n\nJSON:")
        }
    }
}
