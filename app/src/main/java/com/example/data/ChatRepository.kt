package com.example.data

import com.example.data.api.AiRetrofitClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.OpenRouterContentPart
import com.example.data.api.OpenRouterImageUrl
import com.example.data.api.OpenRouterMessage
import com.example.data.api.OpenRouterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import android.util.Log

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessages(sessionId: Int): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createSession(title: String): Long = withContext(Dispatchers.IO) {
        chatDao.insertSession(ChatSession(title = title))
    }

    suspend fun updateSessionTitle(sessionId: Int, title: String) = withContext(Dispatchers.IO) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    suspend fun deleteSession(sessionId: Int) = withContext(Dispatchers.IO) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun saveMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun clearSessionMessages(sessionId: Int) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
    }

    /**
     * Sends a chat request to OpenRouter (or Gemini fallback) and returns the assistant's ChatMessage response.
     */
    suspend fun sendAiRequest(
        sessionId: Int,
        modelName: String,
        isGemini: Boolean,
        messages: List<ChatMessage>,
        systemInstruction: String,
        geminiApiKey: String,
        openRouterApiKey: String
    ): ChatMessage = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var promptTokens = 0
        var completionTokens = 0
        var reasoningTokens = 0
        var assistantContent = ""
        var reasoningContent: String? = null
        var actualModel = modelName

        if (openRouterApiKey.isBlank() || openRouterApiKey == "MY_OPENROUTER_API_KEY") {
            return@withContext ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                content = "⚠️ **مفتاح OpenRouter API مطلوب / OpenRouter Key Required**\n\nيرجى التوجه إلى **الإعدادات (⚙️ Settings)** وإدخال مفتاح OpenRouter الخاص بك للتواصل مع كافة الموديلات المتاحة.\n\nيمكنك الحصول على مفتاح مجاني من [OpenRouter.ai](https://openrouter.ai/keys).",
                modelName = modelName
            )
        }

        try {
            // Prepare OpenRouter multimodal messages
            val openRouterMessages = mutableListOf<OpenRouterMessage>()
            
            if (systemInstruction.isNotBlank()) {
                openRouterMessages.add(OpenRouterMessage(role = "system", content = systemInstruction))
            }
            
            messages.forEach { msg ->
                if (!msg.imageUri.isNullOrBlank()) {
                    val parts = mutableListOf<OpenRouterContentPart>()
                    if (msg.content.isNotBlank()) {
                        parts.add(OpenRouterContentPart(type = "text", text = msg.content))
                    }
                    parts.add(OpenRouterContentPart(type = "image_url", imageUrl = com.example.data.api.OpenRouterImageUrl(url = msg.imageUri)))
                    openRouterMessages.add(OpenRouterMessage(role = msg.role, content = parts))
                } else {
                    openRouterMessages.add(OpenRouterMessage(role = msg.role, content = msg.content))
                }
            }

            val request = OpenRouterRequest(
                model = modelName,
                messages = openRouterMessages,
                temperature = 0.7,
                includeReasoning = true
            )

            val response = AiRetrofitClient.openRouterApi.chatCompletions(
                authorization = "Bearer $openRouterApiKey",
                request = request
            )

            val choice = response.choices?.firstOrNull()
            var rawContent = choice?.message?.content ?: "⚠️ **لم يتم استلام رد من OpenRouter.**"
            reasoningContent = choice?.message?.reasoning

            // Extract <think>...</think> if present in the raw text
            if (rawContent.contains("<think>") && rawContent.contains("</think>")) {
                val thinkStart = rawContent.indexOf("<think>") + 7
                val thinkEnd = rawContent.indexOf("</think>")
                if (thinkEnd > thinkStart) {
                    reasoningContent = rawContent.substring(thinkStart, thinkEnd).trim()
                    rawContent = rawContent.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
                }
            }

            assistantContent = rawContent
            actualModel = response.model ?: modelName
            
            response.usage?.let { usage ->
                promptTokens = usage.promptTokens ?: 0
                completionTokens = usage.completionTokens ?: 0
                reasoningTokens = usage.completionTokensDetails?.reasoningTokens ?: 0
            }
        } catch (e: retrofit2.HttpException) {
            Log.e("ChatRepository", "HTTP Exception code=${e.code()}", e)
            val rawErrorBody = try {
                e.response()?.errorBody()?.string() ?: ""
            } catch (_: Exception) { "" }

            assistantContent = when (e.code()) {
                401 -> "⚠️ **خطأ المصادقة (401 Unauthorized)**\nمفتاح API المستعمل غير صحيح أو منتهي الصلاحية. يرجى مراجعة المفتاح في الإعدادات (⚙️ Settings).\n\n$rawErrorBody"
                400 -> "⚠️ **طلب غير صحيح (400 Bad Request)**\nقد يكون اسم النموذج غير مدعوم أو أن صيغة الطلب غير متوافقة.\n\n$rawErrorBody"
                403 -> "⚠️ **تم رفض الوصول (403 Forbidden)**\nيرجى التأكد من صلاحيات مفتاح API أو القيود الجغرافية.\n\n$rawErrorBody"
                429 -> "⚠️ **تجاوز حد الاستخدام (429 Rate Limit Exceeded)**\nتم تجاوز عدد الطلبات المسموح بها في الوقت الحالي. يرجى الانتظار قليلاً وإعادة المحاولة."
                500, 502, 503 -> "⚠️ **خطأ في خادم الذكاء الاصطناعي (${e.code()})**\nالخادم غير متاح حالياً. يرجى المحاولة لاحقاً."
                else -> "⚠️ **خطأ في الاتصال بالسيرفر (${e.code()})**\n$rawErrorBody"
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "AI Request failed", e)
            assistantContent = "⚠️ **فشل الاتصال بالخدمة:** ${e.localizedMessage ?: e.message}\nيرجى التأكد من الاتصال بالإنترنت ومراجعة الإعدادات."
        }

        val latencyMs = System.currentTimeMillis() - startTime

        ChatMessage(
            sessionId = sessionId,
            role = "assistant",
            content = assistantContent,
            modelName = actualModel,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            reasoningTokens = reasoningTokens,
            latencyMs = latencyMs,
            reasoningContent = reasoningContent
        )
    }
}

