using System;
using System.Runtime.InteropServices;
using UnityEngine;
using UnityEngine.SceneManagement;

[Serializable]
public class SmartLabUnityEventEnvelope
{
    public string eventName;
    public string source;
    public string occurredAt;
    public string scene;
    public string message;
    public string level;
    public string entityId;
    public string entityName;
    public string requestId;
}

public class SmartLabSceneSyncBridge : MonoBehaviour
{
    private const float DefaultNoticeDuration = 3.5f;

    private static SmartLabSceneSyncBridge instance;

    private string noticeMessage = string.Empty;
    private string noticeLevel = "info";
    private float noticeUntilTime;
    private GUIStyle noticeBoxStyle;
    private GUIStyle noticeTitleStyle;
    private GUIStyle noticeMessageStyle;

#if UNITY_WEBGL && !UNITY_EDITOR
    [DllImport("__Internal")]
    private static extern void SmartLabBridge_PostMessage(string json);
#endif

    private void Awake()
    {
        if (instance != null && instance != this)
        {
            Destroy(this);
            return;
        }

        instance = this;
    }

    private void Start()
    {
        EmitWebEvent(
            "scene.bridge_ready",
            $"Unity 双向桥已就绪，当前场景：{ResolveActiveSceneLabel()}",
            "success");
    }

    private void OnEnable()
    {
        WebGLBridge.ContextChanged += HandleContextChanged;
        WebGLBridge.WebCommandReceived += HandleWebCommandReceived;
        SceneManager.sceneLoaded += HandleSceneLoaded;
    }

    private void OnDisable()
    {
        WebGLBridge.ContextChanged -= HandleContextChanged;
        WebGLBridge.WebCommandReceived -= HandleWebCommandReceived;
        SceneManager.sceneLoaded -= HandleSceneLoaded;

        if (instance == this)
            instance = null;
    }

    public static void ShowNotice(string message, string level = "info", float durationSeconds = DefaultNoticeDuration)
    {
        if (instance == null || string.IsNullOrWhiteSpace(message))
            return;

        instance.noticeMessage = message.Trim();
        instance.noticeLevel = string.IsNullOrWhiteSpace(level) ? "info" : level.Trim().ToLowerInvariant();
        instance.noticeUntilTime = Time.unscaledTime + Mathf.Max(1.5f, durationSeconds);
    }

    public static void EmitWebEvent(string eventName, string message, string level = "info", string entityName = null, string entityId = null, string requestId = null)
    {
        if (string.IsNullOrWhiteSpace(eventName))
            return;

        SmartLabUnityEventEnvelope envelope = new SmartLabUnityEventEnvelope
        {
            eventName = eventName.Trim(),
            source = "unity",
            occurredAt = DateTime.UtcNow.ToString("o"),
            scene = ResolveActiveSceneLabel(),
            message = string.IsNullOrWhiteSpace(message) ? string.Empty : message.Trim(),
            level = string.IsNullOrWhiteSpace(level) ? "info" : level.Trim().ToLowerInvariant(),
            entityName = string.IsNullOrWhiteSpace(entityName) ? string.Empty : entityName.Trim(),
            entityId = string.IsNullOrWhiteSpace(entityId) ? string.Empty : entityId.Trim(),
            requestId = string.IsNullOrWhiteSpace(requestId) ? string.Empty : requestId.Trim(),
        };

        DispatchEnvelope(envelope);
    }

    private static void DispatchEnvelope(SmartLabUnityEventEnvelope envelope)
    {
        string json = JsonUtility.ToJson(envelope);

#if UNITY_WEBGL && !UNITY_EDITOR
        SmartLabBridge_PostMessage(json);
#else
        Debug.Log($"[SmartLabSceneSyncBridge] {json}");
#endif
    }

    private void HandleContextChanged(SmartLabWebContext context)
    {
        string contextScene = context != null && !string.IsNullOrEmpty(context.scene)
            ? context.scene
            : ResolveActiveSceneLabel();

        ShowNotice("已接收 Web 端登录态与接口上下文", "info", 2.4f);
        EmitWebEvent("scene.context_synced", $"已同步场景上下文：{contextScene}", "info", contextScene);
    }

    private void HandleSceneLoaded(Scene scene, LoadSceneMode mode)
    {
        EmitWebEvent("scene.loaded", $"Unity 已加载场景：{scene.name}", "success", scene.name);
    }

