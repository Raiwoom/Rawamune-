package p2p.messenger.hardcore.net

object PayloadHandler {

    // Сборка текстового сообщения с датой и временем
    fun createTextMessageJson(text: String): String {
        return """{
            "type": "TEXT",
            "timestamp": ${System.currentTimeMillis()},
            "body": "$text"
        }""".trimIndent()
    }

    // Сборка опроса или викторины
    fun createPollJson(question: String, options: List<String>, isQuiz: Boolean, correctAnswerIndex: Int): String {
        val optionsStr = options.joinToString(",") { "\"$it\"" }
        return """{
            "type": "POLL",
            "timestamp": ${System.currentTimeMillis()},
            "question": "$question",
            "options": [$optionsStr],
            "is_quiz": $isQuiz,
            "correct_index": $correctAnswerIndex
        }""".trimIndent()
    }

    // Сборка любого медиафайла (Фото, Видео, Голосовое, Документ, Любое Аудио) в формате Base64
    fun createMediaJson(fileType: String, fileName: String, base64Data: String): String {
        return """{
            "type": "MEDIA",
            "timestamp": ${System.currentTimeMillis()},
            "media_type": "$fileType", // "IMAGE", "VIDEO", "VOICE", "AUDIO", "DOCUMENT"
            "file_name": "$fileName",
            "bytes_base64": "$base64Data"
        }""".trimIndent()
    }

    // Сборка пакетов для управления вашими галочками и кружочками статусов доставки в личках и группах
    fun createStatusUpdateJson(messageUuid: String, newStatus: String): String {
        return """{
            "type": "STATUS_UPDATE",
            "timestamp": ${System.currentTimeMillis()},
            "msg_id": "$messageUuid",
            "status": "$newStatus" // "DELIVERED" (кружочек) или "READ" (зеленый кружочек)
        }""".trimIndent()
    }
}
