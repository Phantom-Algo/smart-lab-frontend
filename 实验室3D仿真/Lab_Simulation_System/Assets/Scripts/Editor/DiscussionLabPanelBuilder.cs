using System.Collections.Generic;
using TMPro;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

/// <summary>
/// Discussion_Lab 小组讨论实验室大屏交互 UI 构建器
/// 使用方法：菜单 Tools → 搭建小组讨论室大屏 UI
/// </summary>
public class DiscussionLabPanelBuilder : EditorWindow
{
    private const string LabType = "小组讨论室";

    [MenuItem("Tools/搭建小组讨论室大屏 UI", false, 112)]
    public static void BuildDiscussionLabUI()
    {
        LabDataProvider provider = PrepareDataProvider();
        if (provider == null)
        {
            Debug.LogError("[DiscussionLabPanelBuilder] 无法创建 LabDataProvider");
            return;
        }

        LabInfoPanelBuilder.BuildFullUI();

        // 设置场景 lab 类型（运行时 RefreshData 会根据此字段加载数据）
        SetSceneLabTypeOverride(LabType);
        PatchLabTypeTexts();

        Debug.Log("[DiscussionLabPanelBuilder] 小组讨论室大屏 UI 构建完成。");
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

    private static void PatchLabTypeTexts()
    {
        GameObject panel = GameObject.Find("LabInfoPanel");
        if (panel == null) return;

        foreach (var tmp in panel.GetComponentsInChildren<TextMeshProUGUI>(true))
        {
            if (tmp.text.Contains("教学实验室"))
            {
                tmp.text = tmp.text.Replace("教学实验室", LabType);
                EditorUtility.SetDirty(tmp);
            }
        }

        EditorUtility.SetDirty(panel);
    }

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
