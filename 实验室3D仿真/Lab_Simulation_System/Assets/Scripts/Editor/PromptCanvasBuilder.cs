using TMPro;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

/// <summary>
/// 交互提示 HUD 构建器：在屏幕下方居中创建一行浮动提示，
/// 角色靠近可交互对象时浮现，离开后消失。
///
/// Canvas 为 Screen Space Overlay，始终覆盖在玩家视野中。
/// 使用方式：
/// 1. 选中场景中任意一个 ScreenTrigger（如 BigScreen）
/// 2. 菜单栏 Tools → 搭建交互提示 UI
/// 3. 生成的 InteractionHUD 会自动绑定到场景中所有 ScreenTrigger
/// </summary>
public class PromptCanvasBuilder : EditorWindow
{
    private const string FontAssetPath = "Assets/字体/ZiHunBingLin.asset";

    [MenuItem("Tools/搭建交互提示 UI", false, 101)]
    public static void BuildPromptUI()
    {
        // 清理所有旧版本的提示 Canvas（包括 inactive 的残余）
        // 遍历场景根对象及子对象，按名称删除旧 Canvas
        Scene scene = SceneManager.GetActiveScene();
        foreach (GameObject rootGo in scene.GetRootGameObjects())
        {
            Transform[] allChildren = rootGo.GetComponentsInChildren<Transform>(includeInactive: true);
            foreach (Transform t in allChildren)
            {
                if (t.name == "InteractionHUD" || t.name == "PromptCanvas")
                {
                    DestroyImmediate(t.gameObject);
                }
            }
        }

        TMP_FontAsset font = ResolveFont();

        // ==================== Canvas（Screen Space Overlay，全局 HUD） ====================
        GameObject canvasGo = new GameObject("InteractionHUD",
            typeof(RectTransform), typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
        // 放在场景根下，不属于任何特定对象
        canvasGo.transform.SetAsFirstSibling();

        Canvas canvas = canvasGo.GetComponent<Canvas>();
        canvas.renderMode = RenderMode.ScreenSpaceOverlay;
        canvas.sortingOrder = 200; // 高于 LabInfoPanel(100)

        CanvasScaler scaler = canvasGo.GetComponent<CanvasScaler>();
        scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        scaler.referenceResolution = new Vector2(1920f, 1080f);
        scaler.matchWidthOrHeight = 0.5f;

        // ==================== 提示文字（底部居中，单行，无背景） ====================
        GameObject promptTextGo = new GameObject("PromptText",
            typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
        promptTextGo.transform.SetParent(canvasGo.transform, false);

        RectTransform textRt = promptTextGo.GetComponent<RectTransform>();
        textRt.anchorMin = new Vector2(0.5f, 0f);
        textRt.anchorMax = new Vector2(0.5f, 0f);
        textRt.pivot = new Vector2(0.5f, 0.5f);
        textRt.sizeDelta = new Vector2(600f, 40f);
        textRt.anchoredPosition = new Vector2(0f, 100f);

        TextMeshProUGUI promptText = promptTextGo.GetComponent<TextMeshProUGUI>();
        promptText.font = font;
        promptText.text = "按 \"Space\" 键查看大屏信息";
        promptText.fontSize = 22;
        promptText.fontWeight = FontWeight.Medium;
        promptText.color = new Color(0.95f, 0.97f, 1f, 0.9f);
        promptText.alignment = TextAlignmentOptions.Center;
        promptText.raycastTarget = false;
        // 添加描边让文字在亮/暗背景下都清晰可读
        promptText.outlineWidth = 0.25f;
        promptText.outlineColor = new Color(0f, 0f, 0f, 0.6f);
        promptText.enableWordWrapping = false;

        // ==================== 初始隐藏 ====================
        canvasGo.SetActive(false);

        // ==================== 自动绑定到场景中所有 ScreenTrigger ====================
        ScreenTrigger[] allTriggers = Object.FindObjectsOfType<ScreenTrigger>();
        int bindCount = 0;
        foreach (ScreenTrigger trigger in allTriggers)
        {
            trigger.promptUI = canvasGo;
            trigger.promptText = promptText;
            trigger.promptMessage = "按 \"Space\" 键查看大屏信息";
            EditorUtility.SetDirty(trigger);
            bindCount++;
        }

        if (bindCount == 0)
        {
            Debug.LogWarning("[PromptCanvasBuilder] 场景中没有 ScreenTrigger，请在 Inspector 中手动将 InteractionHUD 拖入 ScreenTrigger 的 promptUI 字段。");
        }
        else
        {
            Debug.Log($"[PromptCanvasBuilder] 已自动绑定 {bindCount} 个 ScreenTrigger 到 InteractionHUD。");
        }

        Selection.activeGameObject = canvasGo;
        EditorSceneManager.MarkSceneDirty(scene);
        Debug.Log("[PromptCanvasBuilder] 交互提示 HUD 创建完成（透明背景，底部居中单行文字）。");
    }

    // ==================== Helpers ====================

    private static TMP_FontAsset ResolveFont()
    {
        var font = AssetDatabase.LoadAssetAtPath<TMP_FontAsset>(FontAssetPath);
        if (font == null)
            font = TMP_Settings.defaultFontAsset;
        if (font == null)
            Debug.LogError("[PromptCanvasBuilder] 无法找到字体！");
        return font;
    }

    // ==================== 退出提示 HUD ====================

    [MenuItem("Tools/搭建退出提示 UI", false, 102)]
    public static void BuildExitPromptUI()
    {
        Scene scene = SceneManager.GetActiveScene();

        // 清理旧版本
        foreach (GameObject rootGo in scene.GetRootGameObjects())
        {
            foreach (Transform t in rootGo.GetComponentsInChildren<Transform>(true))
            {
                if (t.name == "ExitHUD")
                    DestroyImmediate(t.gameObject);
            }
        }

        TMP_FontAsset font = ResolveFont();

        // Canvas（Screen Space Overlay）
        GameObject canvasGo = new GameObject("ExitHUD",
            typeof(RectTransform), typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
        canvasGo.transform.SetAsFirstSibling();

        Canvas canvas = canvasGo.GetComponent<Canvas>();
        canvas.renderMode = RenderMode.ScreenSpaceOverlay;
        canvas.sortingOrder = 200;

        CanvasScaler scaler = canvasGo.GetComponent<CanvasScaler>();
        scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        scaler.referenceResolution = new Vector2(1920f, 1080f);
        scaler.matchWidthOrHeight = 0.5f;

        // 提示文字（底部居中，与 InteractionHUD 完全同款样式）
        GameObject textGo = new GameObject("PromptText",
            typeof(RectTransform), typeof(CanvasRenderer), typeof(TextMeshProUGUI));
        textGo.transform.SetParent(canvasGo.transform, false);

        RectTransform textRt = textGo.GetComponent<RectTransform>();
        textRt.anchorMin = new Vector2(0.5f, 0f);
        textRt.anchorMax = new Vector2(0.5f, 0f);
        textRt.pivot = new Vector2(0.5f, 0.5f);
        textRt.sizeDelta = new Vector2(600f, 40f);
        textRt.anchoredPosition = new Vector2(0f, 100f);

        TextMeshProUGUI promptText = textGo.GetComponent<TextMeshProUGUI>();
        promptText.font = font;
        promptText.text = "按 \"Q\" 键退出游戏";
        promptText.fontSize = 22;
        promptText.fontWeight = FontWeight.Medium;
        promptText.color = new Color(0.95f, 0.97f, 1f, 0.9f);
        promptText.alignment = TextAlignmentOptions.Center;
        promptText.raycastTarget = false;
        promptText.outlineWidth = 0.25f;
        promptText.outlineColor = new Color(0f, 0f, 0f, 0.6f);
        promptText.enableWordWrapping = false;

        canvasGo.SetActive(false);

        // 自动绑定到场景中所有 ExitTrigger
        ExitTrigger[] allTriggers = Object.FindObjectsOfType<ExitTrigger>(true);
        int bindCount = 0;
        foreach (ExitTrigger trigger in allTriggers)
        {
            trigger.promptUI = canvasGo;
            trigger.promptText = promptText;
            EditorUtility.SetDirty(trigger);
            bindCount++;
        }

        if (bindCount == 0)
            Debug.LogWarning("[PromptCanvasBuilder] 场景中没有 ExitTrigger，请先将 ExitTrigger 脚本挂到门对象上，再运行此菜单。");
        else
            Debug.Log($"[PromptCanvasBuilder] ExitHUD 已绑定到 {bindCount} 个 ExitTrigger。");

        Selection.activeGameObject = canvasGo;
        EditorSceneManager.MarkSceneDirty(scene);
        Debug.Log("[PromptCanvasBuilder] 退出提示 HUD 创建完成。");
    }
}
