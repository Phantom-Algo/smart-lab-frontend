using System;
using System.Collections;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;

[Serializable]
public class SmartLabWebContext
{
    public string scene;
    public string token;
    public string userId;
    public string userName;
    public string apiBase;
}

public class SmartLabRealtimeMessage
{
    public string eventName;
    public string source;
    public string occurredAt;
    public string rawJson;
}

[Serializable]
public class SmartLabWebCommand
{
    public string command;
    public string requestId;
    public string message;
    public string level;
    public string target;
}

[Serializable]
public class SmartLabPlainApiResponse
{
    public int code;
    public string message;
}

public class WebGLBridge : MonoBehaviour
{
    public static WebGLBridge Instance { get; private set; }
    public static SmartLabWebContext CurrentContext { get; private set; } = new SmartLabWebContext();
    public static event Action<SmartLabWebContext> ContextChanged;
    public static event Action<SmartLabRealtimeMessage> RealtimeEventReceived;
    public static event Action<SmartLabWebCommand> WebCommandReceived;

    public static bool HasUsableContext =>
        !string.IsNullOrEmpty(CurrentContext.apiBase) &&
        !string.IsNullOrEmpty(CurrentContext.token);

    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
    private static void Bootstrap()
    {
        if (Instance != null)
            return;

        GameObject bridgeObject = new GameObject("WebGLBridge");
        DontDestroyOnLoad(bridgeObject);
        Instance = bridgeObject.AddComponent<WebGLBridge>();
        bridgeObject.AddComponent<SmartLabSceneSyncBridge>();
    }

    private void Awake()
    {
        if (Instance != null && Instance != this)
        {
            Destroy(gameObject);
            return;
        }

        Instance = this;
        DontDestroyOnLoad(gameObject);
        TryLoadContextFromAbsoluteUrl();
    }

    public void ReceiveWebContext(string json)
    {
        if (string.IsNullOrEmpty(json))
            return;

        try
        {
            SmartLabWebContext incoming = JsonUtility.FromJson<SmartLabWebContext>(json);
            ApplyContext(incoming);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[WebGLBridge] 解析 Web 上下文失败: {ex.Message}");
        }
    }

    public void ReceiveRealtimeEvent(string json)
    {
        if (string.IsNullOrEmpty(json))
            return;

        SmartLabRealtimeMessage message = new SmartLabRealtimeMessage
        {
            eventName = SmartLabJsonHelper.ExtractStringField(json, "event"),
            source = SmartLabJsonHelper.ExtractStringField(json, "source"),
            occurredAt = SmartLabJsonHelper.ExtractStringField(json, "occurredAt"),
            rawJson = json
        };

        RealtimeEventReceived?.Invoke(message);
    }

    public void ReceiveWebCommand(string json)
    {
        if (string.IsNullOrEmpty(json))
            return;

        try
        {
            SmartLabWebCommand command = JsonUtility.FromJson<SmartLabWebCommand>(json);
            if (command == null || string.IsNullOrEmpty(command.command))
                return;

            WebCommandReceived?.Invoke(command);
        }
        catch (Exception ex)
        {
            Debug.LogWarning($"[WebGLBridge] 解析 Web 命令失败: {ex.Message}");
        }
    }

    public static void EmitWebEvent(string eventName, string message, string level = "info", string entityName = null, string entityId = null, string requestId = null)
    {
        SmartLabSceneSyncBridge.EmitWebEvent(eventName, message, level, entityName, entityId, requestId);
    }

    public static void ShowBridgeNotice(string message, string level = "info", float durationSeconds = 3.5f)
    {
        SmartLabSceneSyncBridge.ShowNotice(message, level, durationSeconds);
    }

    private void TryLoadContextFromAbsoluteUrl()
    {
        string url = Application.absoluteURL;
        if (string.IsNullOrEmpty(url) || !url.Contains("?"))
            return;

        SmartLabWebContext context = new SmartLabWebContext
        {
            scene = ExtractQueryValue(url, "scene"),
            token = ExtractQueryValue(url, "token"),
            userId = ExtractQueryValue(url, "userId"),
            userName = ExtractQueryValue(url, "userName"),
            apiBase = ExtractQueryValue(url, "apiBase")
        };

        ApplyContext(context);
    }

    private void ApplyContext(SmartLabWebContext incoming)
    {
        if (incoming == null)
            return;

        bool changed = false;
        changed |= ApplyString(ref CurrentContext.scene, incoming.scene);
        changed |= ApplyString(ref CurrentContext.token, incoming.token);
        changed |= ApplyString(ref CurrentContext.userId, incoming.userId);
        changed |= ApplyString(ref CurrentContext.userName, incoming.userName);
        changed |= ApplyString(ref CurrentContext.apiBase, incoming.apiBase);

        if (changed)
        {
            ContextChanged?.Invoke(CloneContext(CurrentContext));
        }
    }