    private void HandleWebCommandReceived(SmartLabWebCommand command)
    {
        if (command == null || string.IsNullOrWhiteSpace(command.command))
            return;

        string normalizedCommand = command.command.Trim();
        switch (normalizedCommand)
        {
            case "scene.requestState":
                ShowNotice("已向 Web 回传当前场景状态", "info", 2.4f);
                EmitWebEvent(
                    "scene.state_reported",
                    BuildSceneStateMessage(),
                    "info",
                    ResolveActiveSceneLabel(),
                    null,
                    command.requestId);
                break;

            case "scene.refreshData":
                RefreshDataProviders();
                ShowNotice("已执行场景数据刷新", "success", 2.8f);
                EmitWebEvent(
                    "scene.refresh_requested",
                    "已触发 Unity 侧数据同步流程",
                    "success",
                    ResolveActiveSceneLabel(),
                    null,
                    command.requestId);
                break;

            case "scene.showNotice":
                string notice = string.IsNullOrWhiteSpace(command.message)
                    ? "Web 端发送了一条场景提示"
                    : command.message.Trim();
                string level = string.IsNullOrWhiteSpace(command.level) ? "info" : command.level.Trim();
                ShowNotice(notice, level, 3.8f);
                EmitWebEvent("scene.notice_displayed", notice, level, ResolveActiveSceneLabel(), null, command.requestId);
                break;

            default:
                string fallback = $"已收到 Web 命令：{normalizedCommand}";
                ShowNotice(fallback, "warning", 2.8f);
                EmitWebEvent("scene.command_received", fallback, "warning", ResolveActiveSceneLabel(), null, command.requestId);
                break;
        }
    }

    private void RefreshDataProviders()
    {
        bool hasProvider = false;

        LabDataProvider[] labProviders = FindObjectsOfType<LabDataProvider>(true);
        for (int i = 0; i < labProviders.Length; i++)
        {
            labProviders[i].ForceBackendRefresh();
            hasProvider = true;
        }

        ReagentDataProvider[] reagentProviders = FindObjectsOfType<ReagentDataProvider>(true);
        for (int i = 0; i < reagentProviders.Length; i++)
        {
            reagentProviders[i].ForceBackendRefresh();
            hasProvider = true;
        }

        if (!hasProvider)
        {
            EmitWebEvent("scene.refresh_skipped", "当前场景未找到可刷新的数据提供者", "warning");
        }
    }

    private string BuildSceneStateMessage()
    {
        string contextScene = WebGLBridge.CurrentContext != null && !string.IsNullOrEmpty(WebGLBridge.CurrentContext.scene)
            ? WebGLBridge.CurrentContext.scene
            : "未注入";
        string apiState = WebGLBridge.HasUsableContext ? "已连接后端 API" : "当前使用离线 Mock 数据";
        return $"Unity 当前场景：{ResolveActiveSceneLabel()}；上下文场景：{contextScene}；{apiState}";
    }

    private static string ResolveActiveSceneLabel()
    {
        Scene activeScene = SceneManager.GetActiveScene();
        if (activeScene.IsValid() && !string.IsNullOrEmpty(activeScene.name))
            return activeScene.name;

        if (WebGLBridge.CurrentContext != null && !string.IsNullOrEmpty(WebGLBridge.CurrentContext.scene))
            return WebGLBridge.CurrentContext.scene;

        return "UnknownScene";
    }

    private void EnsureGuiStyles()
    {
        if (noticeBoxStyle != null)
            return;

        noticeBoxStyle = new GUIStyle(GUI.skin.box)
        {
            alignment = TextAnchor.UpperLeft,
            padding = new RectOffset(16, 16, 14, 14),
        };

        noticeTitleStyle = new GUIStyle(GUI.skin.label)
        {
            fontSize = 11,
            fontStyle = FontStyle.Bold,
            normal = { textColor = new Color(0.85f, 0.92f, 0.98f, 0.96f) }
        };

        noticeMessageStyle = new GUIStyle(GUI.skin.label)
        {
            fontSize = 15,
            wordWrap = true,
            normal = { textColor = Color.white }
        };
    }

    private void OnGUI()
    {
        if (string.IsNullOrEmpty(noticeMessage) || Time.unscaledTime >= noticeUntilTime)
            return;

        EnsureGuiStyles();

        Rect noticeRect = new Rect(18f, 18f, Mathf.Min(560f, Screen.width - 36f), 88f);
        Color previousBackground = GUI.backgroundColor;
        GUI.backgroundColor = ResolveBackgroundColor(noticeLevel);
        GUI.Box(noticeRect, GUIContent.none, noticeBoxStyle);
        GUI.backgroundColor = previousBackground;

        Rect titleRect = new Rect(noticeRect.x + 16f, noticeRect.y + 12f, noticeRect.width - 32f, 18f);
        Rect messageRect = new Rect(noticeRect.x + 16f, noticeRect.y + 34f, noticeRect.width - 32f, noticeRect.height - 42f);
        GUI.Label(titleRect, ResolveNoticeTitle(noticeLevel), noticeTitleStyle);
        GUI.Label(messageRect, noticeMessage, noticeMessageStyle);
    }

    private static string ResolveNoticeTitle(string level)
    {
        switch (level)
        {
            case "success":
                return "Web -> Unity 联动成功";
            case "warning":
                return "Web -> Unity 联动提醒";
            default:
                return "Web -> Unity 联动通知";
        }
    }

    private static Color ResolveBackgroundColor(string level)
    {
        switch (level)
        {
            case "success":
                return new Color(0.10f, 0.38f, 0.21f, 0.95f);
            case "warning":
                return new Color(0.43f, 0.24f, 0.03f, 0.96f);
            default:
                return new Color(0.07f, 0.16f, 0.30f, 0.96f);
        }
    }
}