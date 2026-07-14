using UnityEngine;
using UnityEngine.SceneManagement;

/// <summary>
/// WebGL 场景启动加载器：从 URL 参数 ?scene=xxx 读取目标场景并加载。
/// 挂载到启动场景（如 BootScene）中的任意 GameObject 上即可。
/// </summary>
public class SceneLoader : MonoBehaviour
{
    [Header("场景 Key 与场景名的映射")]
    public SceneMapping[] sceneMappings;

    void Start()
    {
        string targetScene = GetSceneFromURL();

        if (!string.IsNullOrEmpty(targetScene))
        {
            Debug.Log($"[SceneLoader] URL 参数指定场景: {targetScene}");
            // 在映射表中查找对应的场景名
            foreach (var mapping in sceneMappings)
            {
                if (mapping.urlKey == targetScene)
                {
                    Debug.Log($"[SceneLoader] 匹配成功，加载场景: {mapping.sceneName}");
                    SceneManager.LoadScene(mapping.sceneName);
                    return;
                }
            }
            Debug.LogWarning($"[SceneLoader] 未找到匹配场景: {targetScene}，将停留在当前场景");
        }
        else
        {
            Debug.Log("[SceneLoader] 未检测到 URL 参数，停留在当前场景（可作为主菜单）");
        }
    }

    /// <summary>
    /// 从浏览器 URL 中提取 ?scene=xxx 参数
    /// </summary>
    private string GetSceneFromURL()
    {
        // Application.absoluteURL 在 WebGL 平台返回完整的浏览器地址
        string url = Application.absoluteURL;

        if (string.IsNullOrEmpty(url)) return null;

        // 解析 ?scene=xxx
        int questionMarkIndex = url.IndexOf('?');
        if (questionMarkIndex < 0) return null;

        string queryString = url.Substring(questionMarkIndex + 1);
        string[] pairs = queryString.Split('&');

        foreach (string pair in pairs)
        {
            string[] kv = pair.Split('=');
            if (kv.Length == 2 && kv[0].ToLower() == "scene")
            {
                // URL 解码，处理中文等特殊字符
                return UnityEngine.Networking.UnityWebRequest.UnEscapeURL(kv[1]);
            }
        }

        return null;
    }
}

[System.Serializable]
public class SceneMapping
{
    [Tooltip("URL 中传递的 key，如 class_lab")]
    public string urlKey;

    [Tooltip("Build Settings 中的场景名称，如 Class_Lab")]
    public string sceneName;
}