    private static bool ApplyString(ref string currentValue, string nextValue)
    {
        if (string.IsNullOrEmpty(nextValue) || string.Equals(currentValue, nextValue, StringComparison.Ordinal))
            return false;

        currentValue = nextValue;
        return true;
    }

    private static SmartLabWebContext CloneContext(SmartLabWebContext source)
    {
        return new SmartLabWebContext
        {
            scene = source.scene,
            token = source.token,
            userId = source.userId,
            userName = source.userName,
            apiBase = source.apiBase,
        };
    }

    private static string ExtractQueryValue(string url, string key)
    {
        int questionMarkIndex = url.IndexOf('?');
        if (questionMarkIndex < 0)
            return string.Empty;

        string queryString = url.Substring(questionMarkIndex + 1);
        string[] pairs = queryString.Split('&');
        for (int i = 0; i < pairs.Length; i++)
        {
            string[] kv = pairs[i].Split('=');
            if (kv.Length == 2 && string.Equals(kv[0], key, StringComparison.OrdinalIgnoreCase))
            {
                return UnityWebRequest.UnEscapeURL(kv[1]);
            }
        }

        return string.Empty;
    }
}

public static class SmartLabWebApi
{
    public static string BuildUrl(string relativePath)
    {
        if (!WebGLBridge.HasUsableContext)
            return string.Empty;

        string baseUrl = WebGLBridge.CurrentContext.apiBase ?? string.Empty;
        return $"{baseUrl.TrimEnd('/')}/{relativePath.TrimStart('/')}";
    }

    public static UnityWebRequest CreateRequest(string method, string relativePath, string jsonBody = null)
    {
        string url = BuildUrl(relativePath);
        UnityWebRequest request = new UnityWebRequest(url, method)
        {
            downloadHandler = new DownloadHandlerBuffer()
        };

        if (!string.IsNullOrEmpty(jsonBody))
        {
            byte[] bodyBytes = Encoding.UTF8.GetBytes(jsonBody);
            request.uploadHandler = new UploadHandlerRaw(bodyBytes);
            request.SetRequestHeader("Content-Type", "application/json");
        }

        request.SetRequestHeader("Accept", "application/json");
        if (!string.IsNullOrEmpty(WebGLBridge.CurrentContext.token))
        {
            request.SetRequestHeader("Authorization", $"Bearer {WebGLBridge.CurrentContext.token}");
        }

        return request;
    }

    public static IEnumerator SendRequest(UnityWebRequest request, Action<string> onSuccess, Action<string> onError)
    {
        using (request)
        {
            yield return request.SendWebRequest();

            if (request.result != UnityWebRequest.Result.Success)
            {
                onError?.Invoke(BuildErrorMessage(request));
                yield break;
            }

            string responseText = request.downloadHandler != null ? request.downloadHandler.text : string.Empty;
            SmartLabPlainApiResponse response = TryParsePlainResponse(responseText);
            if (response != null && response.code != 0)
            {
                onError?.Invoke(string.IsNullOrEmpty(response.message) ? "请求失败" : response.message);
                yield break;
            }

            onSuccess?.Invoke(responseText);
        }
    }

    private static string BuildErrorMessage(UnityWebRequest request)
    {
        string responseText = request.downloadHandler != null ? request.downloadHandler.text : string.Empty;
        SmartLabPlainApiResponse response = TryParsePlainResponse(responseText);
        if (response != null && !string.IsNullOrEmpty(response.message))
        {
            return response.message;
        }

        if (!string.IsNullOrEmpty(request.error))
        {
            return request.error;
        }

        return $"请求失败，HTTP {request.responseCode}";
    }

    private static SmartLabPlainApiResponse TryParsePlainResponse(string responseText)
    {
        if (string.IsNullOrEmpty(responseText))
            return null;

        try
        {
            return JsonUtility.FromJson<SmartLabPlainApiResponse>(responseText);
        }
        catch
        {
            return null;
        }
    }
}

public static class SmartLabJsonHelper
{
    public static string ExtractStringField(string json, string fieldName)
    {
        if (string.IsNullOrEmpty(json) || string.IsNullOrEmpty(fieldName))
            return string.Empty;

        string marker = $"\"{fieldName}\"";
        int markerIndex = json.IndexOf(marker, StringComparison.Ordinal);
        if (markerIndex < 0)
            return string.Empty;

        int colonIndex = json.IndexOf(':', markerIndex + marker.Length);
        if (colonIndex < 0)
            return string.Empty;

        int startQuote = json.IndexOf('"', colonIndex + 1);
        if (startQuote < 0)
            return string.Empty;

        int endQuote = startQuote + 1;
        while (endQuote < json.Length)
        {
            if (json[endQuote] == '"' && json[endQuote - 1] != '\\')
            {
                return json.Substring(startQuote + 1, endQuote - startQuote - 1);
            }
            endQuote++;
        }

        return string.Empty;
    }
}