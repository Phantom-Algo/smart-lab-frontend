using System.Collections.Generic;
using TMPro;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

/// <summary>
/// Common_Lab 大屏交互 UI 构建器
/// 复用 LabInfoPanelBuilder 的全部 UI 逻辑，
/// 在构建前将 LabDataProvider 的实验室类型切换为"通用实验室"并注入 Mock 数据，
/// 构建完成后修正所有硬编码的"教学实验室"文字。
///
/// 使用方法：菜单 Tools → 搭建通用实验室大屏 UI
/// </summary>
public class CommonLabPanelBuilder : EditorWindow
{
    private const string LabType = "通用实验室";

    [MenuItem("Tools/搭建通用实验室大屏 UI", false, 110)]
    public static void BuildCommonLabUI()
    {
        // 1. 确保 LabDataProvider 存在，并预设为"通用实验室"数据
        LabDataProvider provider = PrepareDataProvider();
        if (provider == null)
        {
            Debug.LogError("[CommonLabPanelBuilder] 无法创建 LabDataProvider");
            return;
        }

        // 2. 复用 LabInfoPanelBuilder 构建全套 UI
        LabInfoPanelBuilder.BuildFullUI();

        // 设置场景 lab 类型（运行时 RefreshData 会根据此字段加载数据）
        SetSceneLabTypeOverride(LabType);
        PatchLabTypeTexts();

        Debug.Log("[CommonLabPanelBuilder] 通用实验室大屏 UI 构建完成。");
        EditorSceneManager.MarkSceneDirty(SceneManager.GetActiveScene());
    }

    private static void SetSceneLabTypeOverride(string labType)
    {
        GameObject panel = GameObject.Find("LabInfoPanel");
        if (panel == null) return;

        LabInfoPanelUI ui = panel.GetComponent<LabInfoPanelUI>();
        if (ui != null)
        {
            ui.sceneLabTypeOverride = labType;
            EditorUtility.SetDirty(ui);
        }
    }

    /// <summary>
    /// 将 LabInfoPanel 中所有写死的"教学实验室"文字替换为"通用实验室"
    /// </summary>
    private static void PatchLabTypeTexts()
    {
        GameObject panel = GameObject.Find("LabInfoPanel");
        if (panel == null)
        {
            Debug.LogWarning("[CommonLabPanelBuilder] 未找到 LabInfoPanel，跳过文字修正");
            return;
        }

        // 收集所有 TextMeshProUGUI 并替换
        foreach (var tmp in panel.GetComponentsInChildren<TextMeshProUGUI>(true))
        {
            if (tmp.text.Contains("教学实验室"))
            {
                tmp.text = tmp.text.Replace("教学实验室", LabType);
                EditorUtility.SetDirty(tmp);
            }
        }

        // 借用面板说明文字里有"教学实验室"（PreviewNoteBodyText）
        // 上面的循环已经覆盖，这里额外 SetDirty 根对象保险起见
        EditorUtility.SetDirty(panel);
    }

    /// <summary>
    /// 确保场景中有 LabDataProvider，类型设为"通用实验室"，并注入足量 Mock 数据。
    /// </summary>
    private static LabDataProvider PrepareDataProvider()
    {
        LabDataProvider provider = null;
        foreach (var p in Resources.FindObjectsOfTypeAll<LabDataProvider>())
        {
            if (p != null && p.gameObject.scene.IsValid())
            {
                provider = p;
                break;
            }
        }

        if (provider == null)
        {
            GameObject go = new GameObject("LabDataProvider");
            provider = go.AddComponent<LabDataProvider>();
        }

        provider.currentSceneLabType = LabType;

        if (provider.mockLabData == null)
            provider.mockLabData = new List<LabInfoData>();

        // 不注入硬编码 mock 数据，清空后由运行时 LabData.Awake() 的 GenerateMockData() 统一兜底
        // 真实数据由后端 API lab-management/labs 提供
        provider.mockLabData.Clear();

        EditorUtility.SetDirty(provider);
        EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo();
        return provider;
    }
}
