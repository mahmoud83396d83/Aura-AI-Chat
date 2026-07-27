package com.example.data

import com.example.data.api.AiRetrofitClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
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
     * Sends a chat request to Gemini or OpenRouter and returns the assistant's ChatMessage response.
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
        var actualModel = modelName

        // Validate API keys before making the network request
        if (isGemini) {
            if (geminiApiKey.isBlank() || geminiApiKey == "MY_GEMINI_API_KEY") {
                return@withContext ChatMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "⚠️ **مفتاح Gemini API مطلوب / Gemini API Key Required**\n\nلم يتم تحديد مفتاح **Gemini API Key** صالح.\nيرجى التوجه إلى **الإعدادات (⚙️ Settings)** وإدخال مفتاح Gemini الخاص بك لتشغيل النموذج المباشر.\n\nيمكنك الحصول على مفتاح مجاني من [Google AI Studio](https://aistudio.google.com/app/apikey).",
                    modelName = modelName
                )
            }
        } else {
            if (openRouterApiKey.isBlank() || openRouterApiKey == "MY_OPENROUTER_API_KEY") {
                return@withContext ChatMessage(
                    sessionId = sessionId,
                    role = "assistant",
                    content = "⚠️ **مفتاح OpenRouter API مطلوب / OpenRouter Key Required**\n\nالنموذج المحدد (`$modelName`) يتطلب مفتاح **OpenRouter API Key**.\nيرجى التوجه إلى **الإعدادات (⚙️ Settings)** وإدخال مفتاح OpenRouter، أو قم بتبديل النموذج إلى **Gemini 2.5 Flash** المباشر.\n\nيمكنك الحصول على مفتاح مجاني من [OpenRouter.ai](https://openrouter.ai/keys).",
                    modelName = modelName
                )
            }
        }

        try {
            if (isGemini) {
                // Prepare Gemini request
                val geminiContents = messages.map { msg ->
                    // Gemini expects "model" role instead of "assistant"
                    val roleName = if (msg.role == "assistant") "model" else "user"
                    GeminiContent(
                        role = roleName,
                        parts = listOf(GeminiPart(text = msg.content))
                    )
                }
                
                val sysInstructionContent = if (systemInstruction.isNotBlank()) {
                    GeminiContent(parts = listOf(GeminiPart(text = systemInstruction)))
                } else null

                val request = GeminiRequest(
                    contents = geminiContents,
                    systemInstruction = sysInstructionContent
                )

                val response = AiRetrofitClient.geminiApi.generateContent(
                    model = modelName,
                    apiKey = geminiApiKey,
                    request = request
                )

                assistantContent = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "⚠️ **لم يتم استلام رد من النموذج** (قد يكون هناك تقييد في إعدادات الأمان للنموذج)."
                
                // Estimate tokens if usage isn't provided (word count * 1.3 as a safe heuristic)
                val totalPromptWords = messages.sumOf { it.content.split("\\s+".toRegex()).size } + systemInstruction.split("\\s+".toRegex()).size
                val responseWords = assistantContent.split("\\s+".toRegex()).size
                promptTokens = (totalPromptWords * 1.3).toInt()
                completionTokens = (responseWords * 1.3).toInt()
                
            } else {
                // Prepare OpenRouter request
                val openRouterMessages = mutableListOf<OpenRouterMessage>()
                
                if (systemInstruction.isNotBlank()) {
                    openRouterMessages.add(OpenRouterMessage(role = "system", content = systemInstruction))
                }
                
                messages.forEach { msg ->
                    openRouterMessages.add(OpenRouterMessage(role = msg.role, content = msg.content))
                }

                val request = OpenRouterRequest(
                    model = modelName,
                    messages = openRouterMessages,
                    temperature = 0.7
                )

                val response = AiRetrofitClient.openRouterApi.chatCompletions(
                    authorization = "Bearer $openRouterApiKey",
                    request = request
                )

                assistantContent = response.choices?.firstOrNull()?.message?.content 
                    ?: "⚠️ **لم يتم استلام رد من OpenRouter.**"
                
                actualModel = response.model ?: modelName
                
                response.usage?.let { usage ->
                    promptTokens = usage.promptTokens ?: 0
                    completionTokens = usage.completionTokens ?: 0
                    reasoningTokens = usage.completionTokensDetails?.reasoningTokens ?: 0
                }
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
            latencyMs = latencyMs
        )
    }
}

