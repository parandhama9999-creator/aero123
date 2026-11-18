package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.AiConfig
import com.aistudio.aerofast.trmst.data.AiProvider
import com.aistudio.aerofast.trmst.data.AiSettings
import com.aistudio.aerofast.trmst.data.OpenAiCompatClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** OpenAI-format requests and replies for OpenRouter, DeepSeek and custom gateways. */
class AiProvidersTest {

    @Test fun onlyEncryptedAddressesAreAccepted() {
        assertNull(OpenAiCompatClient.baseUrlProblem("https://openrouter.ai/api/v1"))
        assertNull(OpenAiCompatClient.baseUrlProblem("https://api.deepseek.com"))
        assertNotNull(OpenAiCompatClient.baseUrlProblem("http://192.168.1.20:3001/v1"))
        assertNotNull(OpenAiCompatClient.baseUrlProblem(""))
        assertEquals("https://openrouter.ai/api/v1/chat/completions", OpenAiCompatClient.endpoint("https://openrouter.ai/api/v1/", "/chat/completions"))
    }

    @Test fun chatBodyIsOpenAiFormat() {
        val text = OpenAiCompatClient.buildChatBody("openrouter/free", "Cut-off for M10x60?", null, 512, 0.2)
        assertEquals("openrouter/free", text.getString("model"))
        val msg = text.getJSONArray("messages").getJSONObject(0)
        assertEquals("user", msg.getString("role"))
        assertEquals("Cut-off for M10x60?", msg.getString("content"))
        assertEquals(512, text.getInt("max_tokens"))
        // With a drawing: text part + data-URI image part
        val img = OpenAiCompatClient.buildChatBody("vision-model", "Read the drawing", "AAAA", 4096, 0.1)
        val parts = img.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertEquals("text", parts.getJSONObject(0).getString("type"))
        assertEquals("data:image/jpeg;base64,AAAA", parts.getJSONObject(1).getJSONObject("image_url").getString("url"))
    }

    @Test fun repliesAndErrorsAreReadExactly() {
        assertEquals("79.6 mm", OpenAiCompatClient.parseChatResponse("""{"choices":[{"message":{"role":"assistant","content":" 79.6 mm "}}]}""").getOrThrow())
        assertEquals("ab", OpenAiCompatClient.parseChatResponse("""{"choices":[{"message":{"content":[{"type":"text","text":"a"},{"type":"text","text":"b"}]}}]}""").getOrThrow())
        val err = OpenAiCompatClient.parseChatResponse("""{"error":{"message":"Model does not exist","code":400}}""")
        assertTrue(err.isFailure)
        assertEquals("Model does not exist", err.exceptionOrNull()!!.message)
        assertTrue(OpenAiCompatClient.parseChatResponse("""{"choices":[{"message":{"content":""}}]}""").isFailure)
        assertTrue(OpenAiCompatClient.parseChatResponse("<html>502</html>").isFailure)
    }

    @Test fun freeModelsAreMarkedAndListedFirst() {
        val list = OpenAiCompatClient.parseModels("""{"data":[
            {"id":"paid/model-a","pricing":{"prompt":"0.000001","completion":"0.000002"}},
            {"id":"vendor/model-b:free","pricing":{"prompt":"0","completion":"0"}},
            {"id":"openrouter/free","pricing":{"prompt":"0","completion":"0"}},
            {"id":"deepseek-flash"}]}""")
        assertEquals(listOf("openrouter/free", "vendor/model-b:free"), list.filter { it.free }.map { it.id })
        assertEquals("openrouter/free", list.first().id)
        assertFalse(list.first { it.id == "deepseek-flash" }.free)
    }

    @Test fun providerNeedsKeyAddressAndModel() {
        assertTrue(AiSettings.isUsable(AiConfig(AiProvider.OPENROUTER, "https://openrouter.ai/api/v1", "openrouter/free", "sk-or-x")))
        assertFalse(AiSettings.isUsable(AiConfig(AiProvider.OPENROUTER, "https://openrouter.ai/api/v1", "openrouter/free", "")))
        assertFalse(AiSettings.isUsable(AiConfig(AiProvider.DEEPSEEK, "https://api.deepseek.com", "", "key")))
        // A self-hosted gateway may run without a key, but never over plain http
        assertTrue(AiSettings.isUsable(AiConfig(AiProvider.CUSTOM, "https://llm.example.org/v1", "auto", "")))
        assertFalse(AiSettings.isUsable(AiConfig(AiProvider.CUSTOM, "http://localhost:3001/v1", "auto", "")))
    }
}
